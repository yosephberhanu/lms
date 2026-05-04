import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormArray, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { httpErrorMessage } from '../core/http-error-message';
import type {
  Ownership,
  OwnershipRole,
  PropertyPayload,
  PropertyRecord,
  PropertyType
} from '../models/lms.models';
import { PropertyApiService } from '../services/property-api.service';
import { formatEnum } from '../shared/format-enum';

interface OwnershipFormValue {
  ownerPartyId: string;
  role: OwnershipRole;
  ownershipPercentage: number;
  effectiveFrom: string;
  effectiveTo: string | null;
  notes: string | null;
}

@Component({
  selector: 'app-property-form',
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './property-form.component.html',
  styleUrl: './property-form.component.css'
})
export class PropertyFormComponent implements OnInit {
  private readonly api = inject(PropertyApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  readonly propertyTypes: PropertyType[] = [
    'RESIDENTIAL',
    'COMMERCIAL',
    'INDUSTRIAL',
    'MIXED_USE',
    'LAND',
    'OTHER'
  ];

  readonly ownershipRoles: OwnershipRole[] = [
    'PRIMARY_OWNER',
    'CO_OWNER',
    'BENEFICIARY',
    'TRUSTEE'
  ];

  saveInFlight = false;
  deleteInFlight = false;
  loadError = '';
  errorMessage = '';
  successMessage = '';

  readonly propertyForm = this.fb.group({
    id: this.fb.control<number | null>(null),
    name: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(255)]),
    propertyType: this.fb.nonNullable.control<PropertyType>('RESIDENTIAL', Validators.required),
    addressLine1: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(255)]),
    addressLine2: this.fb.control<string>('', Validators.maxLength(255)),
    city: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(120)]),
    stateOrProvince: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(120)]),
    postalCode: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(32)]),
    country: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(120)]),
    description: this.fb.control<string>('', Validators.maxLength(2000)),
    ownerships: this.fb.array([])
  });

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadProperty(+id);
    } else {
      this.startCreate();
    }
  }

  get ownerships(): FormArray {
    return this.propertyForm.get('ownerships') as FormArray;
  }

  get isEdit(): boolean {
    return this.propertyForm.getRawValue().id != null;
  }

  formatEnum(value: string): string {
    return formatEnum(value);
  }

  startCreate(): void {
    this.loadError = '';
    this.propertyForm.reset({
      id: null,
      name: '',
      propertyType: 'RESIDENTIAL',
      addressLine1: '',
      addressLine2: '',
      city: '',
      stateOrProvince: '',
      postalCode: '',
      country: '',
      description: ''
    });
    this.ownerships.clear();
    this.addOwnership();
  }

  loadProperty(id: number): void {
    this.loadError = '';
    this.api.get(id).subscribe({
      next: (property) => this.populateFrom(property),
      error: (err) => {
        this.loadError = httpErrorMessage(err, 'Unable to load property');
      }
    });
  }

  private populateFrom(property: PropertyRecord): void {
    this.propertyForm.patchValue({
      id: property.id,
      name: property.name,
      propertyType: property.propertyType,
      addressLine1: property.addressLine1,
      addressLine2: property.addressLine2 ?? '',
      city: property.city,
      stateOrProvince: property.stateOrProvince,
      postalCode: property.postalCode,
      country: property.country,
      description: property.description ?? ''
    });
    this.ownerships.clear();
    if (property.ownerships.length === 0) {
      this.addOwnership();
      return;
    }
    property.ownerships.forEach((ownership) => {
      this.ownerships.push(this.createOwnershipGroup(ownership));
    });
  }

  addOwnership(): void {
    this.ownerships.push(this.createOwnershipGroup());
  }

  removeOwnership(index: number): void {
    if (this.ownerships.length === 1) {
      this.ownerships.at(0).reset({
        ownerPartyId: '',
        role: 'PRIMARY_OWNER',
        ownershipPercentage: 100,
        effectiveFrom: this.today(),
        effectiveTo: '',
        notes: ''
      });
      return;
    }
    this.ownerships.removeAt(index);
  }

  saveProperty(): void {
    if (this.propertyForm.invalid) {
      this.propertyForm.markAllAsTouched();
      return;
    }

    const payload = this.toPayload();
    const propertyId = this.propertyForm.getRawValue().id;
    this.saveInFlight = true;
    this.errorMessage = '';
    this.successMessage = '';

    const request = propertyId
      ? this.api.update(propertyId, payload)
      : this.api.create(payload);

    request.subscribe({
      next: (property) => {
        this.saveInFlight = false;
        this.successMessage = propertyId ? `Updated ${property.name}.` : `Created ${property.name}.`;
        this.populateFrom(property);
        this.router.navigate(['/properties', property.id]);
      },
      error: (error) => {
        this.saveInFlight = false;
        this.errorMessage = httpErrorMessage(error, 'Unable to save property');
      }
    });
  }

  deleteProperty(): void {
    const propertyId = this.propertyForm.getRawValue().id;
    if (!propertyId || !window.confirm('Delete this property? This cannot be undone.')) {
      return;
    }

    this.deleteInFlight = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.api.delete(propertyId).subscribe({
      next: () => {
        this.deleteInFlight = false;
        this.router.navigate(['/properties']);
      },
      error: (error) => {
        this.deleteInFlight = false;
        this.errorMessage = httpErrorMessage(error, 'Unable to delete property');
      }
    });
  }

  private createOwnershipGroup(ownership?: Partial<Ownership>) {
    return this.fb.group({
      ownerPartyId: this.fb.nonNullable.control(ownership?.ownerPartyId ?? '', [
        Validators.required,
        Validators.maxLength(128)
      ]),
      role: this.fb.nonNullable.control<OwnershipRole>(ownership?.role ?? 'PRIMARY_OWNER', Validators.required),
      ownershipPercentage: this.fb.nonNullable.control<number>(ownership?.ownershipPercentage ?? 100, [
        Validators.required,
        Validators.min(0),
        Validators.max(100)
      ]),
      effectiveFrom: this.fb.nonNullable.control(ownership?.effectiveFrom ?? this.today(), Validators.required),
      effectiveTo: this.fb.control<string>(ownership?.effectiveTo ?? ''),
      notes: this.fb.control<string>(ownership?.notes ?? '', Validators.maxLength(1000))
    });
  }

  private toPayload(): PropertyPayload {
    const rawValue = this.propertyForm.getRawValue();

    return {
      name: rawValue.name.trim(),
      propertyType: rawValue.propertyType,
      addressLine1: rawValue.addressLine1.trim(),
      addressLine2: rawValue.addressLine2?.trim() || null,
      city: rawValue.city.trim(),
      stateOrProvince: rawValue.stateOrProvince.trim(),
      postalCode: rawValue.postalCode.trim(),
      country: rawValue.country.trim(),
      description: rawValue.description?.trim() || null,
      ownerships: (rawValue.ownerships as OwnershipFormValue[]).map((ownership) => ({
        ownerPartyId: ownership.ownerPartyId.trim(),
        role: ownership.role,
        ownershipPercentage: Number(ownership.ownershipPercentage),
        effectiveFrom: ownership.effectiveFrom,
        effectiveTo: ownership.effectiveTo || null,
        notes: ownership.notes?.trim() || null
      }))
    };
  }

  private today(): string {
    return new Date().toISOString().slice(0, 10);
  }
}

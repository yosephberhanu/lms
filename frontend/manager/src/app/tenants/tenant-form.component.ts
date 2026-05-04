import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { httpErrorMessage } from '../core/http-error-message';
import type { TenantPayload, TenantRecord } from '../models/lms.models';
import { TenantApiService } from '../services/tenant-api.service';

@Component({
  selector: 'app-tenant-form',
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './tenant-form.component.html',
  styleUrl: './tenant-form.component.css'
})
export class TenantFormComponent implements OnInit {
  private readonly api = inject(TenantApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  saveInFlight = false;
  deleteInFlight = false;
  loadError = '';
  errorMessage = '';

  readonly form = this.fb.nonNullable.group({
    externalPartyId: ['', Validators.maxLength(128)],
    displayName: ['', [Validators.required, Validators.maxLength(255)]],
    email: ['', [Validators.email, Validators.maxLength(255)]],
    phone: ['', Validators.maxLength(50)],
    status: ['', Validators.maxLength(64)]
  });

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.load(+id);
    }
  }

  get isEdit(): boolean {
    return this.route.snapshot.paramMap.has('id');
  }

  private load(id: number): void {
    this.loadError = '';
    this.api.get(id).subscribe({
      next: (tenant) => this.populate(tenant),
      error: (err) => {
        this.loadError = httpErrorMessage(err, 'Unable to load tenant');
      }
    });
  }

  private populate(tenant: TenantRecord): void {
    this.form.reset({
      externalPartyId: tenant.externalPartyId ?? '',
      displayName: tenant.displayName,
      email: tenant.email ?? '',
      phone: tenant.phone ?? '',
      status: tenant.status ?? ''
    });
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const raw = this.form.getRawValue();
    const payload: TenantPayload = {
      externalPartyId: raw.externalPartyId.trim() || null,
      displayName: raw.displayName.trim(),
      email: raw.email.trim() || null,
      phone: raw.phone.trim() || null,
      status: raw.status.trim() || null
    };

    const idParam = this.route.snapshot.paramMap.get('id');
    this.saveInFlight = true;
    this.errorMessage = '';

    const req = idParam ? this.api.update(+idParam, payload) : this.api.create(payload);

    req.subscribe({
      next: (tenant) => {
        this.saveInFlight = false;
        this.router.navigate(['/tenants', tenant.id]);
      },
      error: (err) => {
        this.saveInFlight = false;
        this.errorMessage = httpErrorMessage(err, 'Unable to save tenant');
      }
    });
  }

  delete(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id || !window.confirm('Delete this tenant?')) {
      return;
    }
    this.deleteInFlight = true;
    this.api.delete(+id).subscribe({
      next: () => {
        this.deleteInFlight = false;
        this.router.navigate(['/tenants']);
      },
      error: (err) => {
        this.deleteInFlight = false;
        this.errorMessage = httpErrorMessage(err, 'Unable to delete tenant');
      }
    });
  }
}

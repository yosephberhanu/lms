import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { httpErrorMessage } from '../core/http-error-message';
import type { OwnerPayload, OwnerRecord } from '../models/lms.models';
import { OwnerApiService } from '../services/owner-api.service';

@Component({
  selector: 'app-owner-form',
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './owner-form.component.html',
  styleUrl: './owner-form.component.css'
})
export class OwnerFormComponent implements OnInit {
  private readonly api = inject(OwnerApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  saveInFlight = false;
  deleteInFlight = false;
  loadError = '';
  errorMessage = '';
  successMessage = '';

  readonly form = this.fb.nonNullable.group({
    partyId: ['', [Validators.required, Validators.maxLength(128)]],
    displayName: ['', [Validators.required, Validators.maxLength(255)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(255)]],
    phone: ['', Validators.maxLength(50)]
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
      next: (owner) => this.populate(owner),
      error: (err) => {
        this.loadError = httpErrorMessage(err, 'Unable to load owner');
      }
    });
  }

  private populate(owner: OwnerRecord): void {
    this.form.reset({
      partyId: owner.partyId,
      displayName: owner.displayName,
      email: owner.email,
      phone: owner.phone ?? ''
    });
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const raw = this.form.getRawValue();
    const payload: OwnerPayload = {
      partyId: raw.partyId.trim(),
      displayName: raw.displayName.trim(),
      email: raw.email.trim(),
      phone: raw.phone.trim() || ''
    };

    const idParam = this.route.snapshot.paramMap.get('id');
    this.saveInFlight = true;
    this.errorMessage = '';
    this.successMessage = '';

    const req = idParam
      ? this.api.update(+idParam, payload)
      : this.api.create(payload);

    req.subscribe({
      next: (owner) => {
        this.saveInFlight = false;
        this.successMessage = 'Saved.';
        this.router.navigate(['/owners', owner.id]);
      },
      error: (err) => {
        this.saveInFlight = false;
        this.errorMessage = httpErrorMessage(err, 'Unable to save owner');
      }
    });
  }

  delete(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id || !window.confirm('Delete this owner?')) {
      return;
    }
    this.deleteInFlight = true;
    this.api.delete(+id).subscribe({
      next: () => {
        this.deleteInFlight = false;
        this.router.navigate(['/owners']);
      },
      error: (err) => {
        this.deleteInFlight = false;
        this.errorMessage = httpErrorMessage(err, 'Unable to delete owner');
      }
    });
  }
}

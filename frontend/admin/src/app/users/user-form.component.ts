import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { httpErrorMessage } from '../core/http-error-message';
import type { KeycloakRealmRole } from '../models/keycloak-role.model';
import { KeycloakAdminApiService } from '../services/auth-service-api.service';

@Component({
  selector: 'app-user-form',
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './user-form.component.html',
  styleUrl: './user-form.component.css'
})
export class UserFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(KeycloakAdminApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  isCreate = true;
  private userId: string | null = null;

  loading = true;
  saving = false;
  errorMessage = '';

  availableRoles: KeycloakRealmRole[] = [];

  readonly form = this.fb.nonNullable.group({
    username: ['', [Validators.required, Validators.maxLength(128)]],
    email: ['', [Validators.maxLength(255)]],
    phone: ['', [Validators.maxLength(64)]],
    firstName: ['', [Validators.maxLength(128)]],
    lastName: ['', [Validators.maxLength(128)]],
    password: ['', [Validators.maxLength(128)]],
    realmRoles: this.fb.nonNullable.control<string[]>([]),
    enabled: [true]
  });

  ngOnInit(): void {
    this.isCreate = this.route.snapshot.data['userFormMode'] === 'create';
    this.userId = this.route.snapshot.paramMap.get('id');
    if (!this.isCreate) {
      this.form.controls.username.disable();
    }

    const roles$ = this.api.listRealmRoles({ prefix: 'lms-' });
    const user$ = this.isCreate
      ? of(null)
      : this.userId
        ? this.api.get(this.userId)
        : of(null);

    forkJoin({ roles: roles$, user: user$ }).subscribe({
      next: ({ roles, user }) => {
        this.availableRoles = roles;
        if (!this.isCreate) {
          if (!this.userId) {
            this.errorMessage = 'Missing user id';
            this.loading = false;
            return;
          }
          if (!user) {
            this.errorMessage = 'Unable to load user';
            this.loading = false;
            return;
          }
          this.form.patchValue({
            username: user.username,
            email: user.email ?? '',
            phone: user.phone ?? '',
            firstName: user.firstName ?? '',
            lastName: user.lastName ?? '',
            password: '',
            realmRoles: user.realmRoles ?? [],
            enabled: user.enabled
          });
        }
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = httpErrorMessage(err, 'Unable to load form data');
        this.loading = false;
      }
    });
  }

  toggleRole(roleName: string, checked: boolean): void {
    const current = this.form.controls.realmRoles.value ?? [];
    const next = checked ? Array.from(new Set([...current, roleName])) : current.filter((r) => r !== roleName);
    this.form.controls.realmRoles.setValue(next);
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving = true;
    this.errorMessage = '';
    if (this.isCreate) {
      const v = this.form.getRawValue();
      this.api
        .create({
          username: v.username.trim(),
          email: v.email.trim() || null,
          phone: v.phone.trim() || null,
          firstName: v.firstName.trim() || null,
          lastName: v.lastName.trim() || null,
          password: v.password.trim() || null,
          realmRoles: v.realmRoles,
          enabled: v.enabled
        })
        .subscribe({
          next: () => {
            this.saving = false;
            this.router.navigate(['/users']);
          },
          error: (err) => {
            this.saving = false;
            this.errorMessage = httpErrorMessage(err, 'Unable to create user');
          }
        });
    } else if (this.userId) {
      const v = this.form.getRawValue();
      this.api
        .update(this.userId, {
          email: v.email.trim() || null,
          phone: v.phone.trim() || null,
          firstName: v.firstName.trim() || null,
          lastName: v.lastName.trim() || null,
          password: v.password.trim() || null,
          realmRoles: v.realmRoles,
          enabled: v.enabled
        })
        .subscribe({
          next: () => {
            this.saving = false;
            this.router.navigate(['/users']);
          },
          error: (err) => {
            this.saving = false;
            this.errorMessage = httpErrorMessage(err, 'Unable to update user');
          }
        });
    }
  }
}

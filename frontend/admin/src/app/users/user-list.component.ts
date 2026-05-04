import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { httpErrorMessage } from '../core/http-error-message';
import type { KeycloakUser } from '../models/keycloak-user.model';
import { KeycloakAdminApiService } from '../services/auth-service-api.service';

@Component({
  selector: 'app-user-list',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './user-list.component.html',
  styleUrl: './user-list.component.css'
})
export class UserListComponent implements OnInit {
  private readonly api = inject(KeycloakAdminApiService);

  rows: KeycloakUser[] = [];
  loading = true;
  errorMessage = '';
  search = '';
  deleteError = '';
  deletingId: string | null = null;

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.errorMessage = '';
    this.deleteError = '';
    this.api.list({ search: this.search || undefined, first: 0, max: 100, includeRoles: true }).subscribe({
      next: (rows) => {
        // Some deployments omit realmRoles in list() to avoid N+1 calls.
        // If roles are missing/empty, hydrate them via per-user GET.
        const missingRoles = rows.some((u) => !u.realmRoles || u.realmRoles.length === 0);
        if (!missingRoles || rows.length === 0) {
          this.rows = rows;
          this.loading = false;
          return;
        }
        forkJoin(
          rows.map((u) =>
            this.api.get(u.id).pipe(
              catchError(() => of(u))
            )
          )
        ).subscribe({
          next: (fullRows) => {
            this.rows = fullRows;
            this.loading = false;
          },
          error: () => {
            // Even if hydration fails unexpectedly, fall back to original list.
            this.rows = rows;
            this.loading = false;
          }
        });
      },
      error: (err) => {
        this.errorMessage = httpErrorMessage(err, 'Unable to load users');
        this.loading = false;
      }
    });
  }

  deleteUser(u: KeycloakUser): void {
    if (!confirm(`Delete Keycloak user "${u.username}"? This cannot be undone.`)) {
      return;
    }
    this.deletingId = u.id;
    this.deleteError = '';
    this.api.delete(u.id).subscribe({
      next: () => {
        this.deletingId = null;
        this.load();
      },
      error: (err) => {
        this.deletingId = null;
        this.deleteError = httpErrorMessage(err, 'Unable to delete user');
      }
    });
  }
}

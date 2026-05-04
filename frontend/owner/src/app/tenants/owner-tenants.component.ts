import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { forkJoin, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { httpErrorMessage } from '../core/http-error-message';
import type { LeaseRecord, TenantRecord } from '../models/lms.models';
import { LeaseApiService } from '../services/lease-api.service';
import { OwnerSessionService } from '../services/owner-session.service';
import { TenantApiService } from '../services/tenant-api.service';

export interface OwnerTenantRow {
  tenantId: number;
  displayName: string;
  email: string | null;
  phone: string | null;
  leaseCount: number;
}

@Component({
  selector: 'app-owner-tenants',
  imports: [CommonModule],
  templateUrl: './owner-tenants.component.html',
  styleUrl: './owner-tenants.component.css'
})
export class OwnerTenantsComponent implements OnInit {
  readonly session = inject(OwnerSessionService);
  private readonly leasesApi = inject(LeaseApiService);
  private readonly tenantsApi = inject(TenantApiService);

  rows: OwnerTenantRow[] = [];
  loading = true;
  errorMessage = '';

  ngOnInit(): void {
    const pid = this.session.currentPartyId();
    this.leasesApi.list({ propertyOwnerPartyId: pid }).subscribe({
      next: (leases) => {
        this.buildRows(leases);
      },
      error: (err) => {
        this.errorMessage = httpErrorMessage(err, 'Unable to load tenants');
        this.loading = false;
      }
    });
  }

  private buildRows(leases: LeaseRecord[]): void {
    const counts = new Map<number, number>();
    for (const l of leases) {
      counts.set(l.tenantId, (counts.get(l.tenantId) ?? 0) + 1);
    }
    const tenantIds = [...counts.keys()];
    if (tenantIds.length === 0) {
      this.rows = [];
      this.loading = false;
      return;
    }

    forkJoin(
      tenantIds.map((tenantId) =>
        this.tenantsApi.get(tenantId).pipe(
          map((t) => ({ tenantId, t })),
          catchError(() => of({ tenantId, t: null as TenantRecord | null }))
        )
      )
    ).subscribe({
      next: (pairs) => {
        this.rows = pairs
          .map(({ tenantId, t }) => {
            const lease = leases.find((l) => l.tenantId === tenantId);
            return {
              tenantId,
              displayName:
                t?.displayName ??
                lease?.tenant?.displayName ??
                lease?.tenantNameSnapshot ??
                `Tenant #${tenantId}`,
              email: t?.email ?? lease?.tenant?.email ?? null,
              phone: t?.phone ?? lease?.tenant?.phone ?? null,
              leaseCount: counts.get(tenantId) ?? 0
            };
          })
          .sort((a, b) => a.displayName.localeCompare(b.displayName));
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = httpErrorMessage(err, 'Unable to load tenant details');
        this.loading = false;
      }
    });
  }
}

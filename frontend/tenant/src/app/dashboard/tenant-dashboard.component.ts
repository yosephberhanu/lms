import { CommonModule, CurrencyPipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { catchError, forkJoin, of } from 'rxjs';
import { httpErrorMessage } from '../core/http-error-message';
import type { LeaseRecord, TenantRecord } from '../models/lms.models';
import { LeaseApiService } from '../services/lease-api.service';
import { TenantApiService } from '../services/tenant-api.service';
import { TenantSessionService } from '../services/tenant-session.service';

@Component({
  selector: 'app-tenant-dashboard',
  imports: [CommonModule, RouterLink, CurrencyPipe],
  templateUrl: './tenant-dashboard.component.html',
  styleUrl: './tenant-dashboard.component.css'
})
export class TenantDashboardComponent implements OnInit {
  readonly session = inject(TenantSessionService);
  private readonly tenantApi = inject(TenantApiService);
  private readonly leasesApi = inject(LeaseApiService);

  tenant: TenantRecord | null = null;
  leases: LeaseRecord[] = [];
  loading = true;
  errorMessage = '';

  ngOnInit(): void {
    const tid = this.session.currentTenantId();
    this.loading = true;
    forkJoin({
      tenant: this.tenantApi.get(tid).pipe(catchError(() => of(null as TenantRecord | null))),
      leases: this.leasesApi.list({ tenantId: tid })
    }).subscribe({
      next: ({ tenant, leases }) => {
        this.tenant = tenant;
        this.leases = leases;
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = httpErrorMessage(err, 'Unable to load dashboard');
        this.loading = false;
      }
    });
  }

  get pendingMyApproval(): LeaseRecord[] {
    return this.leases.filter((l) => l.status === 'PENDING_APPROVAL' && !l.tenantApprovedAt);
  }

  get activeLeases(): LeaseRecord[] {
    return this.leases.filter((l) => l.status === 'ACTIVE');
  }

  get monthlyRentActive(): number {
    return this.activeLeases.reduce((s, l) => s + Number(l.monthlyRent ?? 0), 0);
  }
}

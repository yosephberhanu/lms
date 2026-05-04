import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { httpErrorMessage } from '../core/http-error-message';
import type { LeaseRecord } from '../models/lms.models';
import { LeaseApiService } from '../services/lease-api.service';
import { TenantSessionService } from '../services/tenant-session.service';
import { formatEnum } from '../shared/format-enum';

@Component({
  selector: 'app-tenant-lease-list',
  imports: [CommonModule, RouterLink, CurrencyPipe, DatePipe],
  templateUrl: './tenant-lease-list.component.html',
  styleUrl: './tenant-lease-list.component.css'
})
export class TenantLeaseListComponent implements OnInit {
  readonly session = inject(TenantSessionService);
  private readonly api = inject(LeaseApiService);
  private readonly route = inject(ActivatedRoute);

  rows: LeaseRecord[] = [];
  loading = true;
  errorMessage = '';
  pendingOnly = false;
  title = 'My leases';

  ngOnInit(): void {
    this.pendingOnly = this.route.snapshot.data['pendingOnly'] === true;
    this.title = this.pendingOnly ? 'Pending my approval' : 'My leases';
    const tid = this.session.currentTenantId();
    this.api
      .list({
        tenantId: tid,
        status: this.pendingOnly ? 'PENDING_APPROVAL' : undefined
      })
      .subscribe({
        next: (rows) => {
          this.rows = this.pendingOnly ? rows.filter((r) => !r.tenantApprovedAt) : rows;
          this.loading = false;
        },
        error: (err) => {
          this.errorMessage = httpErrorMessage(err, 'Unable to load leases');
          this.loading = false;
        }
      });
  }

  formatEnum(v: string): string {
    return formatEnum(v);
  }
}

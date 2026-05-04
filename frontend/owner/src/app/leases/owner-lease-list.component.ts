import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { httpErrorMessage } from '../core/http-error-message';
import type { LeaseRecord } from '../models/lms.models';
import { LeaseApiService } from '../services/lease-api.service';
import { OwnerSessionService } from '../services/owner-session.service';
import { formatEnum } from '../shared/format-enum';

@Component({
  selector: 'app-owner-lease-list',
  imports: [CommonModule, RouterLink, CurrencyPipe, DatePipe],
  templateUrl: './owner-lease-list.component.html',
  styleUrl: './owner-lease-list.component.css'
})
export class OwnerLeaseListComponent implements OnInit {
  readonly session = inject(OwnerSessionService);
  private readonly api = inject(LeaseApiService);
  private readonly route = inject(ActivatedRoute);

  rows: LeaseRecord[] = [];
  loading = true;
  errorMessage = '';
  pendingOnly = false;
  title = 'My leases';

  ngOnInit(): void {
    this.pendingOnly = this.route.snapshot.data['pendingOnly'] === true;
    this.title = this.pendingOnly ? 'Pending approvals' : 'My leases';
    const pid = this.session.currentPartyId();
    this.api
      .list({
        propertyOwnerPartyId: pid,
        status: this.pendingOnly ? 'PENDING_APPROVAL' : undefined
      })
      .subscribe({
        next: (rows) => {
          this.rows = this.pendingOnly
            ? rows.filter(
                (r) =>
                  !r.ownerApprovedAt &&
                  !!r.ownerId &&
                  r.ownerId.trim() === pid
              )
            : rows;
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

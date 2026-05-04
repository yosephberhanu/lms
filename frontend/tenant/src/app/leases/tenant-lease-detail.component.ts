import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { httpErrorMessage } from '../core/http-error-message';
import type { LeasePaymentsResponse, LeaseRecord, PropertyRecord } from '../models/lms.models';
import type { LeaseAttachment } from '../models/lease-attachments.models';
import { LeaseApiService } from '../services/lease-api.service';
import { PropertyApiService } from '../services/property-api.service';
import { TenantSessionService } from '../services/tenant-session.service';
import { formatEnum } from '../shared/format-enum';

@Component({
  selector: 'app-tenant-lease-detail',
  imports: [CommonModule, RouterLink, CurrencyPipe, DatePipe],
  templateUrl: './tenant-lease-detail.component.html',
  styleUrl: './tenant-lease-detail.component.css'
})
export class TenantLeaseDetailComponent implements OnInit, OnDestroy {
  private readonly api = inject(LeaseApiService);
  private readonly propertyApi = inject(PropertyApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly sanitizer = inject(DomSanitizer);
  readonly session = inject(TenantSessionService);

  lease: LeaseRecord | null = null;
  propertyDetail: PropertyRecord | null = null;
  /** This tenant’s other leases on the same property (newest first). */
  leasesOnProperty: LeaseRecord[] = [];
  attachments: LeaseAttachment[] = [];
  attachmentsError = '';
  previewOpen = false;
  previewTitle = '';
  previewUrl: string | null = null;
  previewSafeUrl: SafeResourceUrl | null = null;
  previewError = '';
  previewLoading = false;
  errorMessage = '';
  loading = true;
  approvalInFlight = false;
  paymentInFlightPeriod: string | null = null;
  notYourLease = false;

  payments: LeasePaymentsResponse | null = null;
  paymentsLoading = false;
  paymentsError = '';
  paymentBanner: 'success' | 'cancel' | null = null;
  private paymentsRefreshTimer: number | null = null;
  private lastStripeSessionId: string | null = null;

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.errorMessage = 'Missing id';
      this.loading = false;
      return;
    }
    const tid = this.session.currentTenantId();
    const paymentQ = this.route.snapshot.queryParamMap.get('payment');
    if (paymentQ === 'success' || paymentQ === 'cancel') {
      this.paymentBanner = paymentQ;
    }
    const sid = this.route.snapshot.queryParamMap.get('session_id');
    if (sid?.trim()) {
      this.lastStripeSessionId = sid.trim();
    }
    this.api.get(+id).subscribe({
      next: (lease) => {
        if (lease.tenantId !== tid) {
          this.notYourLease = true;
          this.lease = null;
          this.loading = false;
          return;
        }
        this.notYourLease = false;
        this.lease = lease;
        this.loadAttachments();
        this.loadPayments();
        if (this.paymentBanner === 'success') {
          this.confirmStripeSessionIfPresent();
          this.schedulePaymentsAutoRefresh();
        }
        forkJoin({
          property: this.propertyApi.get(lease.propertyId).pipe(catchError(() => of(null))),
          onProperty: this.api.list({ tenantId: tid, propertyId: lease.propertyId })
        }).subscribe({
          next: ({ property, onProperty }) => {
            this.propertyDetail = property;
            this.leasesOnProperty = [...onProperty].sort((a, b) => b.id - a.id);
            this.loading = false;
          },
          error: (err) => {
            this.errorMessage = httpErrorMessage(err, 'Unable to load property or related leases');
            this.loading = false;
          }
        });
      },
      error: (err) => {
        this.errorMessage = httpErrorMessage(err, 'Unable to load lease');
        this.loading = false;
      }
    });
  }

  refreshPayments(): void {
    this.loadPayments();
  }

  ngOnDestroy(): void {
    this.clearPaymentsRefreshTimer();
  }

  private confirmStripeSessionIfPresent(): void {
    if (!this.lease || !this.lastStripeSessionId) {
      return;
    }
    const tid = this.session.currentTenantId();
    this.api.confirmCheckoutSession(this.lease.id, tid, this.lastStripeSessionId).subscribe({
      next: () => {
        // Once confirmed, refresh payments immediately (webhook fallback)
        this.loadPayments();
      },
      error: () => {
        // Ignore confirm errors; auto-refresh + manual refresh still available
      }
    });
  }

  private loadAttachments(): void {
    if (!this.lease) {
      return;
    }
    const tid = this.session.currentTenantId();
    this.attachmentsError = '';
    this.api.listAttachments(this.lease.id, tid).subscribe({
      next: (items) => {
        this.attachments = items ?? [];
      },
      error: (err) => {
        this.attachmentsError = httpErrorMessage(err, 'Unable to load attachments');
      }
    });
  }

  private loadPayments(): void {
    if (!this.lease) {
      return;
    }
    const tid = this.session.currentTenantId();
    this.paymentsLoading = true;
    this.paymentsError = '';
    this.api.payments(this.lease.id, tid).subscribe({
      next: (resp) => {
        this.payments = resp;
        this.paymentsLoading = false;
      },
      error: (err) => {
        this.paymentsLoading = false;
        this.paymentsError = httpErrorMessage(err, 'Unable to load payments');
      }
    });
  }

  private schedulePaymentsAutoRefresh(): void {
    // Stripe redirects back immediately; confirmation can lag a bit.
    // Single delayed refresh (no looping).
    this.clearPaymentsRefreshTimer();
    this.paymentsRefreshTimer = window.setTimeout(() => {
      this.loadPayments();
      this.clearPaymentsRefreshTimer();
    }, 1200);
  }

  private clearPaymentsRefreshTimer(): void {
    if (this.paymentsRefreshTimer != null) {
      window.clearTimeout(this.paymentsRefreshTimer);
      this.paymentsRefreshTimer = null;
    }
  }

  pay(period: string): void {
    if (!this.lease) {
      return;
    }
    if (this.paymentInFlightPeriod) {
      return;
    }
    const tid = this.session.currentTenantId();
    this.paymentInFlightPeriod = period;
    this.paymentsError = '';
    this.api.createCheckoutSession(this.lease.id, tid, { period }).subscribe({
      next: (resp) => {
        this.paymentInFlightPeriod = null;
        if (resp?.url) {
          window.location.href = resp.url;
        } else {
          this.paymentsError = 'Checkout URL was not returned by the server.';
        }
      },
      error: (err) => {
        this.paymentInFlightPeriod = null;
        this.paymentsError = httpErrorMessage(err, 'Unable to start checkout');
      }
    });
  }

  downloadAttachment(a: LeaseAttachment): void {
    if (!this.lease) {
      return;
    }
    const tid = this.session.currentTenantId();
    this.api.downloadAttachment(this.lease.id, a.id, tid).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = a.originalFileName || 'attachment';
        link.click();
        URL.revokeObjectURL(url);
      },
      error: (err) => {
        this.attachmentsError = httpErrorMessage(err, 'Unable to download attachment');
      }
    });
  }

  viewAttachment(a: LeaseAttachment): void {
    if (!this.lease) {
      return;
    }
    const tid = this.session.currentTenantId();
    this.previewError = '';
    this.previewLoading = true;
    this.previewTitle = a.originalFileName;
    this.previewOpen = true;
    this.api.downloadAttachment(this.lease.id, a.id, tid).subscribe({
      next: (blob) => {
        this.previewLoading = false;
        if (this.previewUrl) {
          URL.revokeObjectURL(this.previewUrl);
        }
        this.previewUrl = URL.createObjectURL(blob);
        this.previewSafeUrl = this.sanitizer.bypassSecurityTrustResourceUrl(this.previewUrl);
      },
      error: (err) => {
        this.previewLoading = false;
        this.previewError = httpErrorMessage(err, 'Unable to load attachment');
      }
    });
  }

  closePreview(): void {
    this.previewOpen = false;
    this.previewError = '';
    this.previewLoading = false;
    this.previewTitle = '';
    this.previewSafeUrl = null;
    if (this.previewUrl) {
      URL.revokeObjectURL(this.previewUrl);
      this.previewUrl = null;
    }
  }

  formatEnum(v: string): string {
    return formatEnum(v);
  }

  get canApprove(): boolean {
    const l = this.lease;
    const tid = this.session.currentTenantId();
    return !!l && l.status === 'PENDING_APPROVAL' && !l.tenantApprovedAt && l.tenantId === tid;
  }

  approve(): void {
    if (!this.lease) {
      return;
    }
    const tid = this.session.currentTenantId();
    this.approvalInFlight = true;
    this.errorMessage = '';
    this.api.approveAsTenant(this.lease.id, tid).subscribe({
      next: (lease) => {
        this.lease = lease;
        this.approvalInFlight = false;
      },
      error: (err) => {
        this.approvalInFlight = false;
        this.errorMessage = httpErrorMessage(err, 'Unable to record approval');
      }
    });
  }
}

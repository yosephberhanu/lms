import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { catchError, forkJoin, map, of, switchMap } from 'rxjs';
import { httpErrorMessage } from '../core/http-error-message';
import type { LeaseRecord, PropertyRecord } from '../models/lms.models';
import type { LeaseAttachment } from '../models/lease-attachments.models';
import { LeaseApiService } from '../services/lease-api.service';
import { OwnerSessionService } from '../services/owner-session.service';
import { PropertyApiService } from '../services/property-api.service';
import { formatEnum } from '../shared/format-enum';
import { SafeUrlPipe } from '../shared/safe-url.pipe';

@Component({
  selector: 'app-owner-lease-detail',
  imports: [CommonModule, RouterLink, CurrencyPipe, DatePipe, SafeUrlPipe],
  templateUrl: './owner-lease-detail.component.html',
  styleUrl: './owner-lease-detail.component.css'
})
export class OwnerLeaseDetailComponent implements OnInit {
  private readonly api = inject(LeaseApiService);
  private readonly propertyApi = inject(PropertyApiService);
  private readonly route = inject(ActivatedRoute);
  readonly session = inject(OwnerSessionService);

  lease: LeaseRecord | null = null;
  /** Loaded for co-owner banner; may be null if property GET failed. */
  propertyForLease: PropertyRecord | null = null;
  attachments: LeaseAttachment[] = [];
  attachmentsError = '';
  previewOpen = false;
  previewTitle = '';
  previewUrl: string | null = null;
  previewError = '';
  previewLoading = false;
  errorMessage = '';
  loading = true;
  actionInFlight = false;
  notYourLease = false;

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.errorMessage = 'Missing id';
      this.loading = false;
      return;
    }
    const party = this.session.currentPartyId();
    forkJoin({
      lease: this.api.get(+id),
      portfolio: this.propertyApi.list({ ownerPartyId: party })
    })
      .pipe(
        switchMap(({ lease, portfolio }) => {
          const inPortfolio = portfolio.some((p) => p.id === lease.propertyId);
          const designated = (lease.ownerId ?? '').trim() === party;
          if (!designated && !inPortfolio) {
            return of({ lease, property: null as PropertyRecord | null, deny: true as const });
          }
          return this.propertyApi.get(lease.propertyId).pipe(
            map((property) => ({ lease, property, deny: false as const })),
            catchError(() => of({ lease, property: null as PropertyRecord | null, deny: false as const }))
          );
        })
      )
      .subscribe({
        next: ({ lease, property, deny }) => {
          this.propertyForLease = property;
          if (deny) {
            this.notYourLease = true;
            this.lease = null;
          } else {
            this.notYourLease = false;
            this.lease = lease;
            this.loadAttachments();
          }
          this.loading = false;
        },
        error: (err) => {
          this.errorMessage = httpErrorMessage(err, 'Unable to load lease');
          this.loading = false;
        }
      });
  }

  private loadAttachments(): void {
    if (!this.lease) {
      return;
    }
    const party = this.session.currentPartyId();
    this.attachmentsError = '';
    this.api.listAttachments(this.lease.id, party).subscribe({
      next: (items) => {
        this.attachments = items ?? [];
      },
      error: (err) => {
        this.attachmentsError = httpErrorMessage(err, 'Unable to load attachments');
      }
    });
  }

  downloadAttachment(a: LeaseAttachment): void {
    if (!this.lease) {
      return;
    }
    const party = this.session.currentPartyId();
    this.api.downloadAttachment(this.lease.id, a.id, party).subscribe({
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
    const party = this.session.currentPartyId();
    this.previewError = '';
    this.previewLoading = true;
    this.previewTitle = a.originalFileName;
    this.previewOpen = true;
    this.api.downloadAttachment(this.lease.id, a.id, party).subscribe({
      next: (blob) => {
        this.previewLoading = false;
        if (this.previewUrl) {
          URL.revokeObjectURL(this.previewUrl);
        }
        this.previewUrl = URL.createObjectURL(blob);
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
    if (this.previewUrl) {
      URL.revokeObjectURL(this.previewUrl);
      this.previewUrl = null;
    }
  }

  formatEnum(v: string): string {
    return formatEnum(v);
  }

  coOwnerViewOnly(): boolean {
    const l = this.lease;
    if (!l) {
      return false;
    }
    const party = this.session.currentPartyId();
    if ((l.ownerId ?? '').trim() === party) {
      return false;
    }
    const prop = this.propertyForLease;
    if (prop?.ownerships?.length) {
      return prop.ownerships.some((o) => (o.ownerPartyId ?? '').trim() === party);
    }
    return true;
  }

  get canApprove(): boolean {
    const l = this.lease;
    const party = this.session.currentPartyId();
    return (
      !!l &&
      l.status === 'PENDING_APPROVAL' &&
      !l.ownerApprovedAt &&
      !!l.ownerId &&
      l.ownerId.trim() === party
    );
  }

  approve(): void {
    if (!this.lease) {
      return;
    }
    const party = this.session.currentPartyId();
    this.actionInFlight = true;
    this.errorMessage = '';
    this.api.approveAsOwner(this.lease.id, party).subscribe({
      next: (lease) => {
        this.lease = lease;
        this.actionInFlight = false;
      },
      error: (err) => {
        this.actionInFlight = false;
        this.errorMessage = httpErrorMessage(err, 'Unable to record approval');
      }
    });
  }
}

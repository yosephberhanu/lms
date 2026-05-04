import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Observable } from 'rxjs';
import { httpErrorMessage } from '../core/http-error-message';
import type { LeaseRecord } from '../models/lms.models';
import type { LeaseAttachment } from '../models/lease-attachments.models';
import { SafeUrlPipe } from '../shared/safe-url.pipe';
import { LeaseApiService } from '../services/lease-api.service';
import { formatEnum } from '../shared/format-enum';

@Component({
  selector: 'app-lease-detail',
  imports: [CommonModule, RouterLink, CurrencyPipe, DatePipe, SafeUrlPipe],
  templateUrl: './lease-detail.component.html',
  styleUrl: './lease-detail.component.css'
})
export class LeaseDetailComponent implements OnInit {
  private readonly api = inject(LeaseApiService);
  private readonly route = inject(ActivatedRoute);

  lease: LeaseRecord | null = null;
  attachments: LeaseAttachment[] = [];
  attachmentsError = '';
  previewOpen = false;
  previewTitle = '';
  previewUrl: string | null = null;
  previewContentType: string | null = null;
  previewError = '';
  previewLoading = false;
  errorMessage = '';
  loading = true;
  actionInFlight = false;

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.errorMessage = 'Missing id';
      this.loading = false;
      return;
    }
    this.api.get(+id).subscribe({
      next: (lease) => {
        this.lease = lease;
        this.loading = false;
        this.loadAttachments();
      },
      error: (err) => {
        this.errorMessage = httpErrorMessage(err, 'Unable to load lease');
        this.loading = false;
      }
    });
  }

  private leaseId(): number | null {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      return null;
    }
    const n = Number(id);
    return Number.isFinite(n) ? n : null;
  }

  loadAttachments(): void {
    const id = this.leaseId();
    if (!id) {
      this.attachments = [];
      return;
    }
    this.attachmentsError = '';
    this.api.listAttachments(id).subscribe({
      next: (items) => {
        this.attachments = items ?? [];
      },
      error: (err) => {
        this.attachmentsError = httpErrorMessage(err, 'Unable to load attachments');
      }
    });
  }

  attachmentHref(a: LeaseAttachment): string {
    const id = this.lease?.id;
    if (!id) {
      return '#';
    }
    return this.api.attachmentDownloadUrl(id, a.id);
  }

  viewAttachment(a: LeaseAttachment): void {
    if (!this.lease) {
      return;
    }
    this.previewError = '';
    this.previewLoading = true;
    this.previewTitle = a.originalFileName;
    this.previewContentType = a.contentType ?? null;
    this.previewOpen = true;

    this.api.downloadAttachment(this.lease.id, a.id).subscribe({
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
    this.previewContentType = null;
    if (this.previewUrl) {
      URL.revokeObjectURL(this.previewUrl);
      this.previewUrl = null;
    }
  }

  formatEnum(value: string): string {
    return formatEnum(value);
  }

  get canEdit(): boolean {
    return this.lease?.status === 'DRAFT' || this.lease?.status === 'PENDING_APPROVAL';
  }

  get canSubmit(): boolean {
    return this.lease?.status === 'DRAFT';
  }

  get canTerminate(): boolean {
    return this.lease?.status === 'ACTIVE';
  }

  submitForApproval(): void {
    if (!this.lease) {
      return;
    }
    this.runAction(this.api.submitForApproval(this.lease.id));
  }

  terminate(): void {
    if (!this.lease || !window.confirm('Terminate this lease?')) {
      return;
    }
    this.runAction(this.api.terminate(this.lease.id));
  }

  private runAction(action: Observable<LeaseRecord>): void {
    this.actionInFlight = true;
    this.errorMessage = '';
    action.subscribe({
      next: (lease) => {
        this.lease = lease;
        this.actionInFlight = false;
        this.loadAttachments();
      },
      error: (err) => {
        this.actionInFlight = false;
        this.errorMessage = httpErrorMessage(err, 'Unable to update lease');
      }
    });
  }
}

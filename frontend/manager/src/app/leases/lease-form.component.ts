import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { httpErrorMessage } from '../core/http-error-message';
import type { LeaseRecord, LeaseWritePayload } from '../models/lms.models';
import type { PropertyRecord } from '../models/lms.models';
import type { TenantRecord } from '../models/lms.models';
import type { LeaseStatus, Ownership } from '../models/lms.models';
import type { LeaseAttachment } from '../models/lease-attachments.models';
import { LeaseApiService } from '../services/lease-api.service';
import { PropertyApiService } from '../services/property-api.service';
import { TenantApiService } from '../services/tenant-api.service';

@Component({
  selector: 'app-lease-form',
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './lease-form.component.html',
  styleUrl: './lease-form.component.css'
})
export class LeaseFormComponent implements OnInit {
  private readonly leaseApi = inject(LeaseApiService);
  private readonly propertyApi = inject(PropertyApiService);
  private readonly tenantApi = inject(TenantApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  properties: PropertyRecord[] = [];
  tenants: TenantRecord[] = [];
  lookupsLoading = true;
  lookupsError = '';

  saveInFlight = false;
  loadError = '';
  errorMessage = '';
  attachments: LeaseAttachment[] = [];
  attachmentsError = '';
  uploadInFlight = false;
  deleteInFlightId: number | null = null;
  queuedFiles: File[] = [];
  queuedUploadInFlight = false;

  /** Map of propertyId -> current blocking lease status (if any). */
  private blockingLeaseByPropertyId = new Map<number, LeaseStatus>();

  readonly form = this.fb.nonNullable.group({
    propertyId: [null as number | null, Validators.required],
    tenantId: [null as number | null, Validators.required],
    monthlyRent: [0, [Validators.required, Validators.min(0)]],
    startDate: ['', Validators.required],
    endDate: ['', Validators.required],
    depositAmount: [null as number | null],
    paymentSchedule: ['', Validators.maxLength(255)]
  });

  ngOnInit(): void {
    forkJoin({
      properties: this.propertyApi.list({}),
      tenants: this.tenantApi.list({}),
      leases: this.leaseApi.list({})
    }).subscribe({
      next: ({ properties, tenants, leases }) => {
        this.tenants = tenants;
        this.blockingLeaseByPropertyId = this.computeBlockingLeaseStatusByProperty(leases);
        this.properties = this.isEdit ? properties : properties.filter((p) => this.isPropertyAvailable(p.id));
        this.lookupsLoading = false;
        const id = this.route.snapshot.paramMap.get('id');
        if (id) {
          this.loadLease(+id);
        } else {
          const pid = this.route.snapshot.queryParamMap.get('propertyId');
          if (pid) {
            const n = Number(pid);
            if (Number.isFinite(n)) {
              this.form.patchValue({ propertyId: n });
            }
          }
        }
      },
      error: (err) => {
        this.lookupsLoading = false;
        this.lookupsError = httpErrorMessage(err, 'Unable to load lookups');
      }
    });
  }

  get isEdit(): boolean {
    return this.route.snapshot.paramMap.has('id');
  }

  private loadLease(id: number): void {
    this.loadError = '';
    this.leaseApi.get(id).subscribe({
      next: (lease) => {
        this.populate(lease);
        this.loadAttachments();
      },
      error: (err) => {
        this.loadError = httpErrorMessage(err, 'Unable to load lease');
      }
    });
  }

  private populate(lease: LeaseRecord): void {
    if (lease.status !== 'DRAFT' && lease.status !== 'PENDING_APPROVAL') {
      this.loadError = 'This lease can no longer be edited in this form.';
      return;
    }
    this.form.patchValue({
      propertyId: lease.propertyId,
      tenantId: lease.tenantId,
      monthlyRent: Number(lease.monthlyRent),
      startDate: lease.startDate,
      endDate: lease.endDate,
      depositAmount: lease.depositAmount != null ? Number(lease.depositAmount) : null,
      paymentSchedule: lease.paymentSchedule ?? ''
    });
  }

  inferredOwnerPartyId(): string {
    const pid = this.form.controls.propertyId.value;
    if (!pid) {
      return '';
    }
    const prop = this.properties.find((p) => p.id === pid);
    if (!prop) {
      return '';
    }
    return inferPrimaryOwnerPartyId(prop.ownerships) ?? '';
  }

  propertyLeaseStatusLabel(propertyId: number): string {
    const st = this.blockingLeaseByPropertyId.get(propertyId);
    return st ? st : 'AVAILABLE';
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const raw = this.form.getRawValue();
    const inferredOwner = this.inferredOwnerPartyId().trim() || null;
    const payload: LeaseWritePayload = {
      propertyId: raw.propertyId as number,
      tenantId: raw.tenantId as number,
      ownerId: inferredOwner,
      monthlyRent: Number(raw.monthlyRent),
      startDate: raw.startDate,
      endDate: raw.endDate,
      depositAmount: raw.depositAmount != null ? Number(raw.depositAmount) : null,
      paymentSchedule: raw.paymentSchedule.trim() || null
    };

    const idParam = this.route.snapshot.paramMap.get('id');
    this.saveInFlight = true;
    this.errorMessage = '';

    const req = idParam ? this.leaseApi.update(+idParam, payload) : this.leaseApi.create(payload);

    req.subscribe({
      next: (lease) => {
        const isNew = !idParam;
        if (isNew && this.queuedFiles.length) {
          this.uploadQueuedAfterCreate(lease.id);
          return;
        }
        this.saveInFlight = false;
        this.router.navigate(['/leases', lease.id]);
      },
      error: (err) => {
        this.saveInFlight = false;
        this.errorMessage = httpErrorMessage(err, 'Unable to save lease');
      }
    });
  }

  private leaseId(): number | null {
    const idParam = this.route.snapshot.paramMap.get('id');
    return idParam ? +idParam : null;
  }

  loadAttachments(): void {
    const id = this.leaseId();
    if (!id) {
      this.attachments = [];
      return;
    }
    this.attachmentsError = '';
    this.leaseApi.listAttachments(id).subscribe({
      next: (items) => {
        this.attachments = items ?? [];
      },
      error: (err) => {
        this.attachmentsError = httpErrorMessage(err, 'Unable to load attachments');
      }
    });
  }

  onPickFiles(ev: Event): void {
    const id = this.leaseId();
    const input = ev.target as HTMLInputElement | null;
    const files = input?.files ? Array.from(input.files) : [];
    if (files.length === 0) {
      return;
    }
    if (!id) {
      this.queuedFiles = [...this.queuedFiles, ...files];
      if (input) {
        input.value = '';
      }
      return;
    }
    const file = files[0];
    this.uploadInFlight = true;
    this.attachmentsError = '';
    this.leaseApi.uploadAttachment(id, file).subscribe({
      next: () => {
        this.uploadInFlight = false;
        if (input) {
          input.value = '';
        }
        this.loadAttachments();
      },
      error: (err) => {
        this.uploadInFlight = false;
        this.attachmentsError = httpErrorMessage(err, 'Unable to upload attachment');
      }
    });
  }

  removeQueuedFile(idx: number): void {
    this.queuedFiles = this.queuedFiles.filter((_f, i) => i !== idx);
  }

  private uploadQueuedAfterCreate(leaseId: number): void {
    const files = [...this.queuedFiles];
    this.queuedUploadInFlight = true;
    this.attachmentsError = '';

    const uploadNext = (i: number) => {
      if (i >= files.length) {
        this.queuedFiles = [];
        this.queuedUploadInFlight = false;
        this.saveInFlight = false;
        this.router.navigate(['/leases', leaseId]);
        return;
      }
      this.leaseApi.uploadAttachment(leaseId, files[i]).subscribe({
        next: () => uploadNext(i + 1),
        error: (err) => {
          this.queuedUploadInFlight = false;
          this.saveInFlight = false;
          this.attachmentsError = httpErrorMessage(err, 'Some attachments failed to upload');
          this.router.navigate(['/leases', leaseId]);
        }
      });
    };

    uploadNext(0);
  }

  deleteAttachment(a: LeaseAttachment): void {
    const id = this.leaseId();
    if (!id || !window.confirm(`Remove "${a.originalFileName}"?`)) {
      return;
    }
    this.deleteInFlightId = a.id;
    this.attachmentsError = '';
    this.leaseApi.deleteAttachment(id, a.id).subscribe({
      next: () => {
        this.deleteInFlightId = null;
        this.loadAttachments();
      },
      error: (err) => {
        this.deleteInFlightId = null;
        this.attachmentsError = httpErrorMessage(err, 'Unable to delete attachment');
      }
    });
  }

  attachmentHref(a: LeaseAttachment): string {
    const id = this.leaseId();
    if (!id) {
      return '#';
    }
    return this.leaseApi.attachmentDownloadUrl(id, a.id);
  }

  private isPropertyAvailable(propertyId: number): boolean {
    return !this.blockingLeaseByPropertyId.has(propertyId);
  }

  private computeBlockingLeaseStatusByProperty(leases: LeaseRecord[]): Map<number, LeaseStatus> {
    const blocking = new Set<LeaseStatus>(['DRAFT', 'PENDING_APPROVAL', 'ACTIVE']);
    const map = new Map<number, LeaseStatus>();
    for (const l of leases ?? []) {
      if (blocking.has(l.status) && !map.has(l.propertyId)) {
        map.set(l.propertyId, l.status);
      }
    }
    return map;
  }
}

function inferPrimaryOwnerPartyId(ownerships: Ownership[] | null | undefined): string | null {
  const rows = ownerships ?? [];
  const primary = rows.find((o) => o.role === 'PRIMARY_OWNER' && (o.ownerPartyId ?? '').trim());
  if (primary) {
    return primary.ownerPartyId.trim();
  }
  const any = rows.find((o) => (o.ownerPartyId ?? '').trim());
  return any ? any.ownerPartyId.trim() : null;
}

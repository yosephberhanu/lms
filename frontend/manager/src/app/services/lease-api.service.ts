import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import type { LeaseRecord, LeaseWritePayload } from '../models/lms.models';
import type { LeaseStatus } from '../models/lms.models';
import type { LeaseAttachment } from '../models/lease-attachments.models';

const BASE = '/api/lease/api/v1/leases';

@Injectable({ providedIn: 'root' })
export class LeaseApiService {
  private readonly http = inject(HttpClient);

  list(filters: { propertyId?: number; tenantId?: number; status?: LeaseStatus; ownerId?: string }): Observable<LeaseRecord[]> {
    let hp = new HttpParams();
    if (filters.propertyId != null) {
      hp = hp.set('propertyId', String(filters.propertyId));
    }
    if (filters.tenantId != null) {
      hp = hp.set('tenantId', String(filters.tenantId));
    }
    if (filters.status) {
      hp = hp.set('status', filters.status);
    }
    if (filters.ownerId?.trim()) {
      hp = hp.set('ownerId', filters.ownerId.trim());
    }
    return this.http.get<LeaseRecord[]>(BASE, { params: hp });
  }

  get(id: number): Observable<LeaseRecord> {
    return this.http.get<LeaseRecord>(`${BASE}/${id}`);
  }

  create(body: LeaseWritePayload): Observable<LeaseRecord> {
    return this.http.post<LeaseRecord>(BASE, body);
  }

  update(id: number, body: LeaseWritePayload): Observable<LeaseRecord> {
    return this.http.put<LeaseRecord>(`${BASE}/${id}`, body);
  }

  submitForApproval(id: number): Observable<LeaseRecord> {
    return this.http.post<LeaseRecord>(`${BASE}/${id}/submit-for-approval`, {});
  }

  terminate(id: number): Observable<LeaseRecord> {
    return this.http.post<LeaseRecord>(`${BASE}/${id}/terminate`, {});
  }

  listAttachments(leaseId: number): Observable<LeaseAttachment[]> {
    return this.http.get<LeaseAttachment[]>(`${BASE}/${leaseId}/attachments`);
  }

  uploadAttachment(leaseId: number, file: File): Observable<LeaseAttachment> {
    const fd = new FormData();
    fd.set('file', file);
    return this.http.post<LeaseAttachment>(`${BASE}/${leaseId}/attachments`, fd);
  }

  deleteAttachment(leaseId: number, attachmentId: number): Observable<void> {
    return this.http.delete<void>(`${BASE}/${leaseId}/attachments/${attachmentId}`);
  }

  downloadAttachment(leaseId: number, attachmentId: number): Observable<Blob> {
    return this.http.get(`${BASE}/${leaseId}/attachments/${attachmentId}`, { responseType: 'blob' });
  }

  attachmentDownloadUrl(leaseId: number, attachmentId: number): string {
    return `${BASE}/${leaseId}/attachments/${attachmentId}`;
  }
}

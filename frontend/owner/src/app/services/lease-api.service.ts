import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import type { LeaseRecord } from '../models/lms.models';
import type { LeaseStatus } from '../models/lms.models';
import type { LeaseAttachment } from '../models/lease-attachments.models';

const BASE = '/api/lease/api/v1/leases';

@Injectable({ providedIn: 'root' })
export class LeaseApiService {
  private readonly http = inject(HttpClient);

  list(filters: {
    propertyOwnerPartyId?: string;
    ownerId?: string;
    status?: LeaseStatus;
    propertyId?: number;
  }): Observable<LeaseRecord[]> {
    let hp = new HttpParams();
    if (filters.propertyOwnerPartyId?.trim()) {
      hp = hp.set('propertyOwnerPartyId', filters.propertyOwnerPartyId.trim());
    }
    if (filters.ownerId?.trim()) {
      hp = hp.set('ownerId', filters.ownerId.trim());
    }
    if (filters.status) {
      hp = hp.set('status', filters.status);
    }
    if (filters.propertyId != null && Number.isFinite(filters.propertyId)) {
      hp = hp.set('propertyId', String(filters.propertyId));
    }
    return this.http.get<LeaseRecord[]>(BASE, { params: hp });
  }

  get(id: number): Observable<LeaseRecord> {
    return this.http.get<LeaseRecord>(`${BASE}/${id}`);
  }

  approveAsOwner(leaseId: number, ownerPartyId: string): Observable<LeaseRecord> {
    const headers = new HttpHeaders().set('X-Owner-Party-Id', ownerPartyId.trim());
    return this.http.post<LeaseRecord>(`${BASE}/${leaseId}/approve-as-owner`, {}, { headers });
  }

  listAttachments(leaseId: number, ownerPartyId: string): Observable<LeaseAttachment[]> {
    const headers = new HttpHeaders().set('X-Owner-Party-Id', ownerPartyId.trim());
    return this.http.get<LeaseAttachment[]>(`${BASE}/${leaseId}/attachments`, { headers });
  }

  attachmentDownloadUrl(leaseId: number, attachmentId: number, ownerPartyId: string): string {
    // Owner header is required for backend authz; use a request instead of a bare URL if needed.
    // For now, the detail component will download via HttpClient with headers.
    return `${BASE}/${leaseId}/attachments/${attachmentId}`;
  }

  downloadAttachment(leaseId: number, attachmentId: number, ownerPartyId: string): Observable<Blob> {
    const headers = new HttpHeaders().set('X-Owner-Party-Id', ownerPartyId.trim());
    return this.http.get(`${BASE}/${leaseId}/attachments/${attachmentId}`, { headers, responseType: 'blob' });
  }
}

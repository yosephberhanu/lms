import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import type {
  CreateCheckoutSessionRequest,
  CreateCheckoutSessionResponse,
  LeasePaymentsResponse,
  LeaseRecord,
  LeaseStatus
} from '../models/lms.models';
import type { LeaseAttachment } from '../models/lease-attachments.models';

const BASE = '/api/lease/api/v1/leases';

@Injectable({ providedIn: 'root' })
export class LeaseApiService {
  private readonly http = inject(HttpClient);

  list(filters: { tenantId?: number; status?: LeaseStatus; propertyId?: number }): Observable<LeaseRecord[]> {
    let hp = new HttpParams();
    if (filters.tenantId != null && Number.isFinite(filters.tenantId) && filters.tenantId > 0) {
      hp = hp.set('tenantId', String(filters.tenantId));
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

  approveAsTenant(leaseId: number, tenantId: number): Observable<LeaseRecord> {
    const headers = new HttpHeaders().set('X-Tenant-Id', String(tenantId));
    return this.http.post<LeaseRecord>(`${BASE}/${leaseId}/approve-as-tenant`, {}, { headers });
  }

  listAttachments(leaseId: number, tenantId: number): Observable<LeaseAttachment[]> {
    const headers = new HttpHeaders().set('X-Tenant-Id', String(tenantId));
    return this.http.get<LeaseAttachment[]>(`${BASE}/${leaseId}/attachments`, { headers });
  }

  downloadAttachment(leaseId: number, attachmentId: number, tenantId: number): Observable<Blob> {
    const headers = new HttpHeaders().set('X-Tenant-Id', String(tenantId));
    return this.http.get(`${BASE}/${leaseId}/attachments/${attachmentId}`, { headers, responseType: 'blob' });
  }

  payments(leaseId: number, tenantId: number): Observable<LeasePaymentsResponse> {
    const headers = new HttpHeaders().set('X-Tenant-Id', String(tenantId));
    return this.http.get<LeasePaymentsResponse>(`${BASE}/${leaseId}/payments`, { headers });
  }

  createCheckoutSession(
    leaseId: number,
    tenantId: number,
    body: CreateCheckoutSessionRequest
  ): Observable<CreateCheckoutSessionResponse> {
    const headers = new HttpHeaders().set('X-Tenant-Id', String(tenantId));
    return this.http.post<CreateCheckoutSessionResponse>(`${BASE}/${leaseId}/payments/checkout-session`, body, { headers });
  }

  confirmCheckoutSession(leaseId: number, tenantId: number, sessionId: string): Observable<void> {
    const headers = new HttpHeaders().set('X-Tenant-Id', String(tenantId));
    return this.http.post<void>(`${BASE}/${leaseId}/payments/confirm`, { sessionId }, { headers });
  }
}

import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import type { TenantPayload, TenantRecord } from '../models/lms.models';

const BASE = '/api/lease/api/v1/tenants';

@Injectable({ providedIn: 'root' })
export class TenantApiService {
  private readonly http = inject(HttpClient);

  list(filters: {
    q?: string;
    externalPartyId?: string;
    displayName?: string;
    email?: string;
    phone?: string;
  }): Observable<TenantRecord[]> {
    let hp = new HttpParams();
    if (filters.q?.trim()) {
      hp = hp.set('q', filters.q.trim());
    }
    if (filters.externalPartyId?.trim()) {
      hp = hp.set('externalPartyId', filters.externalPartyId.trim());
    }
    if (filters.displayName?.trim()) {
      hp = hp.set('displayName', filters.displayName.trim());
    }
    if (filters.email?.trim()) {
      hp = hp.set('email', filters.email.trim());
    }
    if (filters.phone?.trim()) {
      hp = hp.set('phone', filters.phone.trim());
    }
    return this.http.get<TenantRecord[]>(BASE, { params: hp });
  }

  get(id: number): Observable<TenantRecord> {
    return this.http.get<TenantRecord>(`${BASE}/${id}`);
  }

  create(body: TenantPayload): Observable<TenantRecord> {
    return this.http.post<TenantRecord>(BASE, body);
  }

  update(id: number, body: TenantPayload): Observable<TenantRecord> {
    return this.http.put<TenantRecord>(`${BASE}/${id}`, body);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${BASE}/${id}`);
  }
}

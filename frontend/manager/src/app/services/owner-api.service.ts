import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import type { OwnerPayload, OwnerRecord } from '../models/lms.models';

const BASE = '/api/property/api/v1/owners';

@Injectable({ providedIn: 'root' })
export class OwnerApiService {
  private readonly http = inject(HttpClient);

  list(filters: {
    q?: string;
    partyId?: string;
    displayName?: string;
    email?: string;
    phone?: string;
  }): Observable<OwnerRecord[]> {
    let hp = new HttpParams();
    if (filters.q?.trim()) {
      hp = hp.set('q', filters.q.trim());
    }
    if (filters.partyId?.trim()) {
      hp = hp.set('partyId', filters.partyId.trim());
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
    return this.http.get<OwnerRecord[]>(BASE, { params: hp });
  }

  get(id: number): Observable<OwnerRecord> {
    return this.http.get<OwnerRecord>(`${BASE}/${id}`);
  }

  create(body: OwnerPayload): Observable<OwnerRecord> {
    return this.http.post<OwnerRecord>(BASE, body);
  }

  update(id: number, body: OwnerPayload): Observable<OwnerRecord> {
    return this.http.put<OwnerRecord>(`${BASE}/${id}`, body);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${BASE}/${id}`);
  }
}

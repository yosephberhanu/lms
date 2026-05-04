import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import type { TenantRecord } from '../models/lms.models';

const BASE = '/api/lease/api/v1/tenants';

@Injectable({ providedIn: 'root' })
export class TenantApiService {
  private readonly http = inject(HttpClient);

  list(params: { externalPartyId?: string }): Observable<TenantRecord[]> {
    let hp = new HttpParams();
    if (params.externalPartyId?.trim()) {
      hp = hp.set('externalPartyId', params.externalPartyId.trim());
    }
    return this.http.get<TenantRecord[]>(BASE, { params: hp });
  }

  get(id: number): Observable<TenantRecord> {
    return this.http.get<TenantRecord>(`${BASE}/${id}`);
  }
}

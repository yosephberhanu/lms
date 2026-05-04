import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import type { TenantRecord } from '../models/lms.models';

const BASE = '/api/lease/api/v1/tenants';

@Injectable({ providedIn: 'root' })
export class TenantApiService {
  private readonly http = inject(HttpClient);

  get(id: number): Observable<TenantRecord> {
    return this.http.get<TenantRecord>(`${BASE}/${id}`);
  }
}

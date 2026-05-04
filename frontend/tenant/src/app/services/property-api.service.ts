import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import type { PropertyRecord } from '../models/lms.models';

const BASE = '/api/property/api/v1/properties';

@Injectable({ providedIn: 'root' })
export class PropertyApiService {
  private readonly http = inject(HttpClient);

  get(id: number): Observable<PropertyRecord> {
    return this.http.get<PropertyRecord>(`${BASE}/${id}`);
  }
}

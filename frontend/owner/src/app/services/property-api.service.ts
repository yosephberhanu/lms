import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import type { PropertyRecord, PropertyType } from '../models/lms.models';

const BASE = '/api/property/api/v1/properties';

@Injectable({ providedIn: 'root' })
export class PropertyApiService {
  private readonly http = inject(HttpClient);

  list(params: {
    ownerPartyId: string;
    q?: string;
    city?: string;
    propertyType?: PropertyType | '';
  }): Observable<PropertyRecord[]> {
    let hp = new HttpParams().set('ownerPartyId', params.ownerPartyId.trim());
    if (params.q?.trim()) {
      hp = hp.set('q', params.q.trim());
    }
    if (params.city?.trim()) {
      hp = hp.set('city', params.city.trim());
    }
    if (params.propertyType) {
      hp = hp.set('propertyType', params.propertyType);
    }
    return this.http.get<PropertyRecord[]>(BASE, { params: hp });
  }

  get(id: number): Observable<PropertyRecord> {
    return this.http.get<PropertyRecord>(`${BASE}/${id}`);
  }
}

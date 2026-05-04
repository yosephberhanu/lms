import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import type { PropertyPayload, PropertyRecord, PropertyType } from '../models/lms.models';

const BASE = '/api/property/api/v1/properties';

@Injectable({ providedIn: 'root' })
export class PropertyApiService {
  private readonly http = inject(HttpClient);

  list(params: {
    q?: string;
    city?: string;
    country?: string;
    propertyType?: PropertyType | '';
    ownerPartyId?: string;
  }): Observable<PropertyRecord[]> {
    let hp = new HttpParams();
    if (params.q?.trim()) {
      hp = hp.set('q', params.q.trim());
    }
    if (params.city?.trim()) {
      hp = hp.set('city', params.city.trim());
    }
    if (params.country?.trim()) {
      hp = hp.set('country', params.country.trim());
    }
    if (params.propertyType) {
      hp = hp.set('propertyType', params.propertyType);
    }
    if (params.ownerPartyId?.trim()) {
      hp = hp.set('ownerPartyId', params.ownerPartyId.trim());
    }
    return this.http.get<PropertyRecord[]>(BASE, { params: hp });
  }

  get(id: number): Observable<PropertyRecord> {
    return this.http.get<PropertyRecord>(`${BASE}/${id}`);
  }

  create(body: PropertyPayload): Observable<PropertyRecord> {
    return this.http.post<PropertyRecord>(BASE, body);
  }

  update(id: number, body: PropertyPayload): Observable<PropertyRecord> {
    return this.http.put<PropertyRecord>(`${BASE}/${id}`, body);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${BASE}/${id}`);
  }
}

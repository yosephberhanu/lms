import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import type { KeycloakRealmRole } from '../models/keycloak-role.model';
import type { KeycloakUser, KeycloakUserCreate, KeycloakUserUpdate } from '../models/keycloak-user.model';

const BASE = '/api/admin/v1/users';
const ROLES_BASE = '/api/admin/v1/roles';

@Injectable({ providedIn: 'root' })
export class KeycloakAdminApiService {
  private readonly http = inject(HttpClient);

  list(filters: { search?: string; first?: number; max?: number; includeRoles?: boolean }): Observable<KeycloakUser[]> {
    let hp = new HttpParams();
    if (filters.search?.trim()) {
      hp = hp.set('search', filters.search.trim());
    }
    if (filters.first != null) {
      hp = hp.set('first', String(filters.first));
    }
    if (filters.max != null) {
      hp = hp.set('max', String(filters.max));
    }
    if (filters.includeRoles != null) {
      hp = hp.set('includeRoles', String(filters.includeRoles));
    }
    return this.http.get<KeycloakUser[]>(BASE, { params: hp });
  }

  get(id: string): Observable<KeycloakUser> {
    return this.http.get<KeycloakUser>(`${BASE}/${encodeURIComponent(id)}`);
  }

  create(body: KeycloakUserCreate): Observable<KeycloakUser> {
    return this.http.post<KeycloakUser>(BASE, body);
  }

  update(id: string, body: KeycloakUserUpdate): Observable<KeycloakUser> {
    return this.http.put<KeycloakUser>(`${BASE}/${encodeURIComponent(id)}`, body);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${BASE}/${encodeURIComponent(id)}`);
  }

  listRealmRoles(filters?: { prefix?: string }): Observable<KeycloakRealmRole[]> {
    let hp = new HttpParams();
    if (filters?.prefix?.trim()) {
      hp = hp.set('prefix', filters.prefix.trim());
    }
    return this.http.get<KeycloakRealmRole[]>(ROLES_BASE, { params: hp });
  }
}

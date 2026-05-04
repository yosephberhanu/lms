import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export interface MyProfile {
  id: string;
  username: string;
  email: string | null;
  phone: string | null;
  firstName: string | null;
  lastName: string | null;
}

export interface MyProfileUpdate {
  email?: string | null;
  phone?: string | null;
  firstName?: string | null;
  lastName?: string | null;
}

const BASE = '/api/profile/v1/me';

@Injectable({ providedIn: 'root' })
export class MyProfileApiService {
  private readonly http = inject(HttpClient);

  me(): Observable<MyProfile> {
    return this.http.get<MyProfile>(BASE);
  }

  update(body: MyProfileUpdate): Observable<MyProfile> {
    return this.http.put<MyProfile>(BASE, body);
  }
}


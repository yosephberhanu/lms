import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export interface CommunicationPreferences {
  notifyEmail: boolean;
  notifySms: boolean;
  email: string | null;
  phone: string | null;
}

const BASE = '/api/notification/v1/me/communication-preferences';

@Injectable({ providedIn: 'root' })
export class CommunicationPreferencesApiService {
  private readonly http = inject(HttpClient);

  get(): Observable<CommunicationPreferences> {
    return this.http.get<CommunicationPreferences>(BASE);
  }

  put(body: CommunicationPreferences): Observable<CommunicationPreferences> {
    return this.http.put<CommunicationPreferences>(BASE, body);
  }
}

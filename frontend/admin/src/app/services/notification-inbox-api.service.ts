import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import type { InAppNotification, SpringPage } from '../models/in-app-notification.model';

const BASE = '/api/notification/v1';

@Injectable({ providedIn: 'root' })
export class NotificationInboxApiService {
  private readonly http = inject(HttpClient);

  list(page = 0, size = 50): Observable<SpringPage<InAppNotification>> {
    const params = new HttpParams().set('page', String(page)).set('size', String(size));
    return this.http.get<SpringPage<InAppNotification>>(`${BASE}/messages`, { params });
  }

  markRead(id: string): Observable<InAppNotification> {
    return this.http.patch<InAppNotification>(`${BASE}/messages/${encodeURIComponent(id)}/read`, {});
  }
}

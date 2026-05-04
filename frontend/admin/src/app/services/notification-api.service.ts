import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import type {
  NotificationBroadcastRequest,
  NotificationBroadcastResponse
} from '../models/notification-broadcast.model';

const BASE = '/api/notification/v1/admin';

@Injectable({ providedIn: 'root' })
export class NotificationApiService {
  private readonly http = inject(HttpClient);

  broadcast(body: NotificationBroadcastRequest): Observable<NotificationBroadcastResponse> {
    return this.http.post<NotificationBroadcastResponse>(`${BASE}/broadcast`, body);
  }
}

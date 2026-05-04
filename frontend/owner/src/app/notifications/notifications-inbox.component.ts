import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { httpErrorMessage } from '../core/http-error-message';
import type { InAppNotification } from '../models/in-app-notification.model';
import { NotificationInboxApiService } from '../services/notification-inbox-api.service';
import { NotificationStreamService } from '../services/notification-stream.service';

@Component({
  selector: 'app-notifications-inbox',
  imports: [CommonModule],
  templateUrl: './notifications-inbox.component.html',
  styleUrl: './notifications-inbox.component.css'
})
export class NotificationsInboxComponent implements OnInit, OnDestroy {
  private readonly api = inject(NotificationInboxApiService);
  private readonly stream = inject(NotificationStreamService);

  rows: InAppNotification[] = [];
  loading = true;
  errorMessage = '';
  markingId: string | null = null;
  private unsubStream: (() => void) | undefined;

  ngOnInit(): void {
    this.load(false);
    this.unsubStream = this.stream.subscribe(() => this.load(true));
  }

  ngOnDestroy(): void {
    this.unsubStream?.();
  }

  load(silent: boolean): void {
    if (!silent) {
      this.loading = true;
    }
    this.errorMessage = '';
    this.api.list(0, 50).subscribe({
      next: (page) => {
        this.rows = page.content ?? [];
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = httpErrorMessage(err, 'Unable to load notifications');
        this.loading = false;
      }
    });
  }

  markRead(n: InAppNotification): void {
    if (n.readAt) {
      return;
    }
    this.markingId = n.id;
    this.api.markRead(n.id).subscribe({
      next: (updated) => {
        this.markingId = null;
        this.rows = this.rows.map((r) => (r.id === updated.id ? updated : r));
      },
      error: (err) => {
        this.markingId = null;
        this.errorMessage = httpErrorMessage(err, 'Unable to mark as read');
      }
    });
  }
}


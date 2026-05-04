import { CommonModule } from '@angular/common';
import { Component, DestroyRef, ElementRef, HostBinding, HostListener, OnInit, computed, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import type { InAppNotification } from '../models/in-app-notification.model';
import { NotificationInboxApiService } from '../services/notification-inbox-api.service';
import { NotificationStreamService } from '../services/notification-stream.service';

@Component({
  selector: 'app-notification-bell',
  imports: [CommonModule, RouterLink],
  templateUrl: './notification-bell.component.html',
  styleUrl: './notification-bell.component.css',
  host: {
    class: 'nav-item dropdown'
  }
})
export class NotificationBellComponent implements OnInit {
  private readonly api = inject(NotificationInboxApiService);
  private readonly stream = inject(NotificationStreamService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly el = inject(ElementRef<HTMLElement>);

  readonly allMessagesPath = input<string>('/notifications');

  readonly dropdownOpen = signal(false);
  readonly recent = signal<InAppNotification[]>([]);
  readonly unreadCount = signal(0);
  readonly loading = signal(false);

  readonly badgeText = computed(() => {
    const n = this.unreadCount();
    if (n <= 0) {
      return '';
    }
    if (n > 99) {
      return '99+';
    }
    return String(n);
  });

  private unsubStream: (() => void) | undefined;
  private pollId: ReturnType<typeof setInterval> | undefined;

  @HostBinding('class.show')
  get hostDropdownShow(): boolean {
    return this.dropdownOpen();
  }

  ngOnInit(): void {
    this.refresh(false);
    this.unsubStream = this.stream.subscribe(() => this.refresh(true));
    this.pollId = window.setInterval(() => this.refresh(true), 60_000);
    this.destroyRef.onDestroy(() => {
      this.unsubStream?.();
      if (this.pollId !== undefined) {
        window.clearInterval(this.pollId);
      }
    });
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(ev: MouseEvent): void {
    if (!this.dropdownOpen()) {
      return;
    }
    const t = ev.target;
    if (t instanceof Node && this.el.nativeElement.contains(t)) {
      return;
    }
    this.dropdownOpen.set(false);
  }

  toggleDropdown(ev: Event): void {
    ev.preventDefault();
    ev.stopPropagation();
    const next = !this.dropdownOpen();
    this.dropdownOpen.set(next);
    if (next) {
      this.refresh(false);
    }
  }

  refresh(silent: boolean): void {
    if (!silent) {
      this.loading.set(true);
    }
    this.api.list(0, 100).subscribe({
      next: (page) => {
        const rows = page.content ?? [];
        this.recent.set(rows.slice(0, 8));
        this.unreadCount.set(rows.filter((r) => !r.readAt).length);
        this.loading.set(false);
      },
      error: () => {
        this.recent.set([]);
        this.unreadCount.set(0);
        this.loading.set(false);
      }
    });
  }

  closeDropdown(): void {
    this.dropdownOpen.set(false);
  }
}

import { Injectable, NgZone, inject } from '@angular/core';
import { OAuthService } from 'angular-oauth2-oidc';

/**
 * Single SSE connection; multiple subscribers (e.g. header bell + messages page) share one stream.
 */
@Injectable({ providedIn: 'root' })
export class NotificationStreamService {
  private readonly oauth = inject(OAuthService);
  private readonly zone = inject(NgZone);

  private readonly listeners = new Set<() => void>();
  private abort: AbortController | null = null;
  private connectLoopPromise: Promise<void> | null = null;

  /** Register for push events; returns teardown that removes this listener and stops the stream when none remain. */
  subscribe(onMessage: () => void): () => void {
    this.listeners.add(onMessage);
    this.ensureStream();
    return () => {
      this.listeners.delete(onMessage);
      if (this.listeners.size === 0) {
        this.abort?.abort();
        this.abort = null;
        this.connectLoopPromise = null;
      }
    };
  }

  /** @deprecated Prefer {@link #subscribe}; kept for existing call sites. */
  start(onMessage: () => void): AbortController {
    const off = this.subscribe(onMessage);
    const ac = new AbortController();
    ac.signal.addEventListener('abort', () => off(), { once: true });
    return ac;
  }

  private ensureStream(): void {
    if (this.abort != null) {
      return;
    }
    this.abort = new AbortController();
    if (!this.connectLoopPromise) {
      this.connectLoopPromise = this.runConnectLoop(this.abort.signal);
    }
  }

  private notifyAll(): void {
    for (const fn of this.listeners) {
      this.zone.run(() => fn());
    }
  }

  private async runConnectLoop(signal: AbortSignal): Promise<void> {
    let backoffMs = 500;
    while (!signal.aborted && this.listeners.size > 0) {
      const token = this.oauth.getAccessToken();
      if (!token) {
        await this.sleep(backoffMs, signal);
        backoffMs = Math.min(backoffMs * 2, 10_000);
        continue;
      }
      backoffMs = 500;
      try {
        await this.runStreamOnce(token, signal);
      } catch {
        // ignore: network errors / aborted
      }
      if (!signal.aborted && this.listeners.size > 0) {
        await this.sleep(backoffMs, signal);
        backoffMs = Math.min(backoffMs * 2, 10_000);
      }
    }
  }

  private async runStreamOnce(token: string, signal: AbortSignal): Promise<void> {
    const res = await fetch('/api/notification/v1/stream', {
      method: 'GET',
      headers: {
        Accept: 'text/event-stream',
        Authorization: `Bearer ${token}`
      },
      signal
    });
    if (!res.ok || !res.body) {
      throw new Error(`SSE stream failed with status ${res.status}`);
    }
    const reader = res.body.getReader();
    const decoder = new TextDecoder();
    let buffer = '';
    while (!signal.aborted) {
      const { done, value } = await reader.read();
      if (done) {
        break;
      }
      buffer += decoder.decode(value, { stream: true });
      const parts = buffer.split(/\r?\n\r?\n/);
      buffer = parts.pop() ?? '';
      for (const block of parts) {
        if (this.blockHasMessageData(block)) {
          this.notifyAll();
        }
      }
    }
  }

  private sleep(ms: number, signal: AbortSignal): Promise<void> {
    if (signal.aborted) {
      return Promise.resolve();
    }
    return new Promise((resolve) => {
      const id = window.setTimeout(resolve, ms);
      signal.addEventListener(
        'abort',
        () => {
          window.clearTimeout(id);
          resolve();
        },
        { once: true }
      );
    });
  }

  private blockHasMessageData(block: string): boolean {
    let eventName: string | null = null;
    let hasData = false;
    for (const rawLine of block.split(/\r?\n/)) {
      const line = rawLine.trim();
      if (!line || line.startsWith(':')) {
        continue;
      }
      if (line.startsWith('event:')) {
        eventName = line.slice(6).trim();
      } else if (line.startsWith('data:')) {
        hasData = true;
      }
    }
    return hasData && (eventName === 'message' || eventName === null);
  }
}

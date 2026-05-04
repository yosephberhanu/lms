import { Injectable, inject, signal } from '@angular/core';
import { OAuthService } from 'angular-oauth2-oidc';
import { catchError, map, of, tap } from 'rxjs';
import type { Observable } from 'rxjs';
import { TenantApiService } from './tenant-api.service';

const MANUAL_STORAGE_KEY = 'lms.tenant.id.manual';
const LEGACY_STORAGE_KEY = 'lms.tenant.id';
const RESOLVED_STORAGE_KEY = 'lms.tenant.id.resolved';

function claimString(value: unknown): string {
  if (typeof value === 'string' && value.trim()) {
    return value.trim();
  }
  if (typeof value === 'number' && Number.isFinite(value) && value > 0) {
    return String(Math.trunc(value));
  }
  if (Array.isArray(value) && value.length > 0) {
    const first = value[0];
    if (typeof first === 'string' && first.trim()) {
      return first.trim();
    }
    if (typeof first === 'number' && Number.isFinite(first)) {
      return String(Math.trunc(first));
    }
  }
  return '';
}

@Injectable({ providedIn: 'root' })
export class TenantSessionService {
  private readonly oauth = inject(OAuthService);
  private readonly tenantApi = inject(TenantApiService);
  private readonly manualTenantId = signal<number>(this.readManualFromStorage());
  private readonly resolvedTenantId = signal<number>(this.readResolvedFromStorage());

  /** Numeric lease <code>tenants.id</code> from Keycloak claim <code>lms_tenant_id</code>, else optional manual override. */
  currentTenantId(): number {
    const fromToken = this.tenantIdFromClaims();
    if (Number.isFinite(fromToken) && fromToken > 0) {
      return fromToken;
    }
    const resolved = this.resolvedTenantId();
    if (Number.isFinite(resolved) && resolved > 0) {
      return resolved;
    }
    const n = this.manualTenantId();
    return Number.isFinite(n) && n > 0 ? n : NaN;
  }

  hasTenantId(): boolean {
    const n = this.currentTenantId();
    return Number.isFinite(n) && n > 0;
  }

  /**
   * Best-effort: resolve tenant numeric id by looking up the current Keycloak user’s external party id
   * against Lease service `/tenants?externalPartyId=...`.
   */
  resolveTenantIdFromIdentity$(): Observable<number | null> {
    if (this.hasTenantId()) {
      return of(this.currentTenantId());
    }
    const external = this.externalPartyIdFromClaims();
    if (!external) {
      return of(null);
    }
    return this.tenantApi.list({ externalPartyId: external }).pipe(
      map((rows) => (rows && rows.length ? rows[0] : null)),
      tap((row) => {
        if (row?.id && Number.isFinite(row.id) && row.id > 0) {
          this.persistResolved(row.id);
        }
      }),
      map((row) => (row?.id && Number.isFinite(row.id) ? row.id : null)),
      catchError(() => of(null))
    );
  }

  /** Manual override when the token has no <code>lms_tenant_id</code> claim. */
  setTenantIdFromString(raw: string): void {
    const trimmed = raw.trim();
    if (!trimmed) {
      this.clear();
      return;
    }
    const n = Number.parseInt(trimmed, 10);
    if (!Number.isFinite(n) || n < 1) {
      this.clear();
      return;
    }
    if (typeof localStorage !== 'undefined') {
      localStorage.setItem(MANUAL_STORAGE_KEY, String(n));
    }
    this.manualTenantId.set(n);
  }

  /** Clears manual override only. */
  clear(): void {
    if (typeof localStorage !== 'undefined') {
      localStorage.removeItem(MANUAL_STORAGE_KEY);
      localStorage.removeItem(LEGACY_STORAGE_KEY);
      localStorage.removeItem(RESOLVED_STORAGE_KEY);
    }
    this.manualTenantId.set(NaN);
    this.resolvedTenantId.set(NaN);
  }

  private tenantIdFromClaims(): number {
    if (!this.oauth.hasValidAccessToken()) {
      return NaN;
    }
    const claims = this.oauth.getIdentityClaims() as Record<string, unknown> | null;
    if (!claims) {
      return NaN;
    }
    const s = claimString(claims['lms_tenant_id']);
    if (!s) {
      return NaN;
    }
    const n = Number.parseInt(s, 10);
    return Number.isFinite(n) && n > 0 ? n : NaN;
  }

  private externalPartyIdFromClaims(): string {
    if (!this.oauth.hasValidAccessToken()) {
      return '';
    }
    const claims = this.oauth.getIdentityClaims() as Record<string, unknown> | null;
    if (!claims) {
      return '';
    }
    // Preferred: explicit mapper claim (if you have one configured)
    const explicit = claimString(claims['lms_external_party_id']);
    if (explicit) {
      return explicit;
    }
    // Fallback: username/sub. Seed data uses strings like `tenant-user-5001`.
    const username = claimString(claims['preferred_username']);
    if (username) {
      return username;
    }
    return claimString(claims['sub']);
  }

  private readManualFromStorage(): number {
    if (typeof localStorage === 'undefined') {
      return NaN;
    }
    const manual = localStorage.getItem(MANUAL_STORAGE_KEY);
    if (manual?.trim()) {
      const n = Number.parseInt(manual.trim(), 10);
      if (Number.isFinite(n) && n > 0) {
        return n;
      }
    }
    const legacy = localStorage.getItem(LEGACY_STORAGE_KEY);
    if (legacy?.trim()) {
      const n = Number.parseInt(legacy.trim(), 10);
      if (Number.isFinite(n) && n > 0) {
        localStorage.setItem(MANUAL_STORAGE_KEY, String(n));
        localStorage.removeItem(LEGACY_STORAGE_KEY);
        return n;
      }
    }
    return NaN;
  }

  private readResolvedFromStorage(): number {
    if (typeof localStorage === 'undefined') {
      return NaN;
    }
    const raw = localStorage.getItem(RESOLVED_STORAGE_KEY);
    if (raw?.trim()) {
      const n = Number.parseInt(raw.trim(), 10);
      if (Number.isFinite(n) && n > 0) {
        return n;
      }
    }
    return NaN;
  }

  private persistResolved(id: number): void {
    if (typeof localStorage !== 'undefined') {
      localStorage.setItem(RESOLVED_STORAGE_KEY, String(id));
    }
    this.resolvedTenantId.set(id);
  }
}

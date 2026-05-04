import { Injectable, inject, signal } from '@angular/core';
import { OAuthService } from 'angular-oauth2-oidc';

/** Optional dev override when the Keycloak user has no `lms_party_id` attribute. */
const MANUAL_STORAGE_KEY = 'lms.owner.partyId.manual';
const LEGACY_STORAGE_KEY = 'lms.owner.partyId';

function claimString(value: unknown): string {
  if (typeof value === 'string' && value.trim()) {
    return value.trim();
  }
  if (Array.isArray(value) && value.length > 0) {
    const first = value[0];
    if (typeof first === 'string' && first.trim()) {
      return first.trim();
    }
  }
  return '';
}

@Injectable({ providedIn: 'root' })
export class OwnerSessionService {
  private readonly oauth = inject(OAuthService);
  private readonly manualPartyId = signal<string>(this.readManualFromStorage());

  /** Party id from Keycloak claim `lms_party_id`, else optional manual override (localStorage). */
  currentPartyId(): string {
    const fromToken = this.partyIdFromClaims();
    if (fromToken) {
      return fromToken;
    }
    return this.manualPartyId().trim();
  }

  hasPartyId(): boolean {
    return this.currentPartyId().length > 0;
  }

  /** Stores a manual party id only used when the access token has no `lms_party_id` claim. */
  setPartyId(raw: string): void {
    const v = raw.trim();
    if (v) {
      localStorage.setItem(MANUAL_STORAGE_KEY, v);
    } else {
      localStorage.removeItem(MANUAL_STORAGE_KEY);
    }
    this.manualPartyId.set(v);
  }

  /** Clears manual override only (Keycloak claim still applies after re-login). */
  clear(): void {
    localStorage.removeItem(MANUAL_STORAGE_KEY);
    localStorage.removeItem(LEGACY_STORAGE_KEY);
    this.manualPartyId.set('');
  }

  private partyIdFromClaims(): string {
    if (!this.oauth.hasValidAccessToken()) {
      return '';
    }
    const claims = this.oauth.getIdentityClaims() as Record<string, unknown> | null;
    if (!claims) {
      return '';
    }
    // Primary mapping: explicit realm/client mapper claim.
    const explicit = claimString(claims['lms_party_id']);
    if (explicit) {
      return explicit;
    }
    // Fallbacks: many Keycloak setups rely on username/sub to identify parties.
    const username = claimString(claims['preferred_username']);
    if (username) {
      return username;
    }
    return claimString(claims['sub']);
  }

  private readManualFromStorage(): string {
    if (typeof localStorage === 'undefined') {
      return '';
    }
    const manual = localStorage.getItem(MANUAL_STORAGE_KEY);
    if (manual?.trim()) {
      return manual.trim();
    }
    const legacy = localStorage.getItem(LEGACY_STORAGE_KEY);
    if (legacy?.trim()) {
      const v = legacy.trim();
      localStorage.setItem(MANUAL_STORAGE_KEY, v);
      localStorage.removeItem(LEGACY_STORAGE_KEY);
      return v;
    }
    return '';
  }
}

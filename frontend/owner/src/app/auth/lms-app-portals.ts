import { OAuthService } from 'angular-oauth2-oidc';

export type LmsAppPortalId = 'manager' | 'admin' | 'owner' | 'tenant';

export interface LmsPortalOption {
  readonly id: LmsAppPortalId;
  readonly label: string;
  /** Path on the gateway origin, e.g. `/manager/`. */
  readonly basePath: string;
}

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

function realmRoles(claims: Record<string, unknown> | null): Set<string> {
  if (!claims) {
    return new Set();
  }
  const ra = claims['realm_access'];
  if (!ra || typeof ra !== 'object') {
    return new Set();
  }
  const roles = (ra as Record<string, unknown>)['roles'];
  if (!Array.isArray(roles)) {
    return new Set();
  }
  return new Set(roles.filter((r): r is string => typeof r === 'string'));
}

function jwtPayloadRecord(token: string | null): Record<string, unknown> | null {
  if (!token) {
    return null;
  }
  const parts = token.split('.');
  if (parts.length < 2) {
    return null;
  }
  try {
    let b64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
    const pad = b64.length % 4;
    if (pad) {
      b64 += '='.repeat(4 - pad);
    }
    const json = atob(b64);
    return JSON.parse(json) as Record<string, unknown>;
  } catch {
    return null;
  }
}

function effectiveRealmRoles(oauth: OAuthService): Set<string> {
  const idClaims = oauth.getIdentityClaims() as Record<string, unknown> | null;
  const accessClaims = jwtPayloadRecord(oauth.getAccessToken());
  return new Set([...realmRoles(idClaims), ...realmRoles(accessClaims)]);
}

function effectiveClaimString(oauth: OAuthService, key: string): string {
  const idClaims = oauth.getIdentityClaims() as Record<string, unknown> | null;
  const fromId = claimString(idClaims?.[key]);
  if (fromId) {
    return fromId;
  }
  return claimString(jwtPayloadRecord(oauth.getAccessToken())?.[key]);
}

const PORTAL_RULES: readonly {
  id: LmsAppPortalId;
  label: string;
  basePath: string;
  eligible: (roles: Set<string>, party: string, tenant: string) => boolean;
}[] = [
  {
    id: 'manager',
    label: 'Manager',
    basePath: '/manager/',
    eligible: (roles) => roles.has('lms-staff')
  },
  {
    id: 'admin',
    label: 'Admin',
    basePath: '/admin/',
    eligible: (roles) => roles.has('lms-admin')
  },
  {
    id: 'owner',
    label: 'Owner',
    basePath: '/owner/',
    eligible: (_, party) => party.length > 0
  },
  {
    id: 'tenant',
    label: 'Tenant',
    basePath: '/tenant/',
    eligible: (_, __, tenant) => tenant.length > 0
  }
];

/** Portals this session may open, in redirect priority order (first wins after login). */
export function eligibleLmsPortals(oauth: OAuthService): LmsPortalOption[] {
  if (!oauth.hasValidAccessToken()) {
    return [];
  }
  const roles = effectiveRealmRoles(oauth);
  const party = effectiveClaimString(oauth, 'lms_party_id');
  const tenant = effectiveClaimString(oauth, 'lms_tenant_id');
  const out: LmsPortalOption[] = [];
  for (const rule of PORTAL_RULES) {
    if (rule.eligible(roles, party, tenant)) {
      out.push({ id: rule.id, label: rule.label, basePath: rule.basePath });
    }
  }
  return out;
}

export function firstLmsPortalPathByPriority(oauth: OAuthService): string {
  const list = eligibleLmsPortals(oauth);
  if (list.length > 0) {
    return list[0].basePath;
  }
  return '/manager/';
}

export function currentLmsPortalId(): LmsAppPortalId | null {
  if (typeof window === 'undefined') {
    return null;
  }
  const m = window.location.pathname.match(/^\/(manager|owner|tenant|admin)(\/|$)/);
  return m ? (m[1] as LmsAppPortalId) : null;
}

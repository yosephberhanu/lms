import { inject } from '@angular/core';
import { CanMatchFn, Router } from '@angular/router';
import { OAuthService } from 'angular-oauth2-oidc';

function realmRolesFromAccessToken(token: string | null): string[] {
  if (!token) {
    return [];
  }
  try {
    const parts = token.split('.');
    if (parts.length < 2) {
      return [];
    }
    let b64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
    const pad = b64.length % 4;
    if (pad) {
      b64 += '='.repeat(4 - pad);
    }
    const payload = JSON.parse(atob(b64)) as { realm_access?: { roles?: string[] } };
    const roles = payload.realm_access?.roles;
    return Array.isArray(roles) ? roles : [];
  } catch {
    return [];
  }
}

/** Requires realm role `lms-admin` (Keycloak Admin REST proxy). */
export const lmsAdminGuard: CanMatchFn = () => {
  const oauth = inject(OAuthService);
  const router = inject(Router);
  const roles = realmRolesFromAccessToken(oauth.getAccessToken());
  if (!roles.includes('lms-admin')) {
    return router.createUrlTree(['/forbidden']);
  }
  return true;
};

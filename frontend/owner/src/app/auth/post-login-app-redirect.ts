import { OAuthService } from 'angular-oauth2-oidc';
import { currentLmsPortalId, firstLmsPortalPathByPriority } from './lms-app-portals';

/**
 * First URL path segment when the SPA is mounted under the edge gateway
 * (`/manager/`, `/owner/`, …). Returns null for plain `ng serve` at `/`.
 */
export function currentMountedAppSegment(): string | null {
  return currentLmsPortalId();
}

/** Target home path including slashes, e.g. `/manager/`. First eligible portal in fixed priority order. */
export function resolveTargetAppPath(oauth: OAuthService): string | null {
  if (!oauth.hasValidAccessToken()) {
    return null;
  }
  return firstLmsPortalPathByPriority(oauth);
}

/**
 * After OIDC login, send the browser to the portal that matches realm roles / LMS claims.
 * No-op when not behind a path-mounted gateway (local dev on a bare `/`).
 */
export function redirectToAppropriateAppIfNeeded(oauth: OAuthService): void {
  const mount = currentMountedAppSegment();
  if (mount === null) {
    return;
  }
  const target = resolveTargetAppPath(oauth);
  if (!target) {
    return;
  }
  const want = target.match(/^\/(manager|owner|tenant|admin)\//)?.[1];
  if (!want || mount === want) {
    return;
  }
  window.location.assign(`${window.location.origin}${target}`);
}

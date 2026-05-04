import { AuthConfig, type OAuthService } from 'angular-oauth2-oidc';
import { environment } from '../../environments/environment';

/**
 * Keycloak can advertise :8180 in discovery while the SPA runs on :80 (or ng serve + proxy).
 * With strictDiscoveryDocumentValidation: false, angular-oauth2-oidc still POSTs to those URLs → CORS on /token.
 * Align endpoints with the page origin so calls go through nginx or the dev proxy.
 */
export function rewriteOidcUrlsToPageOrigin(oauth: OAuthService): void {
  if (typeof document === 'undefined') return;
  const page = new URL(document.location.origin);
  const rewrite = (url: string | undefined | null): string | undefined => {
    if (!url) return undefined;
    let parsed: URL;
    try {
      parsed = new URL(url);
    } catch {
      return url;
    }
    if (parsed.origin === page.origin) return url;
    parsed.protocol = page.protocol;
    parsed.host = page.host;
    return parsed.toString();
  };

  if (oauth.issuer) {
    const next = rewrite(oauth.issuer);
    if (next) oauth.issuer = next;
  }
  if (oauth.tokenEndpoint) {
    const next = rewrite(oauth.tokenEndpoint);
    if (next) oauth.tokenEndpoint = next;
  }
  if (oauth.loginUrl) {
    const next = rewrite(oauth.loginUrl);
    if (next) oauth.loginUrl = next;
  }
  if (oauth.logoutUrl) {
    const next = rewrite(oauth.logoutUrl);
    if (next) oauth.logoutUrl = next;
  }
  if (oauth.userinfoEndpoint) {
    const next = rewrite(oauth.userinfoEndpoint);
    if (next) oauth.userinfoEndpoint = next;
  }
  if (oauth.revocationEndpoint) {
    const next = rewrite(oauth.revocationEndpoint);
    if (next) oauth.revocationEndpoint = next;
  }
  const internal = oauth as unknown as { jwksUri?: string; sessionCheckIFrameUrl?: string };
  if (internal.jwksUri) {
    const next = rewrite(internal.jwksUri);
    if (next) internal.jwksUri = next;
  }
  if (internal.sessionCheckIFrameUrl) {
    const next = rewrite(internal.sessionCheckIFrameUrl);
    if (next) internal.sessionCheckIFrameUrl = next;
  }
}

export function buildAuthConfig(): AuthConfig {
  const origin =
    typeof document !== 'undefined' && document.location?.origin?.length
      ? document.location.origin
      : environment.keycloakUrl;
  const redirectUri =
    typeof document !== 'undefined' ? document.baseURI : `${origin}/`;
  return {
    issuer: `${origin}/realms/${environment.realm}`,
    clientId: environment.clientId,
    responseType: 'code',
    redirectUri,
    postLogoutRedirectUri: redirectUri,
    // offline_access requires the Keycloak realm role "offline_access"; our realm import only defines LMS roles.
    // "roles" asks Keycloak to attach realm_access (and related) role claims used by Spring Resource Server.
    scope: 'openid profile email roles',
    showDebugInformation: !environment.production,
    sessionChecksEnabled: true,
    strictDiscoveryDocumentValidation: false,
    disablePKCE: false
  };
}

import { inject } from '@angular/core';
import { CanMatchFn } from '@angular/router';
import { OAuthService } from 'angular-oauth2-oidc';

/** Starts OIDC code flow when there is no valid access token. */
export const keycloakAuthGuard: CanMatchFn = () => {
  const oauth = inject(OAuthService);
  if (oauth.hasValidAccessToken()) {
    return true;
  }
  oauth.initCodeFlow();
  return false;
};

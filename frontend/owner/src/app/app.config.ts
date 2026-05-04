import { ApplicationConfig, provideAppInitializer, provideZoneChangeDetection, inject } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { AuthConfig, OAuthService, provideOAuthClient } from 'angular-oauth2-oidc';

import { routes } from './app.routes';
import { buildAuthConfig, rewriteOidcUrlsToPageOrigin } from './auth/auth.config';
import { redirectToAppropriateAppIfNeeded } from './auth/post-login-app-redirect';
import { authBearerInterceptor } from './auth/auth.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideOAuthClient(),
    { provide: AuthConfig, useFactory: buildAuthConfig },
    provideAppInitializer(async () => {
      const oauth = inject(OAuthService);
      await oauth.loadDiscoveryDocument();
      rewriteOidcUrlsToPageOrigin(oauth);
      await oauth.tryLogin();
      redirectToAppropriateAppIfNeeded(oauth);
    }),
    provideHttpClient(withInterceptors([authBearerInterceptor])),
    provideRouter(routes)
  ]
};

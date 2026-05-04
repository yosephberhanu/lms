import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { OAuthService } from 'angular-oauth2-oidc';

export const authBearerInterceptor: HttpInterceptorFn = (req, next) => {
  const oauth = inject(OAuthService);
  const isApiUrl = (() => {
    if (req.url.startsWith('/api')) return true;
    try {
      const u = new URL(req.url);
      return u.pathname.startsWith('/api');
    } catch {
      return false;
    }
  })();
  if (isApiUrl && !req.headers.has('Authorization')) {
    // `hasValidAccessToken()` can briefly be false during startup/refresh even though a token is present.
    const token = oauth.getAccessToken();
    if (token?.trim()) {
      req = req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
    }
  }
  return next(req);
};

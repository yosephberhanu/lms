import { inject } from '@angular/core';
import { CanMatchFn } from '@angular/router';
import { map } from 'rxjs';
import { TenantSessionService } from '../services/tenant-session.service';

export const tenantShellCanMatch: CanMatchFn = () => {
  const session = inject(TenantSessionService);
  if (session.hasTenantId()) {
    return true;
  }
  return session.resolveTenantIdFromIdentity$().pipe(map((id) => !!id));
};

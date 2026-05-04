import { inject } from '@angular/core';
import { CanMatchFn } from '@angular/router';
import { OwnerSessionService } from '../services/owner-session.service';

/** Allows the main shell only when an owner party id has been chosen (stored in localStorage). */
export const ownerShellCanMatch: CanMatchFn = () => {
  return inject(OwnerSessionService).hasPartyId();
};

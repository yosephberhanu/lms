import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { OAuthService } from 'angular-oauth2-oidc';
import { currentLmsPortalId, eligibleLmsPortals, type LmsPortalOption } from '../auth/lms-app-portals';
import { environment } from '../../environments/environment';
import { NotificationBellComponent } from './notification-bell.component';

@Component({
  selector: 'app-main-layout',
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive, NotificationBellComponent],
  templateUrl: './main-layout.component.html',
  styleUrl: './main-layout.component.css'
})
export class MainLayoutComponent {
  readonly oauth = inject(OAuthService);
  readonly appSwitcherOpen = signal(false);
  readonly menuOpen = signal(false);

  readonly portalSwitchTargets = computed(() => {
    const all = eligibleLmsPortals(this.oauth);
    const current = currentLmsPortalId();
    if (all.length <= 1) {
      return [];
    }
    return all.filter((p) => p.id !== current);
  });

  displayName(): string {
    const c = this.oauth.getIdentityClaims() as Record<string, unknown> | null;
    if (!c) {
      return '';
    }
    const name = c['name'];
    if (typeof name === 'string' && name.trim()) {
      return name;
    }
    const preferred = c['preferred_username'];
    return typeof preferred === 'string' ? preferred : '';
  }

  logout(): void {
    // Keycloak can respond with "Invalid parameter: id_token_hint" when the stored id_token is missing/stale.
    // Use a client_id based logout URL (works without id_token_hint) and clear local tokens first.
    const postLogoutRedirectUri = typeof document !== 'undefined' ? document.baseURI : window.location.origin;
    const keycloakOrigin = window.location.origin;
    const logoutUrl =
      `${keycloakOrigin}/realms/${environment.realm}/protocol/openid-connect/logout` +
      `?client_id=${encodeURIComponent(environment.clientId)}` +
      `&post_logout_redirect_uri=${encodeURIComponent(postLogoutRedirectUri)}`;
    this.oauth.logOut(true);
    window.location.assign(logoutUrl);
  }

  toggleAppSwitcher(event: Event): void {
    event.preventDefault();
    event.stopPropagation();
    this.appSwitcherOpen.update((v) => !v);
  }

  toggleMenu(event: Event): void {
    event.preventDefault();
    event.stopPropagation();
    this.menuOpen.update((v) => !v);
  }

  closeMenu(): void {
    this.menuOpen.set(false);
    this.appSwitcherOpen.set(false);
  }

  openPortal(portal: LmsPortalOption): void {
    this.closeMenu();
    window.location.assign(`${window.location.origin}${portal.basePath}`);
  }
}

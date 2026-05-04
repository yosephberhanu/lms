import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { OAuthService } from 'angular-oauth2-oidc';
import { currentLmsPortalId, eligibleLmsPortals, type LmsPortalOption } from '../auth/lms-app-portals';
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
    this.oauth.logOut();
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

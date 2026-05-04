import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { OAuthService } from 'angular-oauth2-oidc';
import { currentLmsPortalId, eligibleLmsPortals, type LmsPortalOption } from '../auth/lms-app-portals';
import { TenantSessionService } from '../services/tenant-session.service';
import { NotificationBellComponent } from './notification-bell.component';

@Component({
  selector: 'app-main-layout',
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive, NotificationBellComponent],
  templateUrl: './main-layout.component.html',
  styleUrl: './main-layout.component.css'
})
export class MainLayoutComponent {
  readonly session = inject(TenantSessionService);
  private readonly router = inject(Router);
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

  changeTenant(): void {
    this.router.navigate(['/welcome']);
  }

  signOut(): void {
    this.session.clear();
    this.router.navigate(['/welcome']);
  }

  logOutAccount(): void {
    this.session.clear();
    this.oauth.logOut();
  }

  accountLabel(): string {
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

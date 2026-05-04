import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { OAuthService } from 'angular-oauth2-oidc';
import { TenantSessionService } from '../services/tenant-session.service';

@Component({
  selector: 'app-welcome',
  imports: [CommonModule, RouterLink],
  templateUrl: './welcome.component.html',
  styleUrl: './welcome.component.css'
})
export class WelcomeComponent implements OnInit {
  readonly session = inject(TenantSessionService);
  readonly oauth = inject(OAuthService);
  private readonly router = inject(Router);

  missingTenant = false;

  ngOnInit(): void {
    if (this.session.hasTenantId()) {
      void this.router.navigate(['/dashboard']);
      return;
    }
    // Try to resolve from Keycloak identity -> lease tenants.external_party_id
    this.session.resolveTenantIdFromIdentity$().subscribe({
      next: (id) => {
        if (id) {
          void this.router.navigate(['/dashboard']);
        } else {
          this.missingTenant = true;
        }
      },
      error: () => {
        this.missingTenant = true;
      }
    });
  }

  logOut(): void {
    this.session.clear();
    this.oauth.logOut();
  }
}

import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { OAuthService } from 'angular-oauth2-oidc';
import { OwnerSessionService } from '../services/owner-session.service';

@Component({
  selector: 'app-welcome',
  imports: [CommonModule, RouterLink],
  templateUrl: './welcome.component.html',
  styleUrl: './welcome.component.css'
})
export class WelcomeComponent implements OnInit {
  readonly session = inject(OwnerSessionService);
  readonly oauth = inject(OAuthService);
  private readonly router = inject(Router);

  /** True when no party id can be resolved (no Keycloak attribute and no manual override). */
  missingParty = false;

  ngOnInit(): void {
    if (this.session.hasPartyId()) {
      void this.router.navigate(['/dashboard']);
      return;
    }
    this.missingParty = true;
  }

  logOut(): void {
    this.session.clear();
    this.oauth.logOut();
  }
}

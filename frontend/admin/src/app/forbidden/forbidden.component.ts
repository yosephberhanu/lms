import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { OAuthService } from 'angular-oauth2-oidc';

@Component({
  selector: 'app-forbidden',
  imports: [CommonModule],
  templateUrl: './forbidden.component.html',
  styleUrl: './forbidden.component.css'
})
export class ForbiddenComponent {
  readonly oauth = inject(OAuthService);

  logOut(): void {
    this.oauth.logOut();
  }
}

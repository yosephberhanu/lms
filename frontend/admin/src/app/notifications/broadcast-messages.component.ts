import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { httpErrorMessage } from '../core/http-error-message';
import type { KeycloakRealmRole } from '../models/keycloak-role.model';
import type { KeycloakUser } from '../models/keycloak-user.model';
import { KeycloakAdminApiService } from '../services/auth-service-api.service';
import { NotificationApiService } from '../services/notification-api.service';

@Component({
  selector: 'app-broadcast-messages',
  imports: [CommonModule, FormsModule],
  templateUrl: './broadcast-messages.component.html',
  styleUrl: './broadcast-messages.component.css'
})
export class BroadcastMessagesComponent implements OnInit {
  private readonly usersApi = inject(KeycloakAdminApiService);
  private readonly notificationApi = inject(NotificationApiService);

  title = '';
  body = '';
  users: KeycloakUser[] = [];
  roles: KeycloakRealmRole[] = [];
  userSearch = '';
  selectedUserIds = new Set<string>();
  selectedRoleNames = new Set<string>();

  loadingUsers = true;
  loadingRoles = true;
  sending = false;
  errorMessage = '';
  successMessage = '';

  ngOnInit(): void {
    this.loadUsers();
    this.loadRoles();
  }

  loadUsers(): void {
    this.loadingUsers = true;
    this.errorMessage = '';
    this.usersApi.list({ search: this.userSearch || undefined, first: 0, max: 100 }).subscribe({
      next: (rows) => {
        this.users = rows;
        this.loadingUsers = false;
      },
      error: (err) => {
        this.errorMessage = httpErrorMessage(err, 'Unable to load users');
        this.loadingUsers = false;
      }
    });
  }

  loadRoles(): void {
    this.loadingRoles = true;
    this.usersApi.listRealmRoles({ prefix: 'lms-' }).subscribe({
      next: (r) => {
        this.roles = r;
        this.loadingRoles = false;
      },
      error: (err) => {
        this.errorMessage = httpErrorMessage(err, 'Unable to load realm roles');
        this.loadingRoles = false;
      }
    });
  }

  toggleUser(id: string): void {
    if (this.selectedUserIds.has(id)) {
      this.selectedUserIds.delete(id);
    } else {
      this.selectedUserIds.add(id);
    }
  }

  isUserSelected(id: string): boolean {
    return this.selectedUserIds.has(id);
  }

  toggleRole(name: string): void {
    if (this.selectedRoleNames.has(name)) {
      this.selectedRoleNames.delete(name);
    } else {
      this.selectedRoleNames.add(name);
    }
  }

  isRoleSelected(name: string): boolean {
    return this.selectedRoleNames.has(name);
  }

  send(): void {
    this.errorMessage = '';
    this.successMessage = '';
    const userIds = [...this.selectedUserIds];
    const realmRoleNames = [...this.selectedRoleNames];
    if (userIds.length === 0 && realmRoleNames.length === 0) {
      this.errorMessage = 'Select at least one user and/or one realm role (mass send by role).';
      return;
    }
    if (!this.title.trim() || !this.body.trim()) {
      this.errorMessage = 'Title and message body are required.';
      return;
    }
    this.sending = true;
    this.notificationApi
      .broadcast({
        title: this.title.trim(),
        body: this.body.trim(),
        userIds,
        realmRoleNames
      })
      .subscribe({
        next: (res) => {
          this.sending = false;
          this.successMessage = `Sent to ${res.recipientCount} recipient(s); ${res.messagesCreated} inbox message(s) created.`;
          this.title = '';
          this.body = '';
          this.selectedUserIds.clear();
          this.selectedRoleNames.clear();
        },
        error: (err) => {
          this.sending = false;
          this.errorMessage = httpErrorMessage(err, 'Unable to send notifications');
        }
      });
  }
}

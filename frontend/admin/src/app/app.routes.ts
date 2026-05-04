import { Routes } from '@angular/router';
import { keycloakAuthGuard } from './auth/keycloak-auth.guard';
import { lmsAdminGuard } from './guards/lms-admin.guard';

export const routes: Routes = [
  {
    path: '',
    canMatch: [keycloakAuthGuard],
    children: [
      {
        path: 'forbidden',
        loadComponent: () =>
          import('./forbidden/forbidden.component').then((m) => m.ForbiddenComponent)
      },
      {
        path: '',
        canMatch: [lmsAdminGuard],
        loadComponent: () => import('./layout/main-layout.component').then((m) => m.MainLayoutComponent),
        children: [
          { path: '', redirectTo: 'users', pathMatch: 'full' },
          {
            path: 'users',
            loadComponent: () => import('./users/user-list.component').then((m) => m.UserListComponent)
          },
          {
            path: 'users/new',
            data: { userFormMode: 'create' },
            loadComponent: () => import('./users/user-form.component').then((m) => m.UserFormComponent)
          },
          {
            path: 'users/:id/edit',
            data: { userFormMode: 'edit' },
            loadComponent: () => import('./users/user-form.component').then((m) => m.UserFormComponent)
          },
          {
            path: 'notifications',
            loadComponent: () =>
              import('./notifications/broadcast-messages.component').then((m) => m.BroadcastMessagesComponent)
          },
          {
            path: 'messages',
            loadComponent: () => import('./messages/inbox-messages.component').then((m) => m.InboxMessagesComponent)
          },
          {
            path: 'profile',
            loadComponent: () => import('./profile/my-profile.component').then((m) => m.MyProfileComponent)
          }
        ]
      },
      { path: '**', redirectTo: 'forbidden' }
    ]
  }
];

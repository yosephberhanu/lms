import { Routes } from '@angular/router';
import { keycloakAuthGuard } from './auth/keycloak-auth.guard';
import { tenantShellCanMatch } from './guards/tenant-shell.guard';

export const routes: Routes = [
  {
    path: '',
    canMatch: [keycloakAuthGuard],
    children: [
      {
        path: 'welcome',
        loadComponent: () => import('./welcome/welcome.component').then((m) => m.WelcomeComponent)
      },
      {
        path: '',
        canMatch: [tenantShellCanMatch],
        loadComponent: () => import('./layout/main-layout.component').then((m) => m.MainLayoutComponent),
        children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./dashboard/tenant-dashboard.component').then((m) => m.TenantDashboardComponent)
      },
      {
        path: 'leases/pending',
        loadComponent: () =>
          import('./leases/tenant-lease-list.component').then((m) => m.TenantLeaseListComponent),
        data: { pendingOnly: true }
      },
      {
        path: 'leases/:id',
        loadComponent: () =>
          import('./leases/tenant-lease-detail.component').then((m) => m.TenantLeaseDetailComponent)
      },
      {
        path: 'leases',
        loadComponent: () =>
          import('./leases/tenant-lease-list.component').then((m) => m.TenantLeaseListComponent),
        data: { pendingOnly: false }
      },
          {
            path: 'notifications',
            loadComponent: () =>
              import('./notifications/notifications-inbox.component').then((m) => m.NotificationsInboxComponent)
          },
          {
            path: 'profile',
            loadComponent: () => import('./profile/my-profile.component').then((m) => m.MyProfileComponent)
          }
        ]
      },
      { path: '**', redirectTo: 'welcome' }
    ]
  }
];

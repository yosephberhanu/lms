import { Routes } from '@angular/router';
import { keycloakAuthGuard } from './auth/keycloak-auth.guard';
import { ownerShellCanMatch } from './guards/owner-shell.guard';

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
        canMatch: [ownerShellCanMatch],
        loadComponent: () => import('./layout/main-layout.component').then((m) => m.MainLayoutComponent),
        children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./dashboard/owner-dashboard.component').then((m) => m.OwnerDashboardComponent)
      },
      {
        path: 'properties/:id',
        loadComponent: () =>
          import('./properties/owner-property-detail.component').then((m) => m.OwnerPropertyDetailComponent)
      },
      {
        path: 'properties',
        loadComponent: () =>
          import('./properties/owner-property-list.component').then((m) => m.OwnerPropertyListComponent)
      },
      {
        path: 'tenants',
        loadComponent: () => import('./tenants/owner-tenants.component').then((m) => m.OwnerTenantsComponent)
      },
      {
        path: 'leases/pending',
        loadComponent: () => import('./leases/owner-lease-list.component').then((m) => m.OwnerLeaseListComponent),
        data: { pendingOnly: true }
      },
      {
        path: 'leases/:id',
        loadComponent: () =>
          import('./leases/owner-lease-detail.component').then((m) => m.OwnerLeaseDetailComponent)
      },
      {
        path: 'leases',
        loadComponent: () => import('./leases/owner-lease-list.component').then((m) => m.OwnerLeaseListComponent),
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

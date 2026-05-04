import { Routes } from '@angular/router';
import { keycloakAuthGuard } from './auth/keycloak-auth.guard';
import { MainLayoutComponent } from './layout/main-layout.component';

export const routes: Routes = [
  {
    path: '',
    canMatch: [keycloakAuthGuard],
    component: MainLayoutComponent,
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      {
        path: 'dashboard',
        loadComponent: () => import('./dashboard/dashboard.component').then((m) => m.DashboardComponent)
      },
      {
        path: 'properties',
        loadComponent: () => import('./properties/property-list.component').then((m) => m.PropertyListComponent)
      },
      {
        path: 'properties/new',
        loadComponent: () => import('./properties/property-form.component').then((m) => m.PropertyFormComponent)
      },
      {
        path: 'properties/:id/edit',
        loadComponent: () => import('./properties/property-form.component').then((m) => m.PropertyFormComponent)
      },
      {
        path: 'properties/:id',
        loadComponent: () => import('./properties/property-detail.component').then((m) => m.PropertyDetailComponent)
      },
      {
        path: 'owners',
        loadComponent: () => import('./owners/owner-list.component').then((m) => m.OwnerListComponent)
      },
      {
        path: 'owners/new',
        loadComponent: () => import('./owners/owner-form.component').then((m) => m.OwnerFormComponent)
      },
      {
        path: 'owners/:id/edit',
        loadComponent: () => import('./owners/owner-form.component').then((m) => m.OwnerFormComponent)
      },
      {
        path: 'owners/:id',
        loadComponent: () => import('./owners/owner-detail.component').then((m) => m.OwnerDetailComponent)
      },
      {
        path: 'tenants',
        loadComponent: () => import('./tenants/tenant-list.component').then((m) => m.TenantListComponent)
      },
      {
        path: 'tenants/new',
        loadComponent: () => import('./tenants/tenant-form.component').then((m) => m.TenantFormComponent)
      },
      {
        path: 'tenants/:id/edit',
        loadComponent: () => import('./tenants/tenant-form.component').then((m) => m.TenantFormComponent)
      },
      {
        path: 'tenants/:id',
        loadComponent: () => import('./tenants/tenant-detail.component').then((m) => m.TenantDetailComponent)
      },
      {
        path: 'leases',
        loadComponent: () => import('./leases/lease-list.component').then((m) => m.LeaseListComponent)
      },
      {
        path: 'leases/new',
        loadComponent: () => import('./leases/lease-form.component').then((m) => m.LeaseFormComponent)
      },
      {
        path: 'leases/:id/edit',
        loadComponent: () => import('./leases/lease-form.component').then((m) => m.LeaseFormComponent)
      },
      {
        path: 'leases/:id',
        loadComponent: () => import('./leases/lease-detail.component').then((m) => m.LeaseDetailComponent)
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
  }
];

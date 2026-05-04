import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { httpErrorMessage } from '../core/http-error-message';
import type { LeaseRecord, PropertyRecord } from '../models/lms.models';
import type { LeaseStatus } from '../models/lms.models';
import { LeaseApiService } from '../services/lease-api.service';
import { PropertyApiService } from '../services/property-api.service';
import { formatEnum } from '../shared/format-enum';

@Component({
  selector: 'app-dashboard',
  imports: [CommonModule, RouterLink, CurrencyPipe, DatePipe],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent implements OnInit {
  private readonly propertiesApi = inject(PropertyApiService);
  private readonly leasesApi = inject(LeaseApiService);

  properties: PropertyRecord[] = [];
  leases: LeaseRecord[] = [];
  loading = true;
  errorMessage = '';

  readonly leaseStatuses: LeaseStatus[] = [
    'DRAFT',
    'PENDING_APPROVAL',
    'ACTIVE',
    'EXPIRED',
    'TERMINATED'
  ];

  ngOnInit(): void {
    this.loading = true;
    forkJoin({
      properties: this.propertiesApi.list({}),
      leases: this.leasesApi.list({})
    }).subscribe({
      next: ({ properties, leases }) => {
        this.properties = properties;
        this.leases = leases;
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = httpErrorMessage(err, 'Unable to load dashboard data');
        this.loading = false;
      }
    });
  }

  get totalProperties(): number {
    return this.properties.length;
  }

  get occupiedProperties(): number {
    return new Set(
      this.leases
        .filter((lease) => lease.status === 'ACTIVE' || lease.status === 'PENDING_APPROVAL')
        .map((lease) => lease.propertyId)
    ).size;
  }

  get occupancyRate(): number {
    return this.totalProperties === 0
      ? 0
      : Math.round((this.occupiedProperties / this.totalProperties) * 100);
  }

  get monthlyRevenue(): number {
    return this.leases
      .filter((lease) => lease.status === 'ACTIVE')
      .reduce((total, lease) => total + Number(lease.monthlyRent ?? 0), 0);
  }

  get pendingLeases(): number {
    return this.leases.filter((lease) => lease.status === 'PENDING_APPROVAL').length;
  }

  get recentLeases(): LeaseRecord[] {
    return [...this.leases]
      .sort((left, right) => right.updatedAt.localeCompare(left.updatedAt))
      .slice(0, 8);
  }

  leasesForStatus(status: LeaseStatus): LeaseRecord[] {
    return this.leases.filter((lease) => lease.status === status);
  }

  leasesForStatusCount(status: LeaseStatus): number {
    return this.leasesForStatus(status).length;
  }

  propertyLabel(propertyId: number): string {
    return this.properties.find((property) => property.id === propertyId)?.name ?? `Property #${propertyId}`;
  }

  formatEnum(value: string): string {
    return formatEnum(value);
  }
}

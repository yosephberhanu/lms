import { CommonModule, CurrencyPipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { httpErrorMessage } from '../core/http-error-message';
import type { LeaseRecord, PropertyRecord } from '../models/lms.models';
import { LeaseApiService } from '../services/lease-api.service';
import { OwnerSessionService } from '../services/owner-session.service';
import { PropertyApiService } from '../services/property-api.service';

@Component({
  selector: 'app-owner-dashboard',
  imports: [CommonModule, RouterLink, CurrencyPipe],
  templateUrl: './owner-dashboard.component.html',
  styleUrl: './owner-dashboard.component.css'
})
export class OwnerDashboardComponent implements OnInit {
  readonly session = inject(OwnerSessionService);
  private readonly propertiesApi = inject(PropertyApiService);
  private readonly leasesApi = inject(LeaseApiService);

  properties: PropertyRecord[] = [];
  leases: LeaseRecord[] = [];
  loading = true;
  errorMessage = '';

  ngOnInit(): void {
    const pid = this.session.currentPartyId();
    this.loading = true;
    forkJoin({
      properties: this.propertiesApi.list({ ownerPartyId: pid }),
      leases: this.leasesApi.list({ propertyOwnerPartyId: pid })
    }).subscribe({
      next: ({ properties, leases }) => {
        this.properties = properties;
        this.leases = leases;
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = httpErrorMessage(err, 'Unable to load dashboard');
        this.loading = false;
      }
    });
  }

  get pendingApprovals(): LeaseRecord[] {
    const party = this.session.currentPartyId();
    return this.leases.filter(
      (l) =>
        l.status === 'PENDING_APPROVAL' &&
        !l.ownerApprovedAt &&
        !!l.ownerId &&
        l.ownerId.trim() === party
    );
  }

  get activeLeases(): LeaseRecord[] {
    return this.leases.filter((l) => l.status === 'ACTIVE');
  }

  get monthlyRentActive(): number {
    return this.activeLeases.reduce((s, l) => s + Number(l.monthlyRent ?? 0), 0);
  }
}

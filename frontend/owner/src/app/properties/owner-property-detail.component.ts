import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { httpErrorMessage } from '../core/http-error-message';
import type { LeaseRecord, PropertyRecord } from '../models/lms.models';
import { LeaseApiService } from '../services/lease-api.service';
import { OwnerSessionService } from '../services/owner-session.service';
import { PropertyApiService } from '../services/property-api.service';
import { formatEnum } from '../shared/format-enum';

@Component({
  selector: 'app-owner-property-detail',
  imports: [CommonModule, RouterLink, DatePipe, CurrencyPipe],
  templateUrl: './owner-property-detail.component.html',
  styleUrl: './owner-property-detail.component.css'
})
export class OwnerPropertyDetailComponent implements OnInit {
  private readonly api = inject(PropertyApiService);
  private readonly leaseApi = inject(LeaseApiService);
  private readonly route = inject(ActivatedRoute);
  readonly session = inject(OwnerSessionService);

  property: PropertyRecord | null = null;
  portfolio: PropertyRecord[] = [];
  leasesOnProperty: LeaseRecord[] = [];
  errorMessage = '';
  loading = true;
  notOwner = false;

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.errorMessage = 'Missing id';
      this.loading = false;
      return;
    }
    const pid = +id;
    const party = this.session.currentPartyId();
    forkJoin({
      property: this.api.get(pid),
      portfolio: this.api.list({ ownerPartyId: party }),
      leases: this.leaseApi.list({ propertyOwnerPartyId: party, propertyId: pid })
    }).subscribe({
      next: ({ property, portfolio, leases }) => {
        const inPortfolio = portfolio.some((row) => row.id === property.id);
        const onDeed =
          property.ownerships?.some((o) => (o.ownerPartyId ?? '').trim() === party) ?? false;
        if (!inPortfolio && !onDeed) {
          this.notOwner = true;
          this.property = null;
          this.portfolio = [];
          this.leasesOnProperty = [];
        } else {
          this.notOwner = false;
          this.property = property;
          this.portfolio = [...portfolio].sort((a, b) => a.name.localeCompare(b.name));
          this.leasesOnProperty = [...leases].sort((a, b) => b.id - a.id);
        }
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = httpErrorMessage(err, 'Unable to load property');
        this.loading = false;
      }
    });
  }

  formatEnum(v: string): string {
    return formatEnum(v);
  }
}

import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { httpErrorMessage } from '../core/http-error-message';
import type { LeaseRecord, PropertyRecord } from '../models/lms.models';
import { LeaseApiService } from '../services/lease-api.service';
import { PropertyApiService } from '../services/property-api.service';
import { formatEnum } from '../shared/format-enum';

@Component({
  selector: 'app-property-detail',
  imports: [CommonModule, RouterLink, DatePipe, CurrencyPipe],
  templateUrl: './property-detail.component.html',
  styleUrl: './property-detail.component.css'
})
export class PropertyDetailComponent implements OnInit {
  private readonly api = inject(PropertyApiService);
  private readonly leaseApi = inject(LeaseApiService);
  private readonly route = inject(ActivatedRoute);

  property: PropertyRecord | null = null;
  leases: LeaseRecord[] = [];
  errorMessage = '';
  loading = true;

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.errorMessage = 'Missing property id';
      this.loading = false;
      return;
    }
    const pid = +id;
    forkJoin({
      property: this.api.get(pid),
      leases: this.leaseApi.list({ propertyId: pid })
    }).subscribe({
      next: ({ property, leases }) => {
        this.property = property;
        this.leases = [...leases].sort((a, b) => b.id - a.id);
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = httpErrorMessage(err, 'Unable to load property');
        this.loading = false;
      }
    });
  }

  formatEnum(value: string): string {
    return formatEnum(value);
  }
}

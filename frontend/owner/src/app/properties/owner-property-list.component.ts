import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { httpErrorMessage } from '../core/http-error-message';
import type { PropertyRecord } from '../models/lms.models';
import { OwnerSessionService } from '../services/owner-session.service';
import { PropertyApiService } from '../services/property-api.service';
import { formatEnum } from '../shared/format-enum';

@Component({
  selector: 'app-owner-property-list',
  imports: [CommonModule, RouterLink],
  templateUrl: './owner-property-list.component.html',
  styleUrl: './owner-property-list.component.css'
})
export class OwnerPropertyListComponent implements OnInit {
  readonly session = inject(OwnerSessionService);
  private readonly api = inject(PropertyApiService);

  rows: PropertyRecord[] = [];
  loading = true;
  errorMessage = '';

  ngOnInit(): void {
    const pid = this.session.currentPartyId();
    this.api.list({ ownerPartyId: pid }).subscribe({
      next: (rows) => {
        this.rows = rows;
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = httpErrorMessage(err, 'Unable to load properties');
        this.loading = false;
      }
    });
  }

  formatEnum(v: string): string {
    return formatEnum(v);
  }
}

import { CommonModule } from '@angular/common';
import {
  AfterViewInit,
  ChangeDetectorRef,
  Component,
  NgZone,
  OnDestroy,
  OnInit,
  inject
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { httpErrorMessage } from '../core/http-error-message';
import type { PropertyRecord, PropertyType } from '../models/lms.models';
import type { LeaseRecord, LeaseStatus } from '../models/lms.models';
import { LeaseApiService } from '../services/lease-api.service';
import { PropertyApiService } from '../services/property-api.service';
import { formatEnum } from '../shared/format-enum';

type PropertyRow = PropertyRecord & {
  leaseStatusLabel: string;
  canCreateLease: boolean;
};

@Component({
  selector: 'app-property-list',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './property-list.component.html',
  styleUrl: './property-list.component.css'
})
export class PropertyListComponent implements OnInit, AfterViewInit, OnDestroy {
  private readonly api = inject(PropertyApiService);
  private readonly leaseApi = inject(LeaseApiService);
  private readonly router = inject(Router);
  private readonly zone = inject(NgZone);
  private readonly cdr = inject(ChangeDetectorRef);

  private dt?: { destroy: () => void };

  rows: PropertyRow[] = [];
  loading = false;
  errorMessage = '';

  propertySearch = '';
  cityFilter = '';
  selectedTypeFilter = '' as PropertyType | '';

  readonly propertyTypes: PropertyType[] = [
    'RESIDENTIAL',
    'COMMERCIAL',
    'INDUSTRIAL',
    'MIXED_USE',
    'LAND',
    'OTHER'
  ];

  readonly tableId = 'properties-dt';

  ngOnInit(): void {
    this.load();
  }

  ngAfterViewInit(): void {
    this.bindDelegatedActions();
  }

  ngOnDestroy(): void {
    this.destroyTable();
    $(document).off('click.prop-actions');
  }

  load(): void {
    this.loading = true;
    this.errorMessage = '';
    forkJoin({
      properties: this.api.list({
        q: this.propertySearch,
        city: this.cityFilter,
        propertyType: this.selectedTypeFilter
      }),
      leases: this.leaseApi.list({})
    }).subscribe({
      next: ({ properties, leases }) => {
        const leaseByProperty = computeBlockingLeaseStatusByProperty(leases);
        this.rows = properties.map((p) => {
          const st = leaseByProperty.get(p.id);
          return {
            ...p,
            leaseStatusLabel: st ?? 'AVAILABLE',
            canCreateLease: !st
          };
        });
        this.loading = false;
        this.cdr.detectChanges();
        setTimeout(() => this.initTable(), 0);
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = httpErrorMessage(err, 'Unable to load properties');
      }
    });
  }

  clearFilters(): void {
    this.propertySearch = '';
    this.cityFilter = '';
    this.selectedTypeFilter = '';
    this.load();
  }

  private destroyTable(): void {
    if (this.dt) {
      this.dt.destroy();
      this.dt = undefined;
    }
  }

  private initTable(): void {
    this.destroyTable();
    const el = $(`#${this.tableId}`);
    if (!el.length) {
      return;
    }

    this.dt = el.DataTable({
      data: this.rows,
      order: [[5, 'desc']],
      pageLength: 25,
      columns: [
        { data: 'name', title: 'Name' },
        {
          data: 'propertyType',
          title: 'Type',
          render: (d: string) => formatEnum(d)
        },
        { data: 'city', title: 'City' },
        { data: 'country', title: 'Country' },
        {
          data: 'leaseStatusLabel',
          title: 'Lease',
          render: (d: string) => formatEnum(d)
        },
        {
          data: 'ownerships',
          title: 'Owners',
          orderable: false,
          render: (d: PropertyRecord['ownerships']) => String(d?.length ?? 0)
        },
        {
          data: 'updatedAt',
          title: 'Updated',
          render: (d: string) => {
            const d2 = new Date(d);
            return isNaN(d2.getTime()) ? d : d2.toLocaleDateString();
          }
        },
        {
          data: null,
          title: 'Actions',
          orderable: false,
          searchable: false,
          render: (_d: unknown, _t: string, row: PropertyRow) => {
            const leaseBtn = row.canCreateLease
              ? `<button type="button" class="btn btn-success js-prop-new-lease" data-id="${row.id}">New lease</button>`
              : `<button type="button" class="btn btn-secondary" disabled title="Property not available">New lease</button>`;
            return `<div class="btn-group btn-group-sm">
              ${leaseBtn}
              <button type="button" class="btn btn-info js-prop-view" data-id="${row.id}">View</button>
              <button type="button" class="btn btn-warning js-prop-edit" data-id="${row.id}">Edit</button>
              <button type="button" class="btn btn-danger js-prop-delete" data-id="${row.id}">Delete</button>
            </div>`;
          }
        }
      ]
    });
  }

  private bindDelegatedActions(): void {
    $(document).on('click.prop-actions', '.js-prop-new-lease', (e) => {
      const id = $(e.currentTarget).data('id') as number;
      this.zone.run(() => this.router.navigate(['/leases/new'], { queryParams: { propertyId: id } }));
    });
    $(document).on('click.prop-actions', '.js-prop-view', (e) => {
      const id = $(e.currentTarget).data('id') as number;
      this.zone.run(() => this.router.navigate(['/properties', id]));
    });
    $(document).on('click.prop-actions', '.js-prop-edit', (e) => {
      const id = $(e.currentTarget).data('id') as number;
      this.zone.run(() => this.router.navigate(['/properties', id, 'edit']));
    });
    $(document).on('click.prop-actions', '.js-prop-delete', (e) => {
      const id = $(e.currentTarget).data('id') as number;
      this.zone.run(() => this.confirmDelete(id));
    });
  }

  private confirmDelete(id: number): void {
    if (!window.confirm('Delete this property? This cannot be undone.')) {
      return;
    }
    this.api.delete(id).subscribe({
      next: () => this.load(),
      error: (err) => {
        this.errorMessage = httpErrorMessage(err, 'Unable to delete property');
      }
    });
  }
}

function computeBlockingLeaseStatusByProperty(leases: LeaseRecord[]): Map<number, LeaseStatus> {
  const blocking = new Set<LeaseStatus>(['DRAFT', 'PENDING_APPROVAL', 'ACTIVE']);
  const map = new Map<number, LeaseStatus>();
  for (const l of leases ?? []) {
    if (blocking.has(l.status) && !map.has(l.propertyId)) {
      map.set(l.propertyId, l.status);
    }
  }
  return map;
}

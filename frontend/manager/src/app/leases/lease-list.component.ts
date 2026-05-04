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
import { httpErrorMessage } from '../core/http-error-message';
import type { LeaseRecord, LeaseStatus } from '../models/lms.models';
import { LeaseApiService } from '../services/lease-api.service';
import { formatEnum } from '../shared/format-enum';

@Component({
  selector: 'app-lease-list',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './lease-list.component.html',
  styleUrl: './lease-list.component.css'
})
export class LeaseListComponent implements OnInit, AfterViewInit, OnDestroy {
  private readonly api = inject(LeaseApiService);
  private readonly router = inject(Router);
  private readonly zone = inject(NgZone);
  private readonly cdr = inject(ChangeDetectorRef);

  private dt?: { destroy: () => void };

  rows: LeaseRecord[] = [];
  loading = false;
  errorMessage = '';

  propertyId = '';
  tenantId = '';
  statusFilter = '' as LeaseStatus | '';

  readonly statuses: LeaseStatus[] = [
    'DRAFT',
    'PENDING_APPROVAL',
    'ACTIVE',
    'EXPIRED',
    'TERMINATED'
  ];

  readonly tableId = 'leases-dt';

  ngOnInit(): void {
    this.load();
  }

  ngAfterViewInit(): void {
    this.bindDelegatedActions();
  }

  ngOnDestroy(): void {
    this.destroyTable();
    $(document).off('click.lease-actions');
  }

  load(): void {
    this.loading = true;
    this.errorMessage = '';
    const parseId = (s: string): number | undefined => {
      const t = s.trim();
      if (!t) {
        return undefined;
      }
      const n = Number(t);
      return Number.isFinite(n) ? n : undefined;
    };
    const pid = parseId(this.propertyId);
    const tid = parseId(this.tenantId);
    const st = this.statusFilter || undefined;

    this.api
      .list({
        propertyId: pid,
        tenantId: tid,
        status: st
      })
      .subscribe({
        next: (rows) => {
          this.rows = rows;
          this.loading = false;
          this.cdr.detectChanges();
          setTimeout(() => this.initTable(), 0);
        },
        error: (err) => {
          this.loading = false;
          this.errorMessage = httpErrorMessage(err, 'Unable to load leases');
        }
      });
  }

  clearFilters(): void {
    this.propertyId = '';
    this.tenantId = '';
    this.statusFilter = '';
    this.load();
  }

  formatEnum(value: string): string {
    return formatEnum(value);
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
      order: [[7, 'desc']],
      pageLength: 25,
      columns: [
        { data: 'id', title: 'ID' },
        {
          data: 'propertyNameSnapshot',
          title: 'Property',
          render: (d: string | null, _t: string, row: LeaseRecord) => d || `Property #${row.propertyId}`
        },
        {
          data: 'tenantNameSnapshot',
          title: 'Tenant',
          render: (d: string | null, _t: string, row: LeaseRecord) =>
            d || row.tenant?.displayName || `Tenant #${row.tenantId}`
        },
        {
          data: 'status',
          title: 'Status',
          render: (d: string) => formatEnum(d)
        },
        {
          data: 'monthlyRent',
          title: 'Rent / mo',
          render: (d: number) =>
            new Intl.NumberFormat(undefined, { style: 'currency', currency: 'USD' }).format(Number(d))
        },
        { data: 'startDate', title: 'Start' },
        { data: 'endDate', title: 'End' },
        {
          data: 'updatedAt',
          title: 'Updated',
          render: (d: string) => {
            const d2 = new Date(d);
            return isNaN(d2.getTime()) ? d : d2.toLocaleString();
          }
        },
        {
          data: null,
          title: 'Actions',
          orderable: false,
          searchable: false,
          render: (_d: unknown, _t: string, row: LeaseRecord) => {
            let html = `<button type="button" class="btn btn-info js-lease-view" data-id="${row.id}">View</button>`;
            if (row.status === 'DRAFT' || row.status === 'PENDING_APPROVAL') {
              html += ` <button type="button" class="btn btn-warning js-lease-edit" data-id="${row.id}">Edit</button>`;
            }
            return `<div class="btn-group btn-group-sm">${html}</div>`;
          }
        }
      ]
    });
  }

  private bindDelegatedActions(): void {
    $(document).on('click.lease-actions', '.js-lease-view', (e) => {
      const id = $(e.currentTarget).data('id') as number;
      this.zone.run(() => this.router.navigate(['/leases', id]));
    });
    $(document).on('click.lease-actions', '.js-lease-edit', (e) => {
      const id = $(e.currentTarget).data('id') as number;
      this.zone.run(() => this.router.navigate(['/leases', id, 'edit']));
    });
  }
}

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
import type { TenantRecord } from '../models/lms.models';
import { TenantApiService } from '../services/tenant-api.service';

@Component({
  selector: 'app-tenant-list',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './tenant-list.component.html',
  styleUrl: './tenant-list.component.css'
})
export class TenantListComponent implements OnInit, AfterViewInit, OnDestroy {
  private readonly api = inject(TenantApiService);
  private readonly router = inject(Router);
  private readonly zone = inject(NgZone);
  private readonly cdr = inject(ChangeDetectorRef);

  private dt?: { destroy: () => void };

  rows: TenantRecord[] = [];
  loading = false;
  errorMessage = '';

  q = '';
  externalPartyId = '';
  displayName = '';
  email = '';
  phone = '';

  readonly tableId = 'tenants-dt';

  ngOnInit(): void {
    this.load();
  }

  ngAfterViewInit(): void {
    this.bindDelegatedActions();
  }

  ngOnDestroy(): void {
    this.destroyTable();
    $(document).off('click.tenant-actions');
  }

  load(): void {
    this.loading = true;
    this.errorMessage = '';
    this.api
      .list({
        q: this.q,
        externalPartyId: this.externalPartyId,
        displayName: this.displayName,
        email: this.email,
        phone: this.phone
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
          this.errorMessage = httpErrorMessage(err, 'Unable to load tenants');
        }
      });
  }

  clearFilters(): void {
    this.q = '';
    this.externalPartyId = '';
    this.displayName = '';
    this.email = '';
    this.phone = '';
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
        { data: 'externalPartyId', title: 'External ID', defaultContent: '—' },
        { data: 'displayName', title: 'Name' },
        { data: 'email', title: 'Email', defaultContent: '—' },
        { data: 'phone', title: 'Phone', defaultContent: '—' },
        { data: 'status', title: 'Status', defaultContent: '—' },
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
          render: (_d: unknown, _t: string, row: TenantRecord) =>
            `<div class="btn-group btn-group-sm">
              <button type="button" class="btn btn-info js-tenant-view" data-id="${row.id}">View</button>
              <button type="button" class="btn btn-warning js-tenant-edit" data-id="${row.id}">Edit</button>
              <button type="button" class="btn btn-danger js-tenant-delete" data-id="${row.id}">Delete</button>
            </div>`
        }
      ]
    });
  }

  private bindDelegatedActions(): void {
    $(document).on('click.tenant-actions', '.js-tenant-view', (e) => {
      const id = $(e.currentTarget).data('id') as number;
      this.zone.run(() => this.router.navigate(['/tenants', id]));
    });
    $(document).on('click.tenant-actions', '.js-tenant-edit', (e) => {
      const id = $(e.currentTarget).data('id') as number;
      this.zone.run(() => this.router.navigate(['/tenants', id, 'edit']));
    });
    $(document).on('click.tenant-actions', '.js-tenant-delete', (e) => {
      const id = $(e.currentTarget).data('id') as number;
      this.zone.run(() => this.confirmDelete(id));
    });
  }

  private confirmDelete(id: number): void {
    if (!window.confirm('Delete this tenant?')) {
      return;
    }
    this.api.delete(id).subscribe({
      next: () => this.load(),
      error: (err) => {
        this.errorMessage = httpErrorMessage(err, 'Unable to delete tenant');
      }
    });
  }
}

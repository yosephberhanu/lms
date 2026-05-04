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
import type { OwnerRecord } from '../models/lms.models';
import { OwnerApiService } from '../services/owner-api.service';

@Component({
  selector: 'app-owner-list',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './owner-list.component.html',
  styleUrl: './owner-list.component.css'
})
export class OwnerListComponent implements OnInit, AfterViewInit, OnDestroy {
  private readonly api = inject(OwnerApiService);
  private readonly router = inject(Router);
  private readonly zone = inject(NgZone);
  private readonly cdr = inject(ChangeDetectorRef);

  private dt?: { destroy: () => void };

  rows: OwnerRecord[] = [];
  loading = false;
  errorMessage = '';

  q = '';
  partyId = '';
  displayName = '';
  email = '';
  phone = '';

  readonly tableId = 'owners-dt';

  ngOnInit(): void {
    this.load();
  }

  ngAfterViewInit(): void {
    this.bindDelegatedActions();
  }

  ngOnDestroy(): void {
    this.destroyTable();
    $(document).off('click.owner-actions');
  }

  load(): void {
    this.loading = true;
    this.errorMessage = '';
    this.api
      .list({
        q: this.q,
        partyId: this.partyId,
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
          this.errorMessage = httpErrorMessage(err, 'Unable to load owners');
        }
      });
  }

  clearFilters(): void {
    this.q = '';
    this.partyId = '';
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
      order: [[4, 'desc']],
      pageLength: 25,
      columns: [
        { data: 'partyId', title: 'Party ID' },
        { data: 'displayName', title: 'Name' },
        { data: 'email', title: 'Email' },
        { data: 'phone', title: 'Phone' },
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
          render: (_d: unknown, _t: string, row: OwnerRecord) =>
            `<div class="btn-group btn-group-sm">
              <button type="button" class="btn btn-info js-owner-view" data-id="${row.id}">View</button>
              <button type="button" class="btn btn-warning js-owner-edit" data-id="${row.id}">Edit</button>
              <button type="button" class="btn btn-danger js-owner-delete" data-id="${row.id}">Delete</button>
            </div>`
        }
      ]
    });
  }

  private bindDelegatedActions(): void {
    $(document).on('click.owner-actions', '.js-owner-view', (e) => {
      const id = $(e.currentTarget).data('id') as number;
      this.zone.run(() => this.router.navigate(['/owners', id]));
    });
    $(document).on('click.owner-actions', '.js-owner-edit', (e) => {
      const id = $(e.currentTarget).data('id') as number;
      this.zone.run(() => this.router.navigate(['/owners', id, 'edit']));
    });
    $(document).on('click.owner-actions', '.js-owner-delete', (e) => {
      const id = $(e.currentTarget).data('id') as number;
      this.zone.run(() => this.confirmDelete(id));
    });
  }

  private confirmDelete(id: number): void {
    if (!window.confirm('Delete this owner?')) {
      return;
    }
    this.api.delete(id).subscribe({
      next: () => this.load(),
      error: (err) => {
        this.errorMessage = httpErrorMessage(err, 'Unable to delete owner');
      }
    });
  }
}

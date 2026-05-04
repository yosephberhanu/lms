import { CommonModule, DatePipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { httpErrorMessage } from '../core/http-error-message';
import type { TenantRecord } from '../models/lms.models';
import { TenantApiService } from '../services/tenant-api.service';

@Component({
  selector: 'app-tenant-detail',
  imports: [CommonModule, RouterLink, DatePipe],
  templateUrl: './tenant-detail.component.html',
  styleUrl: './tenant-detail.component.css'
})
export class TenantDetailComponent implements OnInit {
  private readonly api = inject(TenantApiService);
  private readonly route = inject(ActivatedRoute);

  tenant: TenantRecord | null = null;
  errorMessage = '';
  loading = true;

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.errorMessage = 'Missing id';
      this.loading = false;
      return;
    }
    this.api.get(+id).subscribe({
      next: (tenant) => {
        this.tenant = tenant;
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = httpErrorMessage(err, 'Unable to load tenant');
        this.loading = false;
      }
    });
  }
}

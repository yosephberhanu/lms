import { CommonModule, DatePipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { map, switchMap } from 'rxjs/operators';
import { httpErrorMessage } from '../core/http-error-message';
import type { OwnerRecord, PropertyRecord } from '../models/lms.models';
import { OwnerApiService } from '../services/owner-api.service';
import { PropertyApiService } from '../services/property-api.service';
import { formatEnum } from '../shared/format-enum';

@Component({
  selector: 'app-owner-detail',
  imports: [CommonModule, RouterLink, DatePipe],
  templateUrl: './owner-detail.component.html',
  styleUrl: './owner-detail.component.css'
})
export class OwnerDetailComponent implements OnInit {
  private readonly api = inject(OwnerApiService);
  private readonly propertyApi = inject(PropertyApiService);
  private readonly route = inject(ActivatedRoute);

  owner: OwnerRecord | null = null;
  properties: PropertyRecord[] = [];
  errorMessage = '';
  loading = true;

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.errorMessage = 'Missing id';
      this.loading = false;
      return;
    }
    this.api
      .get(+id)
      .pipe(
        switchMap((owner) =>
          this.propertyApi.list({ ownerPartyId: owner.partyId }).pipe(map((properties) => ({ owner, properties })))
        )
      )
      .subscribe({
        next: ({ owner, properties }) => {
          this.owner = owner;
          this.properties = [...properties].sort((a, b) => a.name.localeCompare(b.name));
          this.loading = false;
        },
        error: (err) => {
          this.errorMessage = httpErrorMessage(err, 'Unable to load owner');
          this.loading = false;
        }
      });
  }

  formatPropertyType(value: string): string {
    return formatEnum(value);
  }
}

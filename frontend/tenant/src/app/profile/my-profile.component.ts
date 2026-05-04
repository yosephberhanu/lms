import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { httpErrorMessage } from '../core/http-error-message';
import { CommunicationPreferencesApiService } from '../services/communication-preferences-api.service';
import { MyProfile, MyProfileApiService, MyProfileUpdate } from '../services/my-profile-api.service';

@Component({
  selector: 'app-my-profile',
  imports: [CommonModule, FormsModule],
  templateUrl: './my-profile.component.html'
})
export class MyProfileComponent implements OnInit {
  private readonly api = inject(MyProfileApiService);
  private readonly commPrefs = inject(CommunicationPreferencesApiService);

  loading = true;
  saving = false;
  errorMessage = '';
  successMessage = '';

  profile: MyProfile | null = null;
  draft: MyProfileUpdate & { notifyEmail?: boolean; notifySms?: boolean } = {};

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.errorMessage = '';
    this.successMessage = '';
    this.api.me().subscribe({
      next: (p) => {
        this.profile = p;
        this.draft = {
          email: p.email,
          phone: p.phone,
          firstName: p.firstName,
          lastName: p.lastName,
          notifyEmail: false,
          notifySms: false
        };
        this.commPrefs.get().subscribe({
          next: (cp) => {
            this.draft.notifyEmail = cp.notifyEmail;
            this.draft.notifySms = cp.notifySms;
            this.draft.email = p.email ?? cp.email ?? null;
            this.draft.phone = p.phone ?? cp.phone ?? null;
            this.loading = false;
          },
          error: (err) => {
            this.loading = false;
            this.errorMessage = httpErrorMessage(err, 'Unable to load communication preferences');
          }
        });
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = httpErrorMessage(err, 'Unable to load profile');
      }
    });
  }

  save(): void {
    this.saving = true;
    this.errorMessage = '';
    this.successMessage = '';
    this.api.update(this.draft).subscribe({
      next: (p) => {
        this.profile = p;
        this.draft.email = p.email;
        this.draft.phone = p.phone;
        this.commPrefs
          .put({
            notifyEmail: !!this.draft.notifyEmail,
            notifySms: !!this.draft.notifySms,
            email: (this.draft.email ?? '').trim() || null,
            phone: (this.draft.phone ?? '').trim() || null
          })
          .subscribe({
            next: () => {
              this.saving = false;
              this.successMessage = 'Profile updated.';
            },
            error: (err) => {
              this.saving = false;
              this.successMessage = 'Profile updated.';
              this.errorMessage = httpErrorMessage(err, 'Profile saved, but communication preferences were not synced');
            }
          });
      },
      error: (err) => {
        this.saving = false;
        this.errorMessage = httpErrorMessage(err, 'Unable to update profile');
      }
    });
  }
}


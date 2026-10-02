import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface ChangePasswordPayload {
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
}

export interface TwoFactorSetupData {
  secret: string;
  otpauthUrl: string;
  qrCodeDataUrl: string;
}

export interface TwoFactorConfirmData {
  message: string;
  backupCodes: string[];
}

export interface TwoFactorStatusData {
  is2faEnabled: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class AdminProfileService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/admin/profile`;

  changePassword(payload: ChangePasswordPayload): Observable<{ message: string }> {
    return this.http.put<{ message: string }>(`${this.baseUrl}/password`, payload);
  }

  setup2fa(): Observable<TwoFactorSetupData> {
    return this.http.post<TwoFactorSetupData>(`${this.baseUrl}/2fa/setup`, {});
  }

  confirm2fa(code: string): Observable<TwoFactorConfirmData> {
    return this.http.post<TwoFactorConfirmData>(`${this.baseUrl}/2fa/confirm`, { code });
  }

  disable2fa(currentPassword: string): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.baseUrl}/2fa/disable`, { currentPassword });
  }

  get2faStatus(): Observable<TwoFactorStatusData> {
    return this.http.get<TwoFactorStatusData>(`${this.baseUrl}/2fa/status`);
  }
}

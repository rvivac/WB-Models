import { Injectable, inject, signal, computed } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { ApiService } from './api.service';
import {
  LoginRequest,
  AuthResponse,
  ForgotPasswordRequest,
  ResetPasswordRequest,
  MessageResponse,
  AdminUser
} from '../../shared/models/auth.interface';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);

  private readonly TOKEN_KEY = 'wb_scouting_token';
  private readonly USER_KEY = 'wb_scouting_user';

  // Signals para reatividade moderna no Angular 18
  readonly currentUser = signal<AdminUser | null>(this.loadUserFromStorage());
  readonly isAuthenticated = computed(() => !!this.currentUser());

  login(credentials: LoginRequest): Observable<AuthResponse> {
    return this.api.post<AuthResponse>('/auth/login', credentials).pipe(
      tap(response => {
        this.setSession(response);
      })
    );
  }

  forgotPassword(request: ForgotPasswordRequest): Observable<MessageResponse> {
    return this.api.post<MessageResponse>('/auth/forgot-password', request);
  }

  resetPassword(request: ResetPasswordRequest): Observable<MessageResponse> {
    return this.api.post<MessageResponse>('/auth/reset-password', request);
  }

  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.USER_KEY);
    this.currentUser.set(null);
    this.router.navigate(['/admin/login']);
  }

  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  private setSession(authResponse: any): void {
    // Captura resiliente do token JWT devolvido pelo Spring Boot
    const jwt = authResponse.token || authResponse.accessToken || authResponse.jwt || authResponse.data?.token;

    if (!jwt) {
      console.error('Token JWT não encontrado na resposta de autenticação:', authResponse);
      return;
    }

    localStorage.setItem(this.TOKEN_KEY, jwt);

    const user: AdminUser = {
      name: authResponse.name || authResponse.adminName || authResponse.user?.name || 'Administrador',
      email: authResponse.email || authResponse.adminEmail || authResponse.user?.email || 'admin@wbscouting.com',
      role: authResponse.role || authResponse.user?.role || 'SUPER_ADMIN'
    };

    localStorage.setItem(this.USER_KEY, JSON.stringify(user));
    this.currentUser.set(user);
  }

  private loadUserFromStorage(): AdminUser | null {
    try {
      const stored = localStorage.getItem(this.USER_KEY);
      return stored ? JSON.parse(stored) : null;
    } catch {
      return null;
    }
  }
}

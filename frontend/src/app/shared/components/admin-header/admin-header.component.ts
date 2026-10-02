import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../../core/services/auth.service';
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-admin-header',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './admin-header.component.html',
  styleUrls: ['./admin-header.component.scss']
})
export class AdminHeaderComponent implements OnInit {
  readonly authService = inject(AuthService);
  private readonly http = inject(HttpClient, { optional: true });

  readonly showConfirmModal = signal<boolean>(false);
  readonly isMobileMenuOpen = signal<boolean>(false);
  pendingCandidatesCount = 0;

  get userEmail(): string {
    return this.authService.currentUser()?.email || 'admin@wbscouting.com';
  }

  get userName(): string {
    return this.authService.currentUser()?.name || 'Administrador';
  }

  get userRole(): string {
    return (this.authService.currentUser()?.role || 'ADMIN').toUpperCase();
  }

  get isWebmasterOrSuperAdmin(): boolean {
    const role = this.userRole;
    return role === 'WEBMASTER' || role === 'SUPER_ADMIN';
  }

  ngOnInit(): void {
    this.fetchPendingCount();
  }

  fetchPendingCount(): void {
    if (!this.http) return;
    this.http.get<any>(`${environment.apiUrl}/admin/applications/counts`).subscribe({
      next: (counts) => {
        if (counts && typeof counts.pending === 'number') {
          this.pendingCandidatesCount = counts.pending;
        } else if (counts && typeof counts.PENDING === 'number') {
          this.pendingCandidatesCount = counts.PENDING;
        }
      },
      error: () => {
        // Fallback silencioso
      }
    });
  }

  toggleMobileMenu(): void {
    this.isMobileMenuOpen.update(v => !v);
  }

  closeMobileMenu(): void {
    this.isMobileMenuOpen.set(false);
  }

  logout(): void {
    this.confirmLogout();
  }

  onSecureLogout(): void {
    this.confirmLogout();
  }

  confirmLogout(): void {
    this.showConfirmModal.set(true);
  }

  cancelLogout(): void {
    this.showConfirmModal.set(false);
  }

  executeLogout(): void {
    this.showConfirmModal.set(false);
    this.authService.secureLogout();
  }
}

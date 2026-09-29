import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-admin-header',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './admin-header.component.html',
  styleUrls: ['./admin-header.component.scss']
})
export class AdminHeaderComponent {
  readonly authService = inject(AuthService);
  readonly showConfirmModal = signal<boolean>(false);

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

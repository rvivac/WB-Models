import { Component, inject, signal, OnInit, ElementRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SiteAccessService } from '../../../core/services/site-access.service';

@Component({
  selector: 'app-site-access-gate',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './site-access-gate.component.html',
  styleUrls: ['./site-access-gate.component.scss']
})
export class SiteAccessGateComponent implements OnInit {
  private readonly siteAccessService = inject(SiteAccessService);

  @ViewChild('passwordInput') passwordInputElement?: ElementRef<HTMLInputElement>;

  password = '';
  readonly showPassword = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);
  readonly isShaking = signal<boolean>(false);
  readonly isSubmitting = signal<boolean>(false);

  ngOnInit(): void {
    setTimeout(() => {
      this.passwordInputElement?.nativeElement.focus();
    }, 150);
  }

  togglePasswordVisibility(): void {
    this.showPassword.update((val) => !val);
  }

  onSubmit(): void {
    const trimmed = (this.password || '').trim();
    if (!trimmed) {
      this.triggerError('Por favor, informe a chave de acesso.');
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set(null);

    // Pequeno delay sensorial de validação de luxo
    setTimeout(() => {
      const success = this.siteAccessService.unlock(trimmed);
      this.isSubmitting.set(false);

      if (!success) {
        this.triggerError('Chave de acesso incorreta. Verifique suas credenciais de homologação.');
      }
    }, 300);
  }

  private triggerError(message: string): void {
    this.errorMessage.set(message);
    this.isShaking.set(true);
    setTimeout(() => {
      this.isShaking.set(false);
      this.passwordInputElement?.nativeElement.focus();
    }, 600);
  }
}

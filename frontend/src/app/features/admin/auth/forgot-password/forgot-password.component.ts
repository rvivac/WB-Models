import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterModule, Router } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { ButtonComponent } from '../../../../shared/components/button/button.component';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule, ButtonComponent],
  templateUrl: './forgot-password.component.html',
  styleUrls: ['./forgot-password.component.scss']
})
export class ForgotPasswordComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly isResetMode = signal<boolean>(false);
  readonly resetToken = signal<string | null>(null);

  readonly isSubmitting = signal<boolean>(false);
  readonly successMessage = signal<string | null>(null);
  readonly errorMessage = signal<string | null>(null);

  readonly forgotForm: FormGroup = this.fb.group({
    email: ['', [Validators.required, Validators.email]]
  });

  readonly resetForm: FormGroup = this.fb.group({
    newPassword: ['', [Validators.required, Validators.minLength(8)]],
    confirmPassword: ['', [Validators.required]]
  });

  ngOnInit(): void {
    const token = this.route.snapshot.queryParamMap.get('token');
    if (token) {
      this.isResetMode.set(true);
      this.resetToken.set(token);
    }
  }

  onForgotSubmit(): void {
    if (this.forgotForm.invalid) {
      this.forgotForm.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    this.authService.forgotPassword(this.forgotForm.value).subscribe({
      next: (res) => {
        this.isSubmitting.set(false);
        this.successMessage.set(res.message);
      },
      error: (err) => {
        this.isSubmitting.set(false);
        console.error('Erro na recuperação:', err);
        this.errorMessage.set('Não foi possível processar a solicitação no momento.');
      }
    });
  }

  onResetSubmit(): void {
    if (this.resetForm.invalid) {
      this.resetForm.markAllAsTouched();
      return;
    }

    const { newPassword, confirmPassword } = this.resetForm.value;
    if (newPassword !== confirmPassword) {
      this.errorMessage.set('As senhas digitadas não coincidem.');
      return;
    }

    const token = this.resetToken();
    if (!token) {
      this.errorMessage.set('Token de redefinição inválido.');
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    this.authService.resetPassword({ token, newPassword }).subscribe({
      next: (res) => {
        this.isSubmitting.set(false);
        this.successMessage.set(res.message);
        setTimeout(() => {
          this.router.navigate(['/admin/login']);
        }, 2000);
      },
      error: (err) => {
        this.isSubmitting.set(false);
        console.error('Erro na redefinição:', err);
        this.errorMessage.set(err.error?.detail || 'O link de recuperação é inválido ou expirou.');
      }
    });
  }
}

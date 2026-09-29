import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import {
  passwordMatchValidator,
  strongPasswordValidator
} from '../../../shared/validators/password-match.validator';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './reset-password.component.html',
  styleUrls: ['./reset-password.component.scss']
})
export class ResetPasswordComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly route = inject(ActivatedRoute);

  token: string | null = null;
  tokenInvalid = false;
  isCompleted = false;
  isLoading = false;

  readonly resetForm: FormGroup = this.fb.group(
    {
      password: ['', [Validators.required, Validators.minLength(8), strongPasswordValidator]],
      confirmPassword: ['', [Validators.required]]
    },
    { validators: passwordMatchValidator }
  );

  ngOnInit(): void {
    const rawToken = this.route.snapshot.queryParamMap.get('token');
    if (!rawToken || rawToken.trim() === '') {
      this.tokenInvalid = true;
      return;
    }
    this.token = rawToken.trim();
  }

  get hasMinLength(): boolean {
    const val = this.resetForm.get('password')?.value || '';
    return val.length >= 8;
  }

  get hasSpecialChar(): boolean {
    const val = this.resetForm.get('password')?.value || '';
    const hasNumber = /[0-9]/.test(val);
    const hasSpecial = /[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?`~]/.test(val);
    return hasNumber && hasSpecial;
  }

  onSubmit(): void {
    if (this.resetForm.invalid || !this.token || this.isLoading) {
      this.resetForm.markAllAsTouched();
      return;
    }

    this.isLoading = true;
    const { password, confirmPassword } = this.resetForm.value;

    this.authService
      .resetPassword({
        token: this.token,
        newPassword: password,
        confirmPassword: confirmPassword
      })
      .subscribe({
        next: () => {
          this.isLoading = false;
          this.isCompleted = true;
        },
        error: () => {
          this.isLoading = false;
          this.tokenInvalid = true;
        }
      });
  }
}

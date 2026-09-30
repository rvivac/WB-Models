import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-admin-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './admin-login.component.html',
  styleUrls: ['./admin-login.component.scss']
})
export class AdminLoginComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly isLoading = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);
  readonly showPassword = signal<boolean>(false);

  readonly loginForm: FormGroup = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
    rememberMe: [false]
  });

  ngOnInit(): void {
    if (typeof localStorage !== 'undefined') {
      const savedEmail = localStorage.getItem('wb_remember_email');
      if (savedEmail) {
        this.loginForm.patchValue({
          email: savedEmail,
          rememberMe: true
        });
      }
    }
  }

  togglePasswordVisibility(): void {
    this.showPassword.update((val) => !val);
  }

  onSubmit(): void {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set(null);

    const { email, password, rememberMe } = this.loginForm.value;

    if (typeof localStorage !== 'undefined') {
      if (rememberMe) {
        localStorage.setItem('wb_remember_email', email);
      } else {
        localStorage.removeItem('wb_remember_email');
      }
    }

    const credentials = { email, password };

    this.authService.login(credentials).subscribe({
      next: () => {
        this.isLoading.set(false);
        this.redirectToTarget();
      },
      error: (err) => {
        this.isLoading.set(false);
        const detail =
          err?.error?.detail ||
          err?.error?.message ||
          'Credenciais inválidas. Verifique seu e-mail e senha de acesso.';
        this.errorMessage.set(detail);
      }
    });
  }

  private redirectToTarget(): void {
    const returnUrl = this.route.snapshot.queryParams['returnUrl'] || '/admin/dashboard';
    this.router.navigateByUrl(returnUrl);
  }
}

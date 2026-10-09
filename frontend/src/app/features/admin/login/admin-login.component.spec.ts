import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AdminLoginComponent } from './admin-login.component';
import { AuthService } from '../../../core/services/auth.service';
import { ActivatedRoute, Router, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';

describe('AdminLoginComponent', () => {
  let component: AdminLoginComponent;
  let fixture: ComponentFixture<AdminLoginComponent>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let router: Router;

  beforeEach(async () => {
    localStorage.clear();
    sessionStorage.clear();
    authServiceSpy = jasmine.createSpyObj('AuthService', ['login', 'loginMock']);
    (authServiceSpy as any).isAuthenticated = jasmine.createSpy('isAuthenticated').and.returnValue(false);

    await TestBed.configureTestingModule({
      imports: [AdminLoginComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authServiceSpy },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              queryParams: { returnUrl: '/admin/candidatos' }
            }
          }
        }
      ]
    }).compileComponents();

    router = TestBed.inject(Router);
    spyOn(router, 'navigateByUrl');

    fixture = TestBed.createComponent(AdminLoginComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    localStorage.clear();
    sessionStorage.clear();
  });

  it('should create and initialize an invalid empty form', () => {
    expect(component).toBeTruthy();
    expect(component.loginForm.valid).toBeFalse();
    expect(component.loginForm.get('email')?.value).toBe('');
    expect(component.loginForm.get('password')?.value).toBe('');
  });

  it('should validate email format and required fields', () => {
    const emailControl = component.loginForm.get('email');
    const passwordControl = component.loginForm.get('password');

    emailControl?.setValue('invalid-email');
    passwordControl?.setValue('');
    expect(emailControl?.valid).toBeFalse();
    expect(passwordControl?.valid).toBeFalse();

    emailControl?.setValue('webmaster@wbagency.com.br');
    passwordControl?.setValue('password123');
    expect(component.loginForm.valid).toBeTrue();
  });

  it('should call authService.login and navigate to returnUrl on success', () => {
    authServiceSpy.login.and.returnValue(
      of({
        token: 'test-token',
        user: { name: 'Admin', email: 'webmaster@wbagency.com.br', role: 'ADMIN' }
      })
    );

    component.loginForm.setValue({
      email: 'webmaster@wbagency.com.br',
      password: 'password123',
      rememberMe: false
    });

    component.onSubmit();

    expect(authServiceSpy.login).toHaveBeenCalledWith({
      email: 'webmaster@wbagency.com.br',
      password: 'password123'
    });
    expect(router.navigateByUrl).toHaveBeenCalledWith('/admin/candidatos');
  });

  it('should navigate to /admin/dashboard on login success when returnUrl is not provided', () => {
    // Reset route queryParams to empty
    (component as unknown as { route: { snapshot: { queryParams: Record<string, string> } } }).route = {
      snapshot: { queryParams: {} }
    };

    authServiceSpy.login.and.returnValue(
      of({
        token: 'test-token',
        user: { name: 'Admin', email: 'webmaster@wbagency.com.br', role: 'ADMIN' }
      })
    );

    component.loginForm.setValue({
      email: 'webmaster@wbagency.com.br',
      password: 'password123',
      rememberMe: false
    });

    component.onSubmit();

    expect(router.navigateByUrl).toHaveBeenCalledWith('/admin/dashboard');
  });

  it('should save email to localStorage when rememberMe is true', () => {
    localStorage.clear();
    authServiceSpy.login.and.returnValue(
      of({
        token: 'test-token',
        user: { name: 'Admin', email: 'webmaster@wbagency.com.br', role: 'ADMIN' }
      })
    );

    component.loginForm.setValue({
      email: 'remember@wbscouting.com',
      password: 'password123',
      rememberMe: true
    });

    component.onSubmit();

    expect(localStorage.getItem('wb_remember_email')).toBe('remember@wbscouting.com');
  });

  it('should show error message when login fails and not mock credentials', () => {
    authServiceSpy.login.and.returnValue(
      throwError(() => ({
        error: { detail: 'Credenciais inválidas. Verifique seu e-mail e senha de acesso.' }
      }))
    );

    component.loginForm.setValue({
      email: 'wrong@wbscouting.com',
      password: 'wrongpassword',
      rememberMe: false
    });

    component.onSubmit();

    expect(component.isLoading()).toBeFalse();
    expect(component.errorMessage()).toBe('Credenciais inválidas. Verifique seu e-mail e senha de acesso.');
  });

  it('should toggle password visibility signal', () => {
    expect(component.showPassword()).toBeFalse();
    component.togglePasswordVisibility();
    expect(component.showPassword()).toBeTrue();
    component.togglePasswordVisibility();
    expect(component.showPassword()).toBeFalse();
  });
});

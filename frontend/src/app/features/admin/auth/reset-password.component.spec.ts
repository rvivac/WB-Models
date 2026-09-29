import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ResetPasswordComponent } from './reset-password.component';
import { AuthService } from '../../../core/services/auth.service';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { of, throwError } from 'rxjs';

describe('ResetPasswordComponent', () => {
  let component: ResetPasswordComponent;
  let fixture: ComponentFixture<ResetPasswordComponent>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let activatedRouteStub: { snapshot: { queryParamMap: { get: jasmine.Spy } } };

  beforeEach(async () => {
    authServiceSpy = jasmine.createSpyObj('AuthService', ['resetPassword']);
    activatedRouteStub = {
      snapshot: {
        queryParamMap: {
          get: jasmine.createSpy('get').and.returnValue('valid-token-abc-123')
        }
      }
    };

    await TestBed.configureTestingModule({
      imports: [ResetPasswordComponent, RouterModule],
      providers: [
        { provide: AuthService, useValue: authServiceSpy },
        { provide: ActivatedRoute, useValue: activatedRouteStub }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ResetPasswordComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should initialize with token extracted from URL query params', () => {
    expect(component).toBeTruthy();
    expect(component.token).toBe('valid-token-abc-123');
    expect(component.tokenInvalid).toBeFalse();
    expect(component.isCompleted).toBeFalse();
  });

  it('should flag tokenInvalid as true if query param token is missing or empty', () => {
    activatedRouteStub.snapshot.queryParamMap.get.and.returnValue(null);
    const newFixture = TestBed.createComponent(ResetPasswordComponent);
    const newComponent = newFixture.componentInstance;
    newFixture.detectChanges();

    expect(newComponent.tokenInvalid).toBeTrue();
  });

  it('should validate password complexity indicators (min 8 chars, numbers & special chars)', () => {
    const passwordControl = component.resetForm.get('password');

    passwordControl?.setValue('abc');
    expect(component.hasMinLength).toBeFalse();
    expect(component.hasSpecialChar).toBeFalse();

    passwordControl?.setValue('Abcdefgh1!');
    expect(component.hasMinLength).toBeTrue();
    expect(component.hasSpecialChar).toBeTrue();
  });

  it('should flag mismatch error when password and confirmPassword do not match', () => {
    component.resetForm.get('password')?.setValue('NovaSenha@2026');
    component.resetForm.get('confirmPassword')?.setValue('OutraSenha@2026');

    expect(component.resetForm.errors?.['mismatch']).toBeTrue();
    expect(component.resetForm.valid).toBeFalse();

    component.resetForm.get('confirmPassword')?.setValue('NovaSenha@2026');
    expect(component.resetForm.errors).toBeNull();
    expect(component.resetForm.valid).toBeTrue();
  });

  it('should call authService.resetPassword and mark isCompleted as true on success', () => {
    authServiceSpy.resetPassword.and.returnValue(
      of({ message: 'Senha atualizada com sucesso.' })
    );

    component.resetForm.setValue({
      password: 'NovaSenha@2026!',
      confirmPassword: 'NovaSenha@2026!'
    });

    component.onSubmit();

    expect(authServiceSpy.resetPassword).toHaveBeenCalledWith({
      token: 'valid-token-abc-123',
      newPassword: 'NovaSenha@2026!',
      confirmPassword: 'NovaSenha@2026!'
    });
    expect(component.isLoading).toBeFalse();
    expect(component.isCompleted).toBeTrue();
  });

  it('should mark tokenInvalid as true when server rejects token', () => {
    authServiceSpy.resetPassword.and.returnValue(
      throwError(() => ({ status: 400, error: { message: 'Token inválido ou expirado.' } }))
    );

    component.resetForm.setValue({
      password: 'NovaSenha@2026!',
      confirmPassword: 'NovaSenha@2026!'
    });

    component.onSubmit();

    expect(component.isLoading).toBeFalse();
    expect(component.tokenInvalid).toBeTrue();
  });
});

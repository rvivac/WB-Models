import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ForgotPasswordComponent } from './forgot-password.component';
import { AuthService } from '../../../core/services/auth.service';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { of, throwError } from 'rxjs';

describe('ForgotPasswordComponent', () => {
  let component: ForgotPasswordComponent;
  let fixture: ComponentFixture<ForgotPasswordComponent>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;

  beforeEach(async () => {
    authServiceSpy = jasmine.createSpyObj('AuthService', ['requestPasswordReset', 'forgotPassword']);

    await TestBed.configureTestingModule({
      imports: [ForgotPasswordComponent, RouterModule],
      providers: [
        { provide: AuthService, useValue: authServiceSpy },
        { provide: ActivatedRoute, useValue: {} }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ForgotPasswordComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and initialize an invalid empty form', () => {
    expect(component).toBeTruthy();
    expect(component.forgotForm.valid).toBeFalse();
    expect(component.submittedSuccessfully).toBeFalse();
    expect(component.isLoading).toBeFalse();
  });

  it('should validate corporate email format', () => {
    const emailControl = component.forgotForm.get('email');

    emailControl?.setValue('invalid-email');
    expect(emailControl?.valid).toBeFalse();

    emailControl?.setValue('webmaster@wbagency.com.br');
    expect(emailControl?.valid).toBeTrue();
  });

  it('should call requestPasswordReset and display generic anti-enumeration success message', () => {
    authServiceSpy.requestPasswordReset.and.returnValue(
      of({ message: 'Se o e-mail informado estiver registrado em nossa base, as instruções de redefinição foram enviadas.' })
    );

    component.forgotForm.setValue({ email: 'webmaster@wbagency.com.br' });
    component.onSubmit();

    expect(authServiceSpy.requestPasswordReset).toHaveBeenCalledWith('webmaster@wbagency.com.br');
    expect(component.isLoading).toBeFalse();
    expect(component.submittedSuccessfully).toBeTrue();
  });

  it('should protect against user enumeration even if backend fails, showing generic success feedback', () => {
    authServiceSpy.requestPasswordReset.and.returnValue(
      throwError(() => new Error('Simulated network or backend error'))
    );

    component.forgotForm.setValue({ email: 'nonexistent@wbscouting.com' });
    component.onSubmit();

    expect(authServiceSpy.requestPasswordReset).toHaveBeenCalledWith('nonexistent@wbscouting.com');
    expect(component.isLoading).toBeFalse();
    expect(component.submittedSuccessfully).toBeTrue();
  });

  it('should not submit if form is invalid', () => {
    component.forgotForm.setValue({ email: '' });
    component.onSubmit();

    expect(authServiceSpy.requestPasswordReset).not.toHaveBeenCalled();
    expect(component.submittedSuccessfully).toBeFalse();
  });
});

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { AdminProfileComponent } from './admin-profile.component';
import { AdminProfileService } from '../../../core/services/admin-profile.service';
import { AuthService } from '../../../core/services/auth.service';

describe('AdminProfileComponent', () => {
  let component: AdminProfileComponent;
  let fixture: ComponentFixture<AdminProfileComponent>;
  let profileServiceSpy: jasmine.SpyObj<AdminProfileService>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;

  beforeEach(async () => {
    profileServiceSpy = jasmine.createSpyObj<AdminProfileService>('AdminProfileService', [
      'changePassword',
      'setup2fa',
      'confirm2fa',
      'disable2fa',
      'get2faStatus'
    ]);
    authServiceSpy = jasmine.createSpyObj<AuthService>('AuthService', ['currentUser']);

    authServiceSpy.currentUser.and.returnValue({
      id: 'uuid-admin',
      name: 'Webmaster Chefe',
      email: 'webmaster@wbagency.com.br',
      role: 'WEBMASTER',
      is2faEnabled: false
    });

    profileServiceSpy.get2faStatus.and.returnValue(of({ is2faEnabled: false }));

    await TestBed.configureTestingModule({
      imports: [AdminProfileComponent],
      providers: [
        { provide: AdminProfileService, useValue: profileServiceSpy },
        { provide: AuthService, useValue: authServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AdminProfileComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('deve inicializar com 2FA desativado', () => {
    expect(component).toBeTruthy();
    expect(component.is2faEnabled()).toBeFalse();
  });

  it('deve validar requisitos de complexidade da nova senha', () => {
    component.passwordForm.get('currentPassword')?.setValue('SenhaAtual@123');
    component.passwordForm.get('newPassword')?.setValue('short');
    component.passwordForm.get('confirmPassword')?.setValue('short');

    expect(component.hasMinLength()).toBeFalse();

    component.passwordForm.get('newPassword')?.setValue('ValidPassword123#');
    component.passwordForm.get('confirmPassword')?.setValue('ValidPassword123#');

    expect(component.hasMinLength()).toBeTrue();
    expect(component.hasUppercase()).toBeTrue();
    expect(component.hasLowercase()).toBeTrue();
    expect(component.hasNumber()).toBeTrue();
    expect(component.hasSpecialChar()).toBeTrue();
    expect(component.passwordsMatch()).toBeTrue();
  });

  it('deve alterar a senha com sucesso', () => {
    component.passwordForm.get('currentPassword')?.setValue('SenhaAtual@123');
    component.passwordForm.get('newPassword')?.setValue('NovaSenhaForte@456');
    component.passwordForm.get('confirmPassword')?.setValue('NovaSenhaForte@456');

    profileServiceSpy.changePassword.and.returnValue(of({ message: 'Senha alterada com sucesso.' }));

    component.submitPasswordChange();

    expect(profileServiceSpy.changePassword).toHaveBeenCalledWith({
      currentPassword: 'SenhaAtual@123',
      newPassword: 'NovaSenhaForte@456',
      confirmPassword: 'NovaSenhaForte@456'
    });
    expect(component.successMessage()).toBe('Senha alterada com sucesso.');
  });

  it('deve abrir modal de setup 2FA com QR Code', () => {
    profileServiceSpy.setup2fa.and.returnValue(
      of({
        secret: 'BASE32SECRETKEY',
        otpauthUrl: 'otpauth://totp/...',
        qrCodeDataUrl: 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA'
      })
    );

    component.start2faSetup();

    expect(profileServiceSpy.setup2fa).toHaveBeenCalled();
    expect(component.isSetup2faModalOpen()).toBeTrue();
    expect(component.setupData()?.secret).toBe('BASE32SECRETKEY');
  });

  it('deve confirmar 2FA e exibir códigos de backup descartáveis', () => {
    component.confirm2faForm.patchValue({ code: '123456' });
    profileServiceSpy.confirm2fa.and.returnValue(
      of({
        message: '2FA ativado com sucesso.',
        backupCodes: ['CODE-0001', 'CODE-0002', 'CODE-0003']
      })
    );

    component.submitConfirm2fa();

    expect(profileServiceSpy.confirm2fa).toHaveBeenCalledWith('123456');
    expect(component.is2faEnabled()).toBeTrue();
    expect(component.isBackupCodesModalOpen()).toBeTrue();
    expect(component.backupCodes().length).toBe(3);
  });
});

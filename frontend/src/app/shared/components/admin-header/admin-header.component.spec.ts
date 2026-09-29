import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AdminHeaderComponent } from './admin-header.component';
import { AuthService } from '../../../core/services/auth.service';
import { signal } from '@angular/core';

describe('AdminHeaderComponent', () => {
  let component: AdminHeaderComponent;
  let fixture: ComponentFixture<AdminHeaderComponent>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;

  beforeEach(async () => {
    authServiceSpy = jasmine.createSpyObj('AuthService', ['secureLogout'], {
      currentUser: signal({
        name: 'Administrador Teste',
        email: 'gestao@wbscouting.com',
        role: 'ADMIN'
      })
    });

    await TestBed.configureTestingModule({
      imports: [AdminHeaderComponent],
      providers: [{ provide: AuthService, useValue: authServiceSpy }]
    }).compileComponents();

    fixture = TestBed.createComponent(AdminHeaderComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and display the authenticated admin email', () => {
    expect(component).toBeTruthy();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('gestao@wbscouting.com');
    expect(compiled.textContent).toContain('Painel Administrativo');
    expect(compiled.textContent).toContain('Sair com Segurança');
  });

  it('should open confirmation modal when clicking logout button', () => {
    expect(component.showConfirmModal()).toBeFalse();
    component.confirmLogout();
    expect(component.showConfirmModal()).toBeTrue();

    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Encerrar Sessão');
    expect(compiled.textContent).toContain('Deseja realmente sair do painel?');
  });

  it('should dismiss modal when cancel is clicked', () => {
    component.confirmLogout();
    expect(component.showConfirmModal()).toBeTrue();

    component.cancelLogout();
    expect(component.showConfirmModal()).toBeFalse();
    expect(authServiceSpy.secureLogout).not.toHaveBeenCalled();
  });

  it('should trigger authService.secureLogout when confirmation is accepted', () => {
    component.confirmLogout();
    component.executeLogout();

    expect(component.showConfirmModal()).toBeFalse();
    expect(authServiceSpy.secureLogout).toHaveBeenCalledTimes(1);
  });
});

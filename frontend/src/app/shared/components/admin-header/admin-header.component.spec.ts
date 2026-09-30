import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
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
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: authServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AdminHeaderComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and display the authenticated admin email and editorial brand label', () => {
    expect(component).toBeTruthy();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('gestao@wbscouting.com');
    expect(compiled.textContent).toContain('Admin');
    expect(compiled.textContent).toContain('Triagem');
    expect(compiled.textContent).toContain('Scouting Desk');
    expect(compiled.textContent).toContain('Casting & Stars');
    expect(compiled.textContent).toContain('Institucional');
    expect(compiled.textContent).toContain('Sair');
  });

  it('should enforce 48px rigid height containment and brand-logo-header class on the brand logo img', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const logoImg = compiled.querySelector('.brand-logo-img') as HTMLImageElement;
    expect(logoImg).toBeTruthy();
    expect(logoImg.classList.contains('brand-logo-header')).toBeTrue();

    const logoAnchor = compiled.querySelector('.brand-logo') as HTMLAnchorElement;
    expect(logoAnchor.getAttribute('routerLink')).toBe('/admin/dashboard');

    // Inline style defensivo e dimensões intrínsecas
    expect(logoImg.style.height).toBe('48px');
    expect(logoImg.style.maxHeight).toBe('48px');
    expect(logoImg.getAttribute('width')).toBe('765');
    expect(logoImg.getAttribute('height')).toBe('507');
  });

  it('should open confirmation modal when onSecureLogout or logout button is triggered', () => {
    expect(component.showConfirmModal()).toBeFalse();
    component.onSecureLogout();
    expect(component.showConfirmModal()).toBeTrue();

    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Encerrar Sessão');
    expect(compiled.textContent).toContain('Deseja realmente sair do painel?');
  });

  it('should dismiss modal when cancelLogout is called', () => {
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

  it('should toggle and close mobile menu state', () => {
    expect(component.isMobileMenuOpen()).toBeFalse();
    component.toggleMobileMenu();
    expect(component.isMobileMenuOpen()).toBeTrue();
    component.closeMobileMenu();
    expect(component.isMobileMenuOpen()).toBeFalse();
  });

  it('should display pending candidates counter when greater than 0', () => {
    component.pendingCandidatesCount = 7;
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('7');
  });
});

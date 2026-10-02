import { TestBed } from '@angular/core/testing';
import { Router, ActivatedRouteSnapshot, RouterStateSnapshot, UrlTree } from '@angular/router';
import { roleGuard } from './role.guard';
import { AuthService } from '../services/auth.service';

describe('roleGuard', () => {
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(() => {
    authServiceSpy = jasmine.createSpyObj<AuthService>('AuthService', ['isAuthenticated', 'currentUser']);
    routerSpy = jasmine.createSpyObj<Router>('Router', ['createUrlTree']);

    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: authServiceSpy },
        { provide: Router, useValue: routerSpy }
      ]
    });
  });

  const runGuard = (roles?: string[]) => {
    const route = { data: roles ? { roles } : {} } as unknown as ActivatedRouteSnapshot;
    const state = { url: '/admin/usuarios' } as RouterStateSnapshot;
    return TestBed.runInInjectionContext(() => roleGuard(route, state));
  };

  it('deve permitir acesso para papel WEBMASTER', () => {
    authServiceSpy.isAuthenticated.and.returnValue(true);
    authServiceSpy.currentUser.and.returnValue({
      id: '1',
      name: 'Webmaster',
      email: 'webmaster@wbagency.com.br',
      role: 'WEBMASTER'
    });

    const result = runGuard();
    expect(result).toBeTrue();
  });

  it('deve permitir acesso para papel SUPER_ADMIN', () => {
    authServiceSpy.isAuthenticated.and.returnValue(true);
    authServiceSpy.currentUser.and.returnValue({
      id: '2',
      name: 'Super Admin',
      email: 'super@wbagency.com.br',
      role: 'SUPER_ADMIN'
    });

    const result = runGuard();
    expect(result).toBeTrue();
  });

  it('deve bloquear e redirecionar papel ADMIN comum', () => {
    const mockTree = {} as UrlTree;
    authServiceSpy.isAuthenticated.and.returnValue(true);
    authServiceSpy.currentUser.and.returnValue({
      id: '3',
      name: 'Admin Comum',
      email: 'admin@wbagency.com.br',
      role: 'ADMIN'
    });
    routerSpy.createUrlTree.and.returnValue(mockTree);

    const result = runGuard();
    expect(result).toBe(mockTree);
    expect(routerSpy.createUrlTree).toHaveBeenCalledWith(['/admin/dashboard']);
  });

  it('deve bloquear e redirecionar papel SCOUT', () => {
    const mockTree = {} as UrlTree;
    authServiceSpy.isAuthenticated.and.returnValue(true);
    authServiceSpy.currentUser.and.returnValue({
      id: '4',
      name: 'Scout',
      email: 'scout@wbagency.com.br',
      role: 'SCOUT'
    });
    routerSpy.createUrlTree.and.returnValue(mockTree);

    const result = runGuard();
    expect(result).toBe(mockTree);
    expect(routerSpy.createUrlTree).toHaveBeenCalledWith(['/admin/dashboard']);
  });
});

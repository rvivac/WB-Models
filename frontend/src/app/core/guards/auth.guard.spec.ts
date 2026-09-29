import { TestBed } from '@angular/core/testing';
import { Router, ActivatedRouteSnapshot, RouterStateSnapshot, UrlTree } from '@angular/router';
import { adminAuthGuard, authGuard } from './auth.guard';
import { AuthService } from '../services/auth.service';

describe('adminAuthGuard', () => {
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(() => {
    authServiceSpy = jasmine.createSpyObj('AuthService', ['isAuthenticated']);
    routerSpy = jasmine.createSpyObj('Router', ['createUrlTree']);

    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: authServiceSpy },
        { provide: Router, useValue: routerSpy }
      ]
    });
  });

  it('should allow access if user is authenticated', () => {
    authServiceSpy.isAuthenticated.and.returnValue(true);

    const route = {} as ActivatedRouteSnapshot;
    const state = { url: '/admin/candidatos' } as RouterStateSnapshot;

    const result = TestBed.runInInjectionContext(() => adminAuthGuard(route, state));

    expect(result).toBeTrue();
  });

  it('should redirect to /admin/login with returnUrl if unauthenticated', () => {
    authServiceSpy.isAuthenticated.and.returnValue(false);
    const mockUrlTree = {} as UrlTree;
    routerSpy.createUrlTree.and.returnValue(mockUrlTree);

    const route = {} as ActivatedRouteSnapshot;
    const state = { url: '/admin/candidatos' } as RouterStateSnapshot;

    const result = TestBed.runInInjectionContext(() => adminAuthGuard(route, state));

    expect(result).toBe(mockUrlTree);
    expect(routerSpy.createUrlTree).toHaveBeenCalledWith(['/admin/login'], {
      queryParams: { returnUrl: '/admin/candidatos' }
    });
  });

  it('should ensure authGuard is an alias of adminAuthGuard', () => {
    expect(authGuard).toBe(adminAuthGuard);
  });
});

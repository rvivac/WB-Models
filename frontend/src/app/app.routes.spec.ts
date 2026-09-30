import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { Location } from '@angular/common';
import { routes } from './app.routes';
import { AuthService } from './core/services/auth.service';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

describe('App Routes - Reconciliação /admin', () => {
  let router: Router;
  let location: Location;
  let authServiceSpy: jasmine.SpyObj<AuthService>;

  beforeEach(async () => {
    authServiceSpy = jasmine.createSpyObj('AuthService', ['isAuthenticated']);

    await TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter(routes),
        { provide: AuthService, useValue: authServiceSpy }
      ]
    }).compileComponents();

    router = TestBed.inject(Router);
    location = TestBed.inject(Location);
  });

  it('deve redirecionar de /admin para /admin/dashboard quando o usuário estiver autenticado', async () => {
    authServiceSpy.isAuthenticated.and.returnValue(true);

    await router.navigateByUrl('/admin');
    expect(location.path()).toBe('/admin/dashboard');
  });

  it('deve redirecionar de /admin para /admin/login com returnUrl quando o usuário não estiver autenticado', async () => {
    authServiceSpy.isAuthenticated.and.returnValue(false);

    await router.navigateByUrl('/admin');
    expect(location.path()).toContain('/admin/login');
    expect(location.path()).toContain('returnUrl=%2Fadmin');
  });

  it('deve redirecionar rotas legadas e atalhos como /seja-modelo para /apply', async () => {
    await router.navigateByUrl('/seja-modelo');
    expect(location.path()).toBe('/apply');
  });

  it('deve redirecionar /become-model para /apply', async () => {
    await router.navigateByUrl('/become-model');
    expect(location.path()).toBe('/apply');
  });

  it('deve redirecionar /admin/modelos para /admin/models quando autenticado', async () => {
    authServiceSpy.isAuthenticated.and.returnValue(true);

    await router.navigateByUrl('/admin/modelos');
    expect(location.path()).toBe('/admin/models');
  });
});

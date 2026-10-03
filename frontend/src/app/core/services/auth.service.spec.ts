import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { AuthService } from './auth.service';
import { environment } from '../../../environments/environment';

describe('AuthService', () => {
  let service: AuthService;
  let httpTesting: HttpTestingController;
  let mockRouter: jasmine.SpyObj<Router>;

  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
    mockRouter = jasmine.createSpyObj('Router', ['navigate', 'navigateByUrl']);
    mockRouter.navigate.and.returnValue(Promise.resolve(true));

    TestBed.configureTestingModule({
      providers: [
        AuthService,
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: Router, useValue: mockRouter }
      ]
    });

    service = TestBed.inject(AuthService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
    localStorage.clear();
    sessionStorage.clear();
  });

  it('should be created and start unauthenticated if storage is empty', () => {
    expect(service).toBeTruthy();
    expect(service.isAuthenticated()).toBeFalse();
    expect(service.currentUser()).toBeNull();
    expect(service.getToken()).toBeNull();
  });

  it('should store token and user on successful login', () => {
    const mockExp = Math.floor(Date.now() / 1000) + 3600;
    const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
    const payload = btoa(JSON.stringify({ exp: mockExp }));
    const testJwt = `${header}.${payload}.sig`;

    const credentials = { email: 'webmaster@wbagency.com.br', password: 'secretpassword' };
    const mockResponse = {
      token: testJwt,
      refreshToken: 'refresh-123',
      user: {
        id: 'admin-1',
        name: 'Carlos Booker',
        email: 'webmaster@wbagency.com.br',
        role: 'WEBMASTER'
      }
    };

    service.login(credentials).subscribe((res) => {
      expect(res.token).toBe(testJwt);
      expect(service.getToken()).toBe(testJwt);
      expect(service.getRefreshToken()).toBe('refresh-123');
      expect(service.currentUser()?.name).toBe('Carlos Booker');
      expect(service.isAuthenticated()).toBeTrue();
    });

    const req = httpTesting.expectOne(`${environment.apiUrl}/auth/login`);
    expect(req.request.method).toBe('POST');
    req.flush(mockResponse);
  });

  it('should handle mock fallback for local testing with webmaster credentials when backend is offline', () => {
    const credentials = { email: 'webmaster@wbagency.com.br', password: 'admin123' };

    service.login(credentials).subscribe((res) => {
      expect(res).toBeTruthy();
      expect(service.isAuthenticated()).toBeTrue();
      expect(service.currentUser()?.email).toBe('webmaster@wbagency.com.br');
      expect(service.currentUser()?.role).toBe('WEBMASTER');
    });

    const req = httpTesting.expectOne(`${environment.apiUrl}/auth/login`);
    req.flush('Network Error', { status: 0, statusText: 'Unknown Error' });
  });

  it('should reject legacy admin@wbscouting.com in offline mock fallback', () => {
    const credentials = { email: 'admin@wbscouting.com', password: 'admin123' };
    let errorThrown = false;

    service.login(credentials).subscribe({
      next: () => fail('Should not authenticate legacy email'),
      error: (err) => {
        errorThrown = true;
        expect(err.status).toBe(0);
      }
    });

    const req = httpTesting.expectOne(`${environment.apiUrl}/auth/login`);
    req.flush('Network Error', { status: 0, statusText: 'Unknown Error' });
    expect(errorThrown).toBeTrue();
  });

  it('should execute secureLogout by notifying backend, purging storage, and replacing url', () => {
    localStorage.setItem('wb_auth_token', 'sample-token');
    localStorage.setItem('wb_auth_user', JSON.stringify({ name: 'Admin', email: 'admin@wb.com', role: 'ADMIN' }));
    sessionStorage.setItem('wb_session_key', 'session-data');

    service.secureLogout();

    const req = httpTesting.expectOne(`${environment.apiUrl}/auth/logout`);
    expect(req.request.method).toBe('POST');
    req.flush(null, { status: 204, statusText: 'No Content' });

    expect(service.getToken()).toBeNull();
    expect(service.currentUser()).toBeNull();
    expect(localStorage.getItem('wb_auth_token')).toBeNull();
    expect(sessionStorage.getItem('wb_session_key')).toBeNull();
    expect(mockRouter.navigate).toHaveBeenCalledWith(['/admin/login'], { replaceUrl: true });
  });

  it('should execute secureLogout immediately when no token is present', () => {
    localStorage.setItem('some_stale_key', 'val');
    service.secureLogout();

    httpTesting.expectNone(`${environment.apiUrl}/auth/logout`);
    expect(service.getToken()).toBeNull();
    expect(service.currentUser()).toBeNull();
    expect(localStorage.getItem('some_stale_key')).toBeNull();
    expect(mockRouter.navigate).toHaveBeenCalledWith(['/admin/login'], { replaceUrl: true });
  });

  it('should correctly detect expired JWT tokens', () => {
    const expiredExp = Math.floor(Date.now() / 1000) - 100;
    const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
    const payload = btoa(JSON.stringify({ exp: expiredExp }));
    const expiredJwt = `${header}.${payload}.sig`;

    expect(service.isTokenExpired(expiredJwt)).toBeTrue();
  });
});

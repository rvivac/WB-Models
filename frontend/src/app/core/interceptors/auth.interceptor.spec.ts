import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from '../services/auth.service';

describe('authInterceptor', () => {
  let http: HttpClient;
  let httpTesting: HttpTestingController;
  let authServiceSpy: jasmine.SpyObj<AuthService>;

  beforeEach(() => {
    authServiceSpy = jasmine.createSpyObj('AuthService', ['getToken', 'logout']);

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: authServiceSpy }
      ]
    });

    http = TestBed.inject(HttpClient);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('should attach Bearer token to /api/v1/admin requests when token exists', () => {
    authServiceSpy.getToken.and.returnValue('my-jwt-token');

    http.get('/api/v1/admin/candidates').subscribe();

    const req = httpTesting.expectOne('/api/v1/admin/candidates');
    expect(req.request.headers.has('Authorization')).toBeTrue();
    expect(req.request.headers.get('Authorization')).toBe('Bearer my-jwt-token');
    req.flush({});
  });

  it('should not attach Authorization header to public requests', () => {
    authServiceSpy.getToken.and.returnValue('my-jwt-token');

    http.get('/api/v1/public/models').subscribe();

    const req = httpTesting.expectOne('/api/v1/public/models');
    expect(req.request.headers.has('Authorization')).toBeFalse();
    req.flush({});
  });

  it('should call authService.logout on 401 response for protected admin requests', () => {
    authServiceSpy.getToken.and.returnValue('my-jwt-token');

    http.get('/api/v1/admin/candidates').subscribe({
      next: () => fail('Should have failed with 401'),
      error: (err) => {
        expect(err.status).toBe(401);
        expect(authServiceSpy.logout).toHaveBeenCalled();
      }
    });

    const req = httpTesting.expectOne('/api/v1/admin/candidates');
    req.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });
  });

  it('should NOT call logout on 401 if request is login endpoint', () => {
    authServiceSpy.getToken.and.returnValue(null);

    http.post('/api/v1/auth/login', {}).subscribe({
      next: () => fail('Should have failed'),
      error: (err) => {
        expect(err.status).toBe(401);
        expect(authServiceSpy.logout).not.toHaveBeenCalled();
      }
    });

    const req = httpTesting.expectOne('/api/v1/auth/login');
    req.flush('Invalid credentials', { status: 401, statusText: 'Unauthorized' });
  });

  it('should NOT call logout on 403 Forbidden response', () => {
    authServiceSpy.getToken.and.returnValue('my-jwt-token');

    http.get('/api/v1/admin/users').subscribe({
      next: () => fail('Should have failed with 403'),
      error: (err) => {
        expect(err.status).toBe(403);
        expect(authServiceSpy.logout).not.toHaveBeenCalled();
      }
    });

    const req = httpTesting.expectOne('/api/v1/admin/users');
    req.flush('Forbidden', { status: 403, statusText: 'Forbidden' });
  });
});

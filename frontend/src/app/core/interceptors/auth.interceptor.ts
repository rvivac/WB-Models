import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { catchError, throwError } from 'rxjs';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const token = authService.getToken();

  let authReq = req;

  // Anexa o cabeçalho Authorization: Bearer <token> em rotas administrativas protegidas
  const isAdminRequest =
    req.url.includes('/api/v1/admin') ||
    req.url.includes('/admin/') ||
    req.url.includes('/api/v1/candidates/admin') ||
    req.url.includes('/auth/logout');

  if (token && isAdminRequest) {
    authReq = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
  }

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      // Ignora rotas de autenticação para evitar loops infinitos de logout
      const isAuthUrl =
        req.url.includes('/auth/login') ||
        req.url.includes('/auth/logout') ||
        req.url.includes('/auth/forgot-password') ||
        req.url.includes('/auth/reset-password');

      const isAlreadyOnLoginPage =
        typeof window !== 'undefined' &&
        (window.location.pathname.includes('/login') || window.location.pathname.includes('/admin/login'));

      const isCurrentRouteAdmin =
        typeof window !== 'undefined' &&
        window.location.pathname.startsWith('/admin');

      // CRÍTICO: Redireciona e limpa a sessão em 401 (não autenticado) SOMENTE se:
      // 1. A requisição for comprovadamente administrativa (isAdminRequest); OU
      // 2. O usuário estiver navegando dentro da área administrativa (isCurrentRouteAdmin).
      // NUNCA redirecionar um visitante público na Home ('/') ou rotas institucionais para o login!
      if (error.status === 401 && !isAuthUrl && !isAlreadyOnLoginPage && (isAdminRequest || isCurrentRouteAdmin)) {
        authService.logout();
      }
      return throwError(() => error);
    })
  );
};

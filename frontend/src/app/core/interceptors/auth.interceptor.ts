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
      // Redireciona e limpa a sessão em 401/403, preservando respostas de falha na tela de login
      if ((error.status === 401 || error.status === 403) && !req.url.includes('/auth/login')) {
        authService.logout();
      }
      return throwError(() => error);
    })
  );
};

import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

/**
 * Route Guard estrito para o perfil WEBMASTER.
 * Impede acesso de perfis operacionais e administradores comuns (ADMIN),
 * redirecionando com segurança para o dashboard administrativo.
 */
export const webmasterOnlyGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (!authService.isAuthenticated()) {
    return router.createUrlTree(['/admin/login'], {
      queryParams: { returnUrl: state.url }
    });
  }

  const currentUser = authService.currentUser();
  const userRole = (currentUser?.role || '').toUpperCase();

  if (userRole === 'WEBMASTER' || userRole === 'SUPER_ADMIN') {
    return true;
  }

  return router.createUrlTree(['/admin/dashboard']);
};

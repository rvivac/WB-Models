import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

/**
 * Route Guard funcional baseado em papéis (RBAC).
 * Protege rotas críticas exclusivas para WEBMASTER e SUPER_ADMIN,
 * redirecionando usuários sem privilégio para o dashboard ou login.
 */
export const roleGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (!authService.isAuthenticated()) {
    return router.createUrlTree(['/admin/login'], {
      queryParams: { returnUrl: state.url }
    });
  }

  const currentUser = authService.currentUser();
  const allowedRoles: string[] = route.data?.['roles'] || ['WEBMASTER', 'SUPER_ADMIN'];
  const userRole = (currentUser?.role || '').toUpperCase();

  if (allowedRoles.includes(userRole)) {
    return true;
  }

  // Redireciona usuários sem o papel necessário para o dashboard com feedback
  return router.createUrlTree(['/admin/dashboard']);
};

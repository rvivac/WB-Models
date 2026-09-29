import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

/**
 * Route guard funcional para proteção de rotas administrativas do WB Agency.
 * Garante que apenas usuários com token JWT válido acessem o backoffice,
 * preservando o parâmetro returnUrl para redirecionamento pós-login.
 */
export const adminAuthGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isAuthenticated()) {
    return true;
  }

  // Redireciona para login mantendo a URL de destino na query string
  return router.createUrlTree(['/admin/login'], {
    queryParams: { returnUrl: state.url }
  });
};

/**
 * Alias de retrocompatibilidade para rotas existentes
 */
export const authGuard: CanActivateFn = adminAuthGuard;

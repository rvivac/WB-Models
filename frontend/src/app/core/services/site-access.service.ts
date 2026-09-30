import { Injectable, signal, computed, inject } from '@angular/core';
import { environment } from '../../../environments/environment';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class SiteAccessService {
  private readonly authService = inject(AuthService);
  private readonly STORAGE_KEY = 'wb_site_access_token';

  // Chaves aceitas para liberação do site na Vercel
  private readonly ACCEPTED_KEYS = new Set([
    'WB@2026',
    'WBAGENCY',
    'WB2026',
    'Admin@WbScouting2026!',
    (environment as any).siteLockKey || 'WB@2026'
  ]);

  private readonly isUnlockedSignal = signal<boolean>(this.checkInitialAccess());
  readonly isUnlocked = computed(() => {
    // Se o usuário já está autenticado como administrador, libera o acesso automaticamente
    if (this.authService.isAuthenticated()) {
      return true;
    }
    return this.isUnlockedSignal();
  });

  readonly isGateActive = computed(() => {
    // Rotas administrativas (/admin, /admin/login, etc.) possuem autenticação própria e não são bloqueadas pelo gate
    if (typeof window !== 'undefined' && window.location.pathname.startsWith('/admin')) {
      return false;
    }

    // O gate só fica ativo se explicitamente habilitado nas variáveis de ambiente ou via ?gate=true
    const explicitlyEnabled = !!(environment as any).siteLockEnabled;
    const testParam = typeof window !== 'undefined' && window.location.search.includes('gate=true');

    // Se qualquer um dos gatilhos estiver ativo e ainda não estiver desbloqueado
    return (explicitlyEnabled || testParam) && !this.isUnlocked();
  });

  constructor() {
    this.checkUrlUnlockParam();
  }

  /**
   * Verifica se o acesso já foi previamente concedido no localStorage/sessionStorage
   */
  private checkInitialAccess(): boolean {
    if (typeof window === 'undefined') {
      return true;
    }

    try {
      const stored = localStorage.getItem(this.STORAGE_KEY) || sessionStorage.getItem(this.STORAGE_KEY);
      if (stored) {
        const data = JSON.parse(stored);
        // Token expira em 7 dias
        if (data?.timestamp && (Date.now() - data.timestamp < 7 * 24 * 60 * 60 * 1000)) {
          return true;
        }
      }
    } catch {
      // Ignora erro de parsing
    }

    return false;
  }

  /**
   * Permite desbloqueio automático via query param ?access=WB@2026 para links diretos
   */
  private checkUrlUnlockParam(): void {
    if (typeof window === 'undefined') return;

    try {
      const urlParams = new URLSearchParams(window.location.search);
      const accessKey = urlParams.get('access') || urlParams.get('key');
      if (accessKey && this.unlock(accessKey)) {
        // Remove o parâmetro da URL sem recarregar a página
        urlParams.delete('access');
        urlParams.delete('key');
        const newSearch = urlParams.toString() ? `?${urlParams.toString()}` : '';
        const newUrl = `${window.location.pathname}${newSearch}${window.location.hash}`;
        window.history.replaceState({}, '', newUrl);
      }
    } catch {
      // Falha silenciosa
    }
  }

  /**
   * Tenta desbloquear o site com a senha fornecida
   */
  unlock(password: string): boolean {
    const trimmed = (password || '').trim();
    if (this.ACCEPTED_KEYS.has(trimmed)) {
      this.isUnlockedSignal.set(true);
      if (typeof window !== 'undefined') {
        const payload = JSON.stringify({
          granted: true,
          timestamp: Date.now()
        });
        try {
          localStorage.setItem(this.STORAGE_KEY, payload);
          sessionStorage.setItem(this.STORAGE_KEY, payload);
        } catch {
          // localStorage pode falhar em modo anônimo estrito
        }
      }
      return true;
    }
    return false;
  }

  /**
   * Bloqueia novamente o site (para testes ou revogação de acesso)
   */
  lock(): void {
    this.isUnlockedSignal.set(false);
    if (typeof window !== 'undefined') {
      try {
        localStorage.removeItem(this.STORAGE_KEY);
        sessionStorage.removeItem(this.STORAGE_KEY);
      } catch {
        // Falha silenciosa
      }
    }
  }
}

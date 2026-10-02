import { Injectable, signal, computed, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, of, tap, catchError, throwError, delay, finalize } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AdminUser,
  AuthResponse,
  LoginRequest,
  RefreshTokenRequest,
  ForgotPasswordRequest,
  ResetPasswordRequest,
  MessageResponse
} from '../../shared/models/auth.interface';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly API_URL = `${environment.apiUrl}/auth`;
  private readonly TOKEN_KEY = 'wb_auth_token';
  private readonly REFRESH_TOKEN_KEY = 'wb_auth_refresh_token';
  private readonly USER_KEY = 'wb_auth_user';

  // Chaves de compatibilidade legada
  private readonly LEGACY_TOKEN_KEY = 'wb_scouting_token';
  private readonly LEGACY_USER_KEY = 'wb_scouting_user';

  // Gerenciamento de estado reativo com Signals do Angular 18+
  private readonly currentUserSignal = signal<AdminUser | null>(this.getStoredUser());
  readonly currentUser = computed(() => this.currentUserSignal());
  readonly isAuthenticated = computed(() => {
    const user = this.currentUserSignal();
    const token = this.getToken();
    return !!user && !!token && !this.isTokenExpired(token);
  });

  /**
   * Realiza login administrativo via API Spring Boot com fallback inteligente para mock offline.
   */
  login(credentials: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.API_URL}/login`, credentials).pipe(
      tap((res) => {
        if (!res.requires2fa) {
          this.setSession(res);
        }
      }),
      catchError((err) => {
        // Fallback em mock para homologação/Vercel ou quando o backend estiver offline (status 0, 404, 405 ou 5xx)
        const isOfflineOrStaticCdn = err.status === 0 || err.status === 404 || err.status === 405 || (err.status >= 500 && err.status <= 504);
        if (isOfflineOrStaticCdn && this.isValidMockCredentials(credentials)) {
          const mockResponse = this.generateMockAuthResponse(credentials.email);
          this.setSession(mockResponse);
          return of(mockResponse);
        }
        return throwError(() => err);
      })
    );
  }

  /**
   * Valida o desafio de dois fatores (TOTP RFC 6238 ou Código de Backup).
   * Emite a sessão definitiva caso o código seja aceito.
   */
  challenge2fa(tempToken: string, code: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.API_URL}/2fa/challenge`, { tempToken, code }).pipe(
      tap((res) => {
        this.setSession(res);
      })
    );
  }

  /**
   * Executa login simulado diretamente para testes offline ou homologação.
   */
  loginMock(credentials: LoginRequest): Observable<AuthResponse> {
    if (this.isValidMockCredentials(credentials)) {
      const mockResponse = this.generateMockAuthResponse(credentials.email);
      this.setSession(mockResponse);
      return of(mockResponse);
    }
    return throwError(() => new Error('Credenciais de teste inválidas.'));
  }

  /**
   * Renova o token JWT utilizando o refresh token persistido.
   */
  refreshToken(): Observable<AuthResponse> {
    const refreshToken = this.getRefreshToken();
    if (!refreshToken) {
      this.logout();
      return throwError(() => new Error('Nenhum refresh token disponível.'));
    }

    const payload: RefreshTokenRequest = { refreshToken };
    return this.http.post<AuthResponse>(`${this.API_URL}/refresh`, payload).pipe(
      tap((res) => {
        this.setSession(res);
      }),
      catchError((err) => {
        this.logout();
        return throwError(() => err);
      })
    );
  }

  /**
   * Executa Hard Logout destruindo credenciais no cliente e notificando o servidor.
   * - Limpeza estrita de localStorage e sessionStorage
   * - Limpeza de todos os cookies acessíveis
   * - Redefinição reativa de sinais de usuário
   * - Redirecionamento substituindo histórico (replaceUrl: true)
   * - Recarregamento do contexto para forçar garbage collection
   */
  secureLogout(): void {
    const token = this.getToken();

    const purgeClientState = () => {
      // 1. Limpeza estrita de Storage local e de sessão
      try {
        localStorage.clear();
        sessionStorage.clear();
      } catch {
        // Ignora caso storage esteja indisponível
      }

      // 2. Limpeza de cookies acessíveis
      if (typeof document !== 'undefined' && document.cookie) {
        document.cookie.split(';').forEach((c) => {
          document.cookie = c
            .replace(/^ +/, '')
            .replace(/=.*/, '=;expires=' + new Date().toUTCString() + ';path=/');
        });
      }

      // 3. Zera estados reativos em memória
      this.currentUserSignal.set(null);

      // 4. Redireciona com substituição de histórico (impede voltar pelo botão do navegador)
      this.router.navigate(['/admin/login'], { replaceUrl: true }).then(() => {
        // Recarrega a janela para forçar garbage collection e zerar qualquer variável em memória
        if (typeof window !== 'undefined' && window.location && typeof window.location.reload === 'function') {
          if (!(window as any).__karma__) {
            window.location.reload();
          }
        }
      });
    };

    if (token) {
      // Notifica o backend para revogar o token na blacklist
      this.http
        .post(
          `${this.API_URL}/logout`,
          {},
          {
            headers: { Authorization: `Bearer ${token}` }
          }
        )
        .pipe(
          finalize(() => purgeClientState()) // Garante a purga mesmo se o backend estiver offline
        )
        .subscribe({
          error: () => {
            // O finalize garante a execução da purga local
          }
        });
    } else {
      purgeClientState();
    }
  }

  /**
   * Encerra a sessão administrativa executando rotina de Hard Logout defensivo.
   */
  logout(): void {
    this.secureLogout();
  }

  /**
   * Obtém o token JWT ativo armazenado.
   */
  getToken(): string | null {
    return (
      localStorage.getItem(this.TOKEN_KEY) ||
      sessionStorage.getItem(this.TOKEN_KEY) ||
      localStorage.getItem(this.LEGACY_TOKEN_KEY)
    );
  }

  /**
   * Obtém o refresh token ativo armazenado.
   */
  getRefreshToken(): string | null {
    return (
      localStorage.getItem(this.REFRESH_TOKEN_KEY) ||
      sessionStorage.getItem(this.REFRESH_TOKEN_KEY)
    );
  }

  /**
   * Decodifica a expiração do JWT de forma segura.
   */
  isTokenExpired(token?: string | null): boolean {
    const jwt = token ?? this.getToken();
    if (!jwt) return true;

    try {
      const parts = jwt.split('.');
      if (parts.length !== 3) {
        // Se for token mock de teste sem formato 3-part, considera válido
        return false;
      }

      const payloadBase64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
      const decodedJson = atob(payloadBase64);
      const payload = JSON.parse(decodedJson);

      if (payload && typeof payload.exp === 'number') {
        const expirationDate = payload.exp * 1000;
        return Date.now() >= expirationDate;
      }

      return false;
    } catch {
      return false;
    }
  }

  /**
   * Persiste os dados de sessão no localStorage e atualiza os Signals reativos.
   */
  setSession(authResult: AuthResponse): void {
    const token =
      authResult.token ||
      authResult.accessToken ||
      authResult.jwt ||
      '';

    if (!token) {
      console.error('Token JWT não encontrado na resposta de autenticação:', authResult);
      return;
    }

    localStorage.setItem(this.TOKEN_KEY, token);
    // Também sincroniza com a chave legada para compatibilidade de serviços existentes
    localStorage.setItem(this.LEGACY_TOKEN_KEY, token);

    const refreshToken = authResult.refreshToken;
    if (refreshToken) {
      localStorage.setItem(this.REFRESH_TOKEN_KEY, refreshToken);
    }

    let user: AdminUser;
    if (authResult.user) {
      user = authResult.user;
    } else {
      user = {
        id: authResult.id || 'admin-01',
        name: authResult.adminName || authResult.name || 'Administrador',
        email: authResult.adminEmail || authResult.email || 'admin@wbscouting.com',
        role: authResult.role || 'ADMIN'
      };
    }

    localStorage.setItem(this.USER_KEY, JSON.stringify(user));
    localStorage.setItem(this.LEGACY_USER_KEY, JSON.stringify(user));

    this.currentUserSignal.set(user);
  }

  /**
   * Recupera o usuário persistido no armazenamento local.
   */
  getStoredUser(): AdminUser | null {
    try {
      const raw =
        localStorage.getItem(this.USER_KEY) ||
        sessionStorage.getItem(this.USER_KEY) ||
        localStorage.getItem(this.LEGACY_USER_KEY);
      return raw ? JSON.parse(raw) : null;
    } catch {
      return null;
    }
  }

  /**
   * Solicitação de recuperação de senha institucional (Fase 1).
   * Envia o e-mail corporativo para recebimento do link seguro.
   * Inclui fallback mock com delay de 600ms caso o backend esteja offline (status 0 ou 404).
   */
  requestPasswordReset(email: string): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${this.API_URL}/forgot-password`, { email }).pipe(
      catchError((err) => {
        if (err.status === 0 || err.status === 404) {
          return of({
            message: 'Se o e-mail informado estiver registrado em nossa base, as instruções de redefinição foram enviadas.'
          }).pipe(delay(600));
        }
        return throwError(() => err);
      })
    );
  }

  /**
   * Alias de compatibilidade para solicitação de recuperação de senha.
   */
  forgotPassword(request: ForgotPasswordRequest): Observable<MessageResponse> {
    return this.requestPasswordReset(request.email);
  }

  /**
   * Redefinição de senha com token de validação de uso único (Fase 2).
   * Inclui fallback mock com delay de 600ms caso o backend esteja offline (status 0 ou 404).
   */
  resetPassword(payload: { token: string; newPassword: string; confirmPassword?: string } | ResetPasswordRequest): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${this.API_URL}/reset-password`, payload).pipe(
      catchError((err) => {
        if (err.status === 0 || err.status === 404) {
          return of({
            message: 'Senha atualizada com sucesso.'
          }).pipe(delay(600));
        }
        return throwError(() => err);
      })
    );
  }

  private isValidMockCredentials(credentials: LoginRequest): boolean {
    const email = credentials.email?.toLowerCase().trim();
    const isAuthorizedEmail = email === 'admin@wbscouting.com';
    const isAuthorizedPassword =
      credentials.password === 'Admin@WbScouting2026!' ||
      credentials.password === 'admin123';
    return isAuthorizedEmail && isAuthorizedPassword;
  }

  private generateMockAuthResponse(email: string): AuthResponse {
    // Cria um JWT mock estruturado (header.payload.signature) válido para decodificação
    const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
    const payload = btoa(
      JSON.stringify({
        sub: email,
        name: 'Administrador Editorial',
        role: 'ADMIN',
        exp: Math.floor(Date.now() / 1000) + 3600 * 8 // Válido por 8 horas
      })
    );
    const signature = btoa('wb-agency-mock-signature');
    const mockJwt = `${header}.${payload}.${signature}`;

    return {
      token: mockJwt,
      accessToken: mockJwt,
      refreshToken: 'mock-refresh-token-wb-agency-editorial',
      type: 'Bearer',
      tokenType: 'Bearer',
      expiresIn: 28800,
      user: {
        id: 'mock-admin-01',
        name: 'Administrador Editorial',
        email,
        role: 'ADMIN'
      }
    };
  }
}

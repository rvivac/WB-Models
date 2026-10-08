import { Component, inject, computed, OnInit, signal, effect } from '@angular/core';
import { Router, NavigationEnd, RouterModule } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { filter, map } from 'rxjs/operators';
import { CommonModule } from '@angular/common';
import { HeaderComponent } from './shared/components/header/header.component';
import { FooterComponent } from './shared/components/footer/footer.component';
import { AdminHeaderComponent } from './shared/components/admin-header/admin-header.component';
import { AuthService } from './core/services/auth.service';
import { APP_VERSION } from '../environments/version';

// TODO depois: criar o arquivo abaixo e entao descomentar os imports:
// import { AdminVersionBadgeComponent } from './shared/components/admin-version-badge/admin-version-badge.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterModule, HeaderComponent, FooterComponent, AdminHeaderComponent],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss'],
  styles: [`
    /* ============= BADGE DISCRETO DE COMMIT (Soh aparece em /admin) ============= */
    .wb-admin-version {
      position: fixed; right: 10px; bottom: 10px; z-index: 50;
      display: inline-flex; align-items: center; gap: 6px;
      padding: 2px 10px 2px 8px;
      border-radius: 999px;
      background: rgba(17,24,39,0.92); color:#d1d5db;
      font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
      font-size: 10.5px; line-height: 18px; letter-spacing: 0.2px;
      opacity: 0.08; transition: all 220ms ease; cursor: pointer;
      user-select: none; -webkit-user-select: none;
      border: 1px solid rgba(107,114,128,0.35);
      backdrop-filter: blur(4px); -webkit-backdrop-filter: blur(4px);
    }
    .wb-admin-version:hover, .wb-admin-version:focus-within { opacity: 1; transform: translateY(-1px); box-shadow: 0 6px 18px rgba(15,23,42,.45); outline: none; }
    .wb-admin-version code { color:#f3f4f6; font-size:10.5px; font-weight:600; background:none; padding:0; border:0; }
    .dot { width:8px; height:8px; border-radius:50%; display:inline-block; box-shadow:0 0 0 2px rgba(255,255,255,0.06); }
    .dot.dev { background:#f59e0b; animation: pulseWarn 3s infinite; }
    .dot.prod{ background:#10b981; animation: pulseOk 5s infinite; }
    @keyframes pulseOk   { 0%,100%{opacity:1} 50%{opacity:.45}}
    @keyframes pulseWarn { 0%,100%{opacity:1} 50%{opacity:.45}}

    .wb-admin-version-toast {
      position: fixed; right: 10px; bottom: 44px; z-index: 51;
      min-width: 260px; max-width: 360px; padding: 12px 14px; border-radius: 10px;
      background: rgba(17,24,39,0.98); color: #e5e7eb;
      font-size: 11.5px; line-height: 1.55;
      border: 1px solid rgba(75,85,99,0.6);
      box-shadow: 0 10px 30px rgba(15,23,42,0.5);
      animation: slideIn 180ms ease-out;
    }
    .wb-admin-version-toast code { background:rgba(75,85,99,0.35); padding: 1px 6px; border-radius: 5px; font-size: 11px; }
    .wb-admin-version-toast .row { display:flex; justify-content:space-between; gap:10px; margin: 2px 0; }
    @keyframes slideIn { from { transform: translateY(6px); opacity:0 } to { transform: none; opacity:1 } }
    @media (max-width: 640px) {
      .wb-admin-version { right:6px; bottom:6px; font-size:9.5px; padding: 2px 8px; opacity:.06;}
      .wb-admin-version-toast { right:6px; bottom:38px; min-width: 220px; font-size: 11px;}
    }
    @media print { .wb-admin-version, .wb-admin-version-toast { display:none !important; } }
  `]
})
export class AppComponent implements OnInit {
  private readonly router = inject(Router);
  readonly authService = inject(AuthService);

  /* ====== Badge do Commit Webmaster (discreto canto inf. direito) ====== */
  readonly vcommitFull  = signal<string>(APP_VERSION.commit);
  readonly vcommitShort = computed(() => this.vcommitFull().substring(0, 7));
  readonly vbranch      = signal<string>(APP_VERSION.branch);
  readonly vbuildDate   = signal<string>(APP_VERSION.buildDate);
  readonly vbuildPretty = computed(() => {
    try { return new Date(this.vbuildDate()).toLocaleString('pt-BR', { dateStyle:'short', timeStyle:'short', timeZone:'America/Sao_Paulo'}); }
    catch { return this.vbuildDate(); }
  });
  readonly vIsProd = computed(() => !this.vcommitFull().includes('dev') && typeof window !== 'undefined' && window.location.hostname !== 'localhost');
  readonly vShowToast = signal<boolean>(false);
  readonly vCopied    = signal<boolean>(false);
  readonly vTooltip   = computed(() => {
    const lines = [ 'Build: ' + this.vbuildPretty(), 'Branch: ' + this.vbranch(), 'Commit: ' + this.vcommitFull(), 'Dobre clique p/ copiar commit' ];
    return lines.join(String.fromCharCode(10));
  });

  _wb_toggleCommitToast() {
    const v = !this.vShowToast();
    this.vShowToast.set(v);
    if (v) setTimeout(() => this.vShowToast.set(false), 5500);
  }
  _wb_copyCommit() {
    const commit = this.vcommitFull();
    const done = () => { this.vCopied.set(true); this.vShowToast.set(true); setTimeout(()=>{ this.vCopied.set(false); this.vShowToast.set(false); }, 3500); };
    if (typeof navigator !== 'undefined' && navigator?.clipboard?.writeText) {
      navigator.clipboard.writeText(commit).then(done).catch(() => this._wb_copyFallback(commit, done));
    } else {
      this._wb_copyFallback(commit, done);
    }
  }
  private _wb_copyFallback(text: string, done: () => void) {
    try {
      const ta = document.createElement('textarea');
      ta.value = text; ta.style.position='fixed'; ta.style.opacity='0';
      document.body.appendChild(ta); ta.select(); document.execCommand('copy');
      document.body.removeChild(ta); done();
    } catch { /* noop */ }
  }

  private getInitialUrl(): string {
    if (typeof window !== 'undefined' && window.location) {
      return window.location.pathname || '/';
    }
    return this.router.url || '/';
  }

  private readonly currentUrl$ = this.router.events.pipe(
    filter((e): e is NavigationEnd => e instanceof NavigationEnd),
    map(e => e.urlAfterRedirects)
  );

  private readonly currentUrl = toSignal(this.currentUrl$, { initialValue: this.getInitialUrl() });

  readonly isAdminRoute = computed(() => {
    const url = this.currentUrl();
    if (!url) return false;
    return url.startsWith('/admin') || url.startsWith('/login');
  });

  readonly showAdminHeader = computed(() => {
    const url = this.currentUrl();
    if (!url) return false;
    const isPublicAuthRoute =
      url.includes('/login') ||
      url.includes('/forgot-password') ||
      url.includes('/reset-password');
    return url.startsWith('/admin') && !isPublicAuthRoute && this.authService.isAuthenticated();
  });

  ngOnInit(): void {
    // Proteção de Navegador contra Cache (BFCache Prevention):
    // Quando o usuário clica em "Voltar" após o logout, se a página estiver no cache de navegação,
    // intercepta a restauração e força redirecionamento imediato para a tela de login.
    if (typeof window !== 'undefined') {
      window.addEventListener('pageshow', (event: PageTransitionEvent) => {
        if (event.persisted && window.location.pathname.startsWith('/admin')) {
          if (!this.authService.isAuthenticated() && !window.location.pathname.includes('/admin/login')) {
            window.location.replace('/admin/login');
          }
        }
      });
    }
  }
}

import { Component, computed, effect, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs/operators';
import { APP_VERSION } from '../../../../environments/version';

/**
 * Badge DISCRETO no CANTO INFERIOR DIREITO — aparece SÓ em rotas /admin/**.
 * Mostra hash curto do último commit (7 chars). Interativo:
 * - 1 clique = mostra detalhes (commit completo, branch, data build) por 5s
 * - Duplo clique = COPIA commit hash COMPLETO para a área de transferência
 * - Opacidade 8% idle (bem discreto), 100% no hover / focus com teclado
 */
@Component({
  selector: 'app-admin-version-badge',
  standalone: true,
  imports: [CommonModule],
  template: `
    <ng-container *ngIf="isAdminRoute()">
      <div
        #badge
        id="wb-admin-version-badge"
        class="wb-admin-version"
        [title]="tooltip()"
        tabindex="0"
        (click)="_toggleDetails()"
        (dblclick)="copyFullCommit()"
        (keydown.enter)="copyFullCommit()"
      >
        <span class="dot" [class.prod]="isProd()" [class.dev]="!isProd()"></span>
        <code>@{{ shortHash() }}</code>
      </div>

      <div *ngIf="showDetails()" class="wb-admin-version-toast" role="status" aria-live="polite">
        <div class="row"><strong>Commit:</strong> <code>{{ fullHash() }}</code></div>
        <div class="row"><strong>Branch:</strong> <span>{{ branch() }}</span></div>
        <div class="row"><strong>Build:</strong> <span>{{ buildDateFormatted() }}</span></div>
        <div class="row"><em>{{ copied() ? '✅ Hash copiado para a área de transferência!' : '💡 Dobre clique para copiar o commit completo' }}</em></div>
      </div>
    </ng-container>
  `,
  styles: [`
    .wb-admin-version {
      position: fixed;
      right: 10px;
      bottom: 10px;
      z-index: 10;
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 2px 10px 2px 8px;
      border-radius: 999px;
      background: rgba(17, 24, 39, 0.92);
      color: #d1d5db;
      font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
      font-size: 10.5px;
      line-height: 18px;
      letter-spacing: 0.2px;
      opacity: 0.08;
      transition: all 220ms ease;
      cursor: pointer;
      user-select: none;
      -webkit-user-select: none;
      border: 1px solid rgba(107, 114, 128, 0.35);
      backdrop-filter: blur(4px);
      -webkit-backdrop-filter: blur(4px);
    }
    .wb-admin-version:hover,
    .wb-admin-version:focus-within { opacity: 1; transform: translateY(-1px); box-shadow: 0 6px 18px rgba(15,23,42,.45); outline: none; }
    .wb-admin-version code { color:#f3f4f6; font-size:10.5px; font-weight:600; background:none; padding:0; border:0; }
    .dot { width:8px; height:8px; border-radius:50%; box-shadow:0 0 0 2px rgba(255,255,255,0.06); display:inline-block; }
    .dot.dev { background:#f59e0b; animation: pulseWarn 3s infinite; }
    .dot.prod{ background:#10b981; animation: pulseOk 5s infinite; }
    @keyframes pulseOk   { 0%,100%{opacity:1} 50%{opacity:.45}}
    @keyframes pulseWarn { 0%,100%{opacity:1} 50%{opacity:.45}}

    .wb-admin-version-toast {
      position: fixed; right: 10px; bottom: 44px; z-index: 11;
      min-width: 260px; max-width: 360px; padding: 12px 14px; border-radius: 10px;
      background: rgba(17, 24, 39, 0.98);
      color: #e5e7eb;
      font-size: 11.5px; line-height: 1.55;
      border: 1px solid rgba(75,85,99, 0.6);
      box-shadow: 0 10px 30px rgba(15,23,42,0.5);
      animation: slideIn 180ms ease-out;
    }
    .wb-admin-version-toast code { background:rgba(75,85,99, 0.35); padding: 1px 6px; border-radius:5px; font-size:11px; }
    .wb-admin-version-toast .row { display:flex; justify-content:space-between; gap:10px; margin:2px 0; }
    @keyframes slideIn { from { transform: translateY(6px); opacity: 0} to {transform: none; opacity:1}}

    @media (max-width: 640px) {
      .wb-admin-version { right:6px; bottom:6px; font-size:9.5px; padding:2px 8px; opacity: 0.06;}
      .wb-admin-version-toast { right:6px; bottom:38px; min-width: 220px; font-size: 11px;}
    }
    @media print { .wb-admin-version, .wb-admin-version-toast { display:none !important; } }
  `]
})
export class AdminVersionBadgeComponent implements OnInit {
  private readonly router = inject(Router);

  readonly fullHash  = signal<string>(APP_VERSION.commit);
  readonly shortHash = computed(() => this.fullHash().substring(0, 7));
  readonly branch    = signal<string>(APP_VERSION.branch);
  readonly buildDate = signal<string>(APP_VERSION.buildDate);
  readonly buildDateFormatted = computed(() => {
    try { return new Date(this.buildDate()).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short', timeZone: 'America/Sao_Paulo' }); }
    catch { return this.buildDate(); }
  });

  readonly isProd = computed(() => !this.fullHash().includes('dev') && window.location.hostname !== 'localhost');
  readonly copied = signal<boolean>(false);
  readonly showDetails = signal<boolean>(false);
  readonly isAdminRoute = signal<boolean>(this.router.url.includes('/admin'));

  readonly tooltip = computed(() => [
    `Build: ${this.buildDateFormatted()}`,
    `Branch: ${this.branch()}`,
    `Commit completo: ${this.fullHash()}`,
    `Dobre clique para copiar o commit completo.`
  ].join(String.fromCharCode(10)));

  constructor() {
    effect(() => this.isAdminRoute.set(this.router.url.includes('/admin')));
  }

  ngOnInit(): void {
    this.router.events.pipe(filter(e => e instanceof NavigationEnd)).subscribe(() => {
      this.isAdminRoute.set(this.router.url.includes('/admin'));
    });
  }

  _toggleDetails(): void {
    const v = !this.showDetails();
    this.showDetails.set(v);
    if (v) setTimeout(() => this.showDetails.set(false), 5000);
  }

  copyFullCommit(): void {
    const commit = this.fullHash();
    const done = () => {
      this.copied.set(true);
      this.showDetails.set(true);
      setTimeout(() => { this.copied.set(false); this.showDetails.set(false); }, 3500);
    };
    if (navigator?.clipboard?.writeText) {
      navigator.clipboard.writeText(commit).then(done).catch(() => this._fallbackCopy(commit, done));
    } else {
      this._fallbackCopy(commit, done);
    }
  }

  private _fallbackCopy(text: string, onDone: () => void): void {
    try {
      const ta = document.createElement('textarea');
      ta.value = text;
      ta.style.position = 'fixed';
      ta.style.opacity = '0';
      document.body.appendChild(ta);
      ta.select();
      document.execCommand('copy');
      document.body.removeChild(ta);
      onDone();
    } catch { /* no-op */ }
  }
}
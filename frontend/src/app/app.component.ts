import { Component, inject, computed, OnInit } from '@angular/core';
import { Router, NavigationEnd, RouterModule } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { filter, map } from 'rxjs/operators';
import { HeaderComponent } from './shared/components/header/header.component';
import { FooterComponent } from './shared/components/footer/footer.component';
import { AdminHeaderComponent } from './shared/components/admin-header/admin-header.component';
import { AuthService } from './core/services/auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterModule, HeaderComponent, FooterComponent, AdminHeaderComponent],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit {
  private readonly router = inject(Router);
  readonly authService = inject(AuthService);

  private readonly currentUrl$ = this.router.events.pipe(
    filter((e): e is NavigationEnd => e instanceof NavigationEnd),
    map(e => e.urlAfterRedirects)
  );

  private readonly currentUrl = toSignal(this.currentUrl$, { initialValue: this.router.url });

  readonly isAdminRoute = computed(() => {
    const url = this.currentUrl();
    return url ? url.startsWith('/admin') : false;
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

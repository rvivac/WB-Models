import { Injectable, signal } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class ThemeService {
  // Controle de estado da barra de navegação móvel e painel
  readonly isMobileMenuOpen = signal<boolean>(false);

  toggleMobileMenu(): void {
    this.isMobileMenuOpen.update(prev => !prev);
  }

  closeMobileMenu(): void {
    this.isMobileMenuOpen.set(false);
  }
}

import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ThemeService } from '../../../core/services/theme.service';
import { TranslatePipe } from '../../pipes/translate.pipe';
import { LanguageSelectorComponent } from '../language-selector/language-selector.component';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslatePipe, LanguageSelectorComponent],
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.scss']
})
export class HeaderComponent {
  readonly themeService = inject(ThemeService);

  toggleMenu(): void {
    this.themeService.toggleMobileMenu();
  }

  closeMenu(): void {
    this.themeService.closeMobileMenu();
  }
}

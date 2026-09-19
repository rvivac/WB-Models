import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslationService, SupportedLanguage } from '../../../core/services/translation.service';

@Component({
  selector: 'app-language-selector',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './language-selector.component.html',
  styleUrls: ['./language-selector.component.scss']
})
export class LanguageSelectorComponent {
  readonly translationService = inject(TranslationService);

  changeLanguage(lang: SupportedLanguage): void {
    this.translationService.setLanguage(lang);
  }

  isCurrent(lang: SupportedLanguage): boolean {
    return this.translationService.currentLang() === lang;
  }
}

import { Injectable, inject, signal, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Observable, tap, catchError, of } from 'rxjs';

export type SupportedLanguage = 'pt' | 'en';

const STORAGE_KEY = 'wb_scouting_lang';

@Injectable({
  providedIn: 'root'
})
export class TranslationService {
  private readonly http = inject(HttpClient);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly isBrowser = isPlatformBrowser(this.platformId);

  readonly currentLang = signal<SupportedLanguage>(this.getInitialLanguage());
  readonly translations = signal<Record<string, any>>({});

  constructor() {}

  /**
   * Initializes translations for the currently selected language.
   * Can be hooked into APP_INITIALIZER.
   */
  init(): Observable<Record<string, any>> {
    const lang = this.currentLang();
    return this.loadTranslations(lang);
  }

  /**
   * Switches language, persists choice, and loads the corresponding dictionary.
   */
  setLanguage(lang: SupportedLanguage): void {
    if (this.currentLang() === lang && Object.keys(this.translations()).length > 0) {
      return;
    }

    this.currentLang.set(lang);

    if (this.isBrowser) {
      try {
        localStorage.setItem(STORAGE_KEY, lang);
      } catch (e) {
        console.warn('Unable to persist language preference in localStorage', e);
      }
    }

    this.loadTranslations(lang).subscribe();
  }

  /**
   * Resolves nested dot-separated keys (e.g. 'nav.models') with variable interpolation.
   */
  translate(key: string, params?: Record<string, string | number>): string {
    if (!key) return '';

    const dict = this.translations();
    const segments = key.split('.');
    let current: any = dict;

    for (const segment of segments) {
      if (current && typeof current === 'object' && segment in current) {
        current = current[segment];
      } else {
        current = undefined;
        break;
      }
    }

    if (typeof current !== 'string') {
      return key;
    }

    let result = current;
    if (params) {
      Object.entries(params).forEach(([placeholder, value]) => {
        result = result.replace(new RegExp(`\\{${placeholder}\\}`, 'g'), String(value));
      });
    }

    return result;
  }

  /**
   * Helper to unpack dynamic backend content payloads based on active language signal.
   */
  selectContent<T>(payloadPt: T, payloadEn: T): T {
    return this.currentLang() === 'pt' ? payloadPt : (payloadEn ?? payloadPt);
  }

  /**
   * Loads translation dictionary file via HttpClient.
   */
  private loadTranslations(lang: SupportedLanguage): Observable<Record<string, any>> {
    return this.http.get<Record<string, any>>(`/assets/i18n/${lang}.json`).pipe(
      tap((data) => {
        this.translations.set(data || {});
      }),
      catchError((error) => {
        console.error(`Failed to load translations for '${lang}'`, error);
        return of({});
      })
    );
  }

  /**
   * Resolves initial language checking localStorage first, then navigator.language, fallback to 'pt'.
   */
  private getInitialLanguage(): SupportedLanguage {
    if (!this.isBrowser) {
      return 'pt';
    }

    try {
      const stored = localStorage.getItem(STORAGE_KEY) as SupportedLanguage | null;
      if (stored === 'pt' || stored === 'en') {
        return stored;
      }
    } catch (e) {
      console.warn('Failed to access localStorage for language preference', e);
    }

    try {
      const browserLang = navigator.language?.toLowerCase() || '';
      if (browserLang.startsWith('en')) {
        return 'en';
      }
    } catch (e) {
      console.warn('Failed to access navigator.language', e);
    }

    return 'pt';
  }
}

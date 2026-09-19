import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { TranslationService } from './translation.service';

describe('TranslationService', () => {
  let service: TranslationService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        TranslationService
      ]
    });
    service = TestBed.inject(TranslationService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
    localStorage.clear();
  });

  it('should be created and default to pt if no storage set', () => {
    expect(service).toBeTruthy();
    expect(service.currentLang()).toBe('pt');
  });

  it('should translate nested keys and handle missing keys gracefully', () => {
    service.translations.set({
      nav: {
        home: 'Início',
        welcome: 'Bem-vindo, {name}!'
      }
    });

    expect(service.translate('nav.home')).toBe('Início');
    expect(service.translate('nav.welcome', { name: 'João' })).toBe('Bem-vindo, João!');
    expect(service.translate('nav.nonexistent')).toBe('nav.nonexistent');
  });

  it('should select content based on active language', () => {
    service.currentLang.set('pt');
    expect(service.selectContent('Texto em Português', 'English Text')).toBe('Texto em Português');

    service.currentLang.set('en');
    expect(service.selectContent('Texto em Português', 'English Text')).toBe('English Text');
  });

  it('should update signal and localStorage on setLanguage', () => {
    service.setLanguage('en');
    expect(service.currentLang()).toBe('en');
    expect(localStorage.getItem('wb_scouting_lang')).toBe('en');

    const req = httpTesting.expectOne('/assets/i18n/en.json');
    expect(req.request.method).toBe('GET');
    req.flush({ nav: { home: 'Home' } });

    expect(service.translate('nav.home')).toBe('Home');
  });
});

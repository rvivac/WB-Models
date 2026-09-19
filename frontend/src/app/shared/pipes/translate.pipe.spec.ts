import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TranslatePipe } from './translate.pipe';
import { TranslationService } from '../../core/services/translation.service';

describe('TranslatePipe', () => {
  let pipe: TranslatePipe;
  let service: TranslationService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        TranslationService,
        TranslatePipe
      ]
    });

    service = TestBed.inject(TranslationService);
    pipe = TestBed.inject(TranslatePipe);
  });

  it('should transform key using TranslationService', () => {
    service.translations.set({
      actions: {
        view_profile: 'Ver perfil'
      }
    });

    expect(pipe.transform('actions.view_profile')).toBe('Ver perfil');
  });
});

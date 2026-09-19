import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { LanguageSelectorComponent } from './language-selector.component';
import { TranslationService } from '../../../core/services/translation.service';

describe('LanguageSelectorComponent', () => {
  let component: LanguageSelectorComponent;
  let fixture: ComponentFixture<LanguageSelectorComponent>;
  let translationService: TranslationService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LanguageSelectorComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        TranslationService
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(LanguageSelectorComponent);
    component = fixture.componentInstance;
    translationService = TestBed.inject(TranslationService);
    fixture.detectChanges();
  });

  it('should create and display language options', () => {
    expect(component).toBeTruthy();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('POR');
    expect(compiled.textContent).toContain('ENG');
  });

  it('should switch language when clicked', () => {
    spyOn(translationService, 'setLanguage');
    const buttons = fixture.nativeElement.querySelectorAll('.lang-btn');
    
    buttons[1].click(); // ENG
    expect(translationService.setLanguage).toHaveBeenCalledWith('en');

    buttons[0].click(); // POR
    expect(translationService.setLanguage).toHaveBeenCalledWith('pt');
  });
});

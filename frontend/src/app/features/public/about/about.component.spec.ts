import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { of } from 'rxjs';
import { AboutComponent } from './about.component';
import { TranslationService } from '../../../core/services/translation.service';
import { PublicContentService } from '../../../core/services/public-content.service';

describe('AboutComponent', () => {
  let component: AboutComponent;
  let fixture: ComponentFixture<AboutComponent>;
  let publicContentService: PublicContentService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AboutComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: TranslationService,
          useValue: {
            currentLang: () => 'pt',
            translate: (key: string) => {
              const dict: Record<string, string> = {
                'nav.home': 'Home',
                'nav.about': 'Sobre Nós'
              };
              return dict[key] || key;
            }
          }
        },
        PublicContentService
      ]
    }).compileComponents();

    publicContentService = TestBed.inject(PublicContentService);
    spyOn(publicContentService, 'getAboutManifestoContent').and.returnValue(of({
      headline: 'A Nova Estética do Scouting Test',
      quote: 'Citação editorial exclusiva para teste.',
      body: 'Texto longo do manifesto sobre a WB Agency.'
    }));

    fixture = TestBed.createComponent(AboutComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create about page without video element', () => {
    expect(component).toBeTruthy();
    const compiled = fixture.nativeElement as HTMLElement;
    // Não deve conter tag de vídeo nem app-home-hero
    expect(compiled.querySelector('video')).toBeNull();
    expect(compiled.querySelector('app-home-hero')).toBeNull();
  });

  it('should render dynamic headline, quote and body from CMS', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.about-headline')?.textContent).toContain('A Nova Estética do Scouting Test');
    expect(compiled.querySelector('.about-quote')?.textContent).toContain('Citação editorial exclusiva para teste.');
    expect(compiled.querySelector('.about-body-text')?.textContent).toContain('Texto longo do manifesto sobre a WB Agency.');
  });

  it('should render standardized breadcrumbs with Home link and Sobre Nós current item', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const breadcrumb = compiled.querySelector('.header-breadcrumbs');
    expect(breadcrumb).toBeTruthy();
    
    const homeLink = compiled.querySelector('.breadcrumb-link') as HTMLAnchorElement;
    expect(homeLink).toBeTruthy();
    expect(homeLink.getAttribute('routerLink')).toBe('/');
    
    const currentItem = compiled.querySelector('.breadcrumb-current');
    expect(currentItem).toBeTruthy();
    expect(currentItem?.textContent).toContain('Sobre Nós');
  });
});

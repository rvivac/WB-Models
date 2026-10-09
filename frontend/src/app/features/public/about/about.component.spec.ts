import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AboutComponent } from './about.component';
import { TranslationService } from '../../../core/services/translation.service';
import { environment } from '../../../../environments/environment';
import { AboutPage } from '../../../shared/models/about-page.interface';

describe('AboutComponent', () => {
  let component: AboutComponent;
  let fixture: ComponentFixture<AboutComponent>;
  let httpMock: HttpTestingController;

  const mockAboutPage: AboutPage = {
    title: 'A Nova Estética do Scouting Test',
    subtitle: 'MANIFESTO INSTITUCIONAL TEST',
    description: 'Descrição institucional detalhada para teste.',
    heroQuote: 'Citação editorial exclusiva para teste.',
    manifestoTitle: 'Nossa Filosofia Test',
    manifestoText: 'Texto longo do manifesto sobre a WB Agency.',
    pillarsTitle: 'Nossos Pilares & Valores Test',
    pillars: [
      { order: 1, titulo: 'Curadoria & Autenticidade', descricao: 'Pilar 1 descrição' },
      { order: 2, titulo: 'Transparência & Ética', descricao: 'Pilar 2 descrição' }
    ],
    seo: {
      metaTitle: 'Sobre Nós Test | WB Agency',
      metaDescription: 'Meta descrição de teste'
    }
  };

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
        }
      ]
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(AboutComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should create and fetch data from backend', () => {
    fixture.detectChanges();

    const req = httpMock.expectOne(`${environment.apiUrl}/public/institutional/about`);
    expect(req.request.method).toBe('GET');
    req.flush(mockAboutPage);

    fixture.detectChanges();

    expect(component).toBeTruthy();
    expect(component.title()).toBe('A Nova Estética do Scouting Test');
    expect(component.heroQuote()).toBe('Citação editorial exclusiva para teste.');
    expect(component.pillars().length).toBe(2);
  });

  it('should not contain video elements nor candidate cta cards', () => {
    fixture.detectChanges();
    const req = httpMock.expectOne(`${environment.apiUrl}/public/institutional/about`);
    req.flush(mockAboutPage);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('video')).toBeNull();
    expect(compiled.querySelector('.cta-card')).toBeNull();
    expect(compiled.textContent).not.toContain('Seu talento merece a visibilidade certa');
  });

  it('should render dynamic title, quote, manifesto and pillars from backend', () => {
    fixture.detectChanges();
    const req = httpMock.expectOne(`${environment.apiUrl}/public/institutional/about`);
    req.flush(mockAboutPage);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.page-title')?.textContent).toContain('A Nova Estética do Scouting Test');
    expect(compiled.querySelector('.hero-quote-box')?.textContent).toContain('Citação editorial exclusiva para teste.');
    expect(compiled.querySelector('.manifesto-section')?.textContent).toContain('Texto longo do manifesto sobre a WB Agency.');
    expect(compiled.querySelectorAll('.pillar-card').length).toBe(2);
  });

  it('should render standardized breadcrumbs with Home link and Sobre Nós current item', () => {
    fixture.detectChanges();
    const req = httpMock.expectOne(`${environment.apiUrl}/public/institutional/about`);
    req.flush(mockAboutPage);
    fixture.detectChanges();

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

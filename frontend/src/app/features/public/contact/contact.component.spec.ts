import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ContactComponent } from './contact.component';
import { SecureContactService } from '../../../core/services/secure-contact.service';
import { TranslationService } from '../../../core/services/translation.service';

describe('ContactComponent (SITE-004 Anti-Scraping & CyberSecurity)', () => {
  let component: ContactComponent;
  let fixture: ComponentFixture<ContactComponent>;
  let secureContactService: SecureContactService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ContactComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        SecureContactService,
        {
          provide: TranslationService,
          useValue: {
            currentLang: () => 'pt',
            translate: (key: string) => {
              const dict: Record<string, string> = {
                'nav.home': 'Home',
                'nav.contact': 'Contato',
                'contact_page.title': 'Canais de Contato',
                'contact_page.subtitle': 'Conecte-se com a WB Agency através de nossos canais institucionais seguros.'
              };
              return dict[key] || key;
            }
          }
        }
      ]
    }).compileComponents();

    secureContactService = TestBed.inject(SecureContactService);
    fixture = TestBed.createComponent(ContactComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create the contact component', () => {
    expect(component).toBeTruthy();
  });

  it('should render standardized breadcrumbs with Home link and Contato current item', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const breadcrumb = compiled.querySelector('.header-breadcrumbs');
    expect(breadcrumb).toBeTruthy();

    const homeLink = compiled.querySelector('.breadcrumb-link') as HTMLAnchorElement;
    expect(homeLink).toBeTruthy();
    expect(homeLink.getAttribute('routerLink')).toBe('/');

    const currentItem = compiled.querySelector('.breadcrumb-current');
    expect(currentItem).toBeTruthy();
    expect(currentItem?.textContent).toContain('Contato');

    const title = compiled.querySelector('.contact-title');
    expect(title).toBeTruthy();
    expect(title?.textContent).toContain('Canais de Contato');
  });

  describe('Anti-Scraping / Defense-in-Depth Verification', () => {
    it('should NOT contain static mailto: links in the DOM', () => {
      const compiled = fixture.nativeElement as HTMLElement;
      const mailtoLinks = compiled.querySelectorAll('a[href^="mailto:"]');
      expect(mailtoLinks.length).toBe(0);
    });

    it('should NOT contain static WhatsApp links (wa.me or api.whatsapp.com) in the DOM', () => {
      const compiled = fixture.nativeElement as HTMLElement;
      const waLinks = compiled.querySelectorAll('a[href*="wa.me"], a[href*="whatsapp.com"]');
      expect(waLinks.length).toBe(0);
    });

    it('should render contact data dynamically from SecureContactService', () => {
      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.textContent).toContain(secureContactService.phoneDisplay);
      expect(compiled.textContent).toContain(secureContactService.emailDisplay);
      expect(compiled.textContent).toContain(secureContactService.instagramDisplay);
    });
  });

  describe('Contact Channels Cards', () => {
    it('should render the simplified Contatos card with all direct channels', () => {
      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.querySelector('#card-bookers')).toBeTruthy();
      expect(compiled.querySelector('#btn-secure-whatsapp')).toBeTruthy();
      expect(compiled.querySelector('#btn-secure-phone')).toBeTruthy();
      expect(compiled.querySelector('#btn-secure-email')).toBeTruthy();
      expect(compiled.querySelector('#btn-secure-instagram')).toBeTruthy();
    });

    it('should trigger openPhone() on Phone button click', () => {
      const spy = spyOn(secureContactService, 'openPhone');
      const compiled = fixture.nativeElement as HTMLElement;
      const phoneBtn = compiled.querySelector('#btn-secure-phone') as HTMLButtonElement;
      expect(phoneBtn).toBeTruthy();

      phoneBtn.click();
      expect(spy).toHaveBeenCalled();
    });

    it('should trigger openWhatsApp() on WhatsApp button click', () => {
      const spy = spyOn(secureContactService, 'openWhatsApp');
      const compiled = fixture.nativeElement as HTMLElement;
      const waBtn = compiled.querySelector('#btn-secure-whatsapp') as HTMLButtonElement;
      expect(waBtn).toBeTruthy();

      waBtn.click();
      expect(spy).toHaveBeenCalled();
    });

    it('should trigger openMail() on Email button click', () => {
      const spy = spyOn(secureContactService, 'openMail');
      const compiled = fixture.nativeElement as HTMLElement;
      const mailBtn = compiled.querySelector('#btn-secure-email') as HTMLButtonElement;
      expect(mailBtn).toBeTruthy();

      mailBtn.click();
      expect(spy).toHaveBeenCalled();
    });

    it('should trigger openInstagram() on Instagram button click', () => {
      const spy = spyOn(secureContactService, 'openInstagram');
      const compiled = fixture.nativeElement as HTMLElement;
      const igBtn = compiled.querySelector('#btn-secure-instagram') as HTMLButtonElement;
      expect(igBtn).toBeTruthy();

      igBtn.click();
      expect(spy).toHaveBeenCalled();
    });

    it('should not render booking quotation card', () => {
      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.querySelector('.booking-form-section')).toBeFalsy();
      expect(compiled.querySelector('.booking-form-card')).toBeFalsy();
    });
  });
});

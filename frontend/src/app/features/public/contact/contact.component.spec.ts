import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ContactComponent } from './contact.component';
import { SecureContactService } from '../../../core/services/secure-contact.service';

describe('ContactComponent (SITE-004 Anti-Scraping & CyberSecurity)', () => {
  let component: ContactComponent;
  let fixture: ComponentFixture<ContactComponent>;
  let secureContactService: SecureContactService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ContactComponent],
      providers: [
        provideRouter([]),
        SecureContactService
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
      expect(compiled.textContent).toContain('+55 11 97065-6003');
      expect(compiled.textContent).toContain('infowbscouting@gmail.com');
      expect(compiled.textContent).toContain('@infowbagency');
    });
  });

  describe('Contact Channels Cards', () => {
    it('should render all 3 distinct contact cards (Casting, Bookers, Social)', () => {
      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.querySelector('#card-casting')).toBeTruthy();
      expect(compiled.querySelector('#card-bookers')).toBeTruthy();
      expect(compiled.querySelector('#card-instagram')).toBeTruthy();
    });

    it('should provide casting redirection to /seja-modelo', () => {
      const compiled = fixture.nativeElement as HTMLElement;
      const applyBtn = compiled.querySelector('#btn-apply-casting') as HTMLAnchorElement;
      expect(applyBtn).toBeTruthy();
      expect(applyBtn.getAttribute('routerLink')).toBe('/seja-modelo');
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
  });

  describe('Booking Quotation Form', () => {
    it('should validate required fields before submission', () => {
      expect(component.contactForm.valid).toBeFalse();
      component.onSubmit();
      expect(component.contactForm.touched).toBeTrue();
      expect(component.isSubmitted()).toBeFalse();
    });

    it('should submit successfully with valid data', fakeAsync(() => {
      component.contactForm.setValue({
        name: 'Carlos Produtor',
        company: 'Agência Criativa',
        email: 'carlos@produtora.com',
        phone: '11988887777',
        interestType: 'CAMPAIGN',
        message: 'Gostaria de solicitar casting para campanha de verão de moda praia.'
      });

      expect(component.contactForm.valid).toBeTrue();

      component.onSubmit();
      expect(component.isSending()).toBeTrue();

      tick(900);
      fixture.detectChanges();

      expect(component.isSending()).toBeFalse();
      expect(component.isSubmitted()).toBeTrue();

      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.querySelector('.form-success')).toBeTruthy();
    }));
  });
});

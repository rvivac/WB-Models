import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { FooterComponent } from './footer.component';
import { TranslationService } from '../../../core/services/translation.service';
import { environment } from '../../../../environments/environment';

describe('FooterComponent', () => {
  let component: FooterComponent;
  let fixture: ComponentFixture<FooterComponent>;
  let translationService: TranslationService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FooterComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    }).compileComponents();

    translationService = TestBed.inject(TranslationService);
    translationService.setLanguage('pt');

    fixture = TestBed.createComponent(FooterComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create footer with current year and brand logo', () => {
    expect(component).toBeTruthy();
    expect(component.currentYear).toBe(new Date().getFullYear());
    const compiled = fixture.nativeElement as HTMLElement;
    const logoImg = compiled.querySelector('img');
    expect(logoImg?.getAttribute('alt')).toBe('WB Agency');
    expect(logoImg?.getAttribute('src')).toBe('assets/images/logo-wb-agency.jpeg');
  });

  it('should render navigation links and contact info without obsolete texts', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    // Coluna Navegação e seus 6 links
    expect(compiled.textContent).toContain('Navegação');
    expect(compiled.textContent).toContain('Modelos Femininos');
    expect(compiled.textContent).toContain('Modelos Masculinos');
    expect(compiled.textContent).toContain('Stars');
    expect(compiled.textContent).toContain('Sobre Nós');
    expect(compiled.textContent).toContain('Contato');
    expect(compiled.textContent).toContain('Quero ser Modelo');

    // Não deve conter e-mail falso de fallback
    expect(compiled.textContent).not.toContain('contato@wbscouting.com');
    expect(compiled.textContent).not.toContain('99999-9999');

    // Verificação de ausência de textos antigos e link de acesso restrito (regra: jamais recolocar)
    expect(compiled.textContent).not.toContain('PARIS • MILAN • NEW YORK • SÃO PAULO');
    expect(compiled.textContent).not.toContain('Agência de modelos e gestão internacional');
    expect(compiled.textContent).not.toContain('Plataforma');
    expect(compiled.textContent).not.toContain('Acesso Restrito');
    expect(compiled.querySelector('a[routerLink="/admin/login"]')).toBeNull();
  });

  it('should have logo with proper classes and alt attribute', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const logoImg = compiled.querySelector('img');
    expect(logoImg).toBeTruthy();
    expect(logoImg?.className).toContain('h-28');
    expect(logoImg?.className).toContain('md:h-36');
    expect(logoImg?.className).toContain('object-contain');
  });

  it('should clean whatsapp number correctly', () => {
    expect(component.cleanWhatsAppNumber('+55 (11) 97065-6003')).toBe('5511970656003');
    expect(component.cleanWhatsAppNumber(undefined)).toBe('');
  });

  it('should update contactData and render dynamic social networks from API', () => {
    (component as any).applyContactSettings({
      primaryEmail: 'booking@wbagency.com.br',
      whatsapp: '+55 11 98888-7777',
      socialMediaList: [
        { name: 'YouTube', url: 'https://youtube.com/@wbagency' },
        { name: 'TikTok', url: 'https://tiktok.com/@wbagency' }
      ]
    });
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('booking@wbagency.com.br');
    expect(compiled.textContent).toContain('+55 11 98888-7777');
    expect(compiled.textContent).toContain('YouTube');
    expect(compiled.textContent).toContain('TikTok');

    const emailLink = compiled.querySelector('a[href^="mailto:"]') as HTMLAnchorElement;
    expect(emailLink?.getAttribute('href')).toBe('mailto:booking@wbagency.com.br');

    const waLink = compiled.querySelector('a[href*="wa.me"]') as HTMLAnchorElement;
    expect(waLink?.getAttribute('href')).toBe('https://wa.me/5511988887777');
  });

  it('should render credits link to rvivac guild with target _blank', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const creditsLink = compiled.querySelector('.credits-link') as HTMLAnchorElement;
    expect(creditsLink).toBeTruthy();
    expect(creditsLink.getAttribute('href')).toBe('https://www.rvivacguild.com.br');
    expect(creditsLink.getAttribute('target')).toBe('_blank');
    expect(creditsLink.getAttribute('rel')).toContain('noopener');
    expect(creditsLink.getAttribute('rel')).toContain('noreferrer');
    expect(compiled.textContent).toContain('Rvivac Guild');
  });

  it('should dynamically update all footer texts when language is changed to ENG', () => {
    translationService.setLanguage('en');
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Navigation');
    expect(compiled.textContent).toContain('Female Models');
    expect(compiled.textContent).toContain('Male Models');
    expect(compiled.textContent).toContain('Stars');
    expect(compiled.textContent).toContain('About Us');
    expect(compiled.textContent).toContain('Contact');
    expect(compiled.textContent).toContain('Become a Model');
    expect(compiled.textContent).toContain('All rights reserved.');
    expect(compiled.textContent).toContain('Terms of Use');
    expect(compiled.textContent).toContain('Privacy & GDPR');
    expect(compiled.textContent).not.toContain('Restricted Access');

    // Se houver redes sociais cadastradas, deve exibir Social Media
    component.contactData.socialMediaList = [{ name: 'Instagram', url: 'https://instagram.com/wb' }];
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Social Media');
  });

  it('should open institutional modal for TERMS and load content', () => {
    const httpTesting = TestBed.inject(HttpTestingController);
    component.openInstitutionalModal('TERMS');
    expect(component.isModalOpen()).toBeTrue();
    expect(component.modalTitle()).toBe('Termos de Uso');
    expect(component.isLoadingContent()).toBeTrue();

    const req = httpTesting.expectOne(`${environment.apiUrl}/public/content/TERMS?lang=pt`);
    expect(req.request.method).toBe('GET');
    req.flush({ content: 'Termos e Condições WB Agency' });

    expect(component.isLoadingContent()).toBeFalse();
    expect(component.modalContent()).toBe('Termos e Condições WB Agency');

    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Termos e Condições WB Agency');

    component.closeModal();
    expect(component.isModalOpen()).toBeFalse();
  });

  it('should open institutional modal for PRIVACY in EN and load content', () => {
    const httpTesting = TestBed.inject(HttpTestingController);
    translationService.setLanguage('en');
    component.openInstitutionalModal('PRIVACY');
    expect(component.isModalOpen()).toBeTrue();
    expect(component.modalTitle()).toBe('Privacy & LGPD');

    const req = httpTesting.expectOne(`${environment.apiUrl}/public/content/PRIVACY?lang=en`);
    expect(req.request.method).toBe('GET');
    req.flush({ content: 'Privacy Policy WB Agency' });

    expect(component.modalContent()).toBe('Privacy Policy WB Agency');

    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Privacy Policy WB Agency');
  });

  it('should use i18n fallback when API fails or returns empty', () => {
    const httpTesting = TestBed.inject(HttpTestingController);
    component.openInstitutionalModal('TERMS');

    const req = httpTesting.expectOne(`${environment.apiUrl}/public/content/TERMS?lang=pt`);
    req.error(new ProgressEvent('Network error'));

    expect(component.isLoadingContent()).toBeFalse();
    expect(component.modalContent()).toContain('TERMOS E CONDIÇÕES DE USO');

    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('TERMOS E CONDIÇÕES DE USO');
  });
});

import { TestBed } from '@angular/core/testing';
import { SecureContactService } from './secure-contact.service';

describe('SecureContactService', () => {
  let service: SecureContactService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [SecureContactService]
    });
    service = TestBed.inject(SecureContactService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should correctly decode obfuscated email display', () => {
    expect(service.emailDisplay).toBe('contato@wbscouting.com');
  });

  it('should return phone display format', () => {
    expect(service.phoneDisplay).toBe('+55 (11) 99999-9999');
  });

  it('should return instagram display handle', () => {
    expect(service.instagramDisplay).toBe('@wbscouting');
  });

  it('should open WhatsApp with direct link and noopener/noreferrer', () => {
    const windowSpy = spyOn(window, 'open').and.stub();

    service.openWhatsApp();

    expect(windowSpy).toHaveBeenCalledWith(
      'https://wa.me/5511999999999',
      '_blank',
      'noopener,noreferrer'
    );
  });

  it('should open Instagram with official handle and noopener/noreferrer', () => {
    const windowSpy = spyOn(window, 'open').and.stub();

    service.openInstagram();

    expect(windowSpy).toHaveBeenCalledWith(
      'https://www.instagram.com/wbscouting/',
      '_blank',
      'noopener,noreferrer'
    );
  });
});

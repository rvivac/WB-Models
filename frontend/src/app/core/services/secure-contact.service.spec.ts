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
    expect(service.emailDisplay).toBe('infowbscouting@gmail.com');
  });

  it('should return phone display format', () => {
    expect(service.phoneDisplay).toBe('+55 11 97065-6003');
  });

  it('should return instagram display handle', () => {
    expect(service.instagramDisplay).toBe('@infowbagency');
  });

  it('should open WhatsApp with decoded number, encoded message and noopener/noreferrer', () => {
    const windowSpy = spyOn(window, 'open').and.stub();

    service.openWhatsApp();

    expect(windowSpy).toHaveBeenCalledWith(
      jasmine.stringMatching(/https:\/\/api\.whatsapp\.com\/send\?phone=5511970656003&text=/),
      '_blank',
      'noopener,noreferrer'
    );
  });

  it('should open Instagram with official handle and noopener/noreferrer', () => {
    const windowSpy = spyOn(window, 'open').and.stub();

    service.openInstagram();

    expect(windowSpy).toHaveBeenCalledWith(
      'https://www.instagram.com/infowbagency/',
      '_blank',
      'noopener,noreferrer'
    );
  });
});

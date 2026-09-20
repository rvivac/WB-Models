import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { PublicContentService } from './public-content.service';
import { environment } from '../../../environments/environment';

describe('PublicContentService', () => {
  let service: PublicContentService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        PublicContentService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });

    service = TestBed.inject(PublicContentService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should get hero content from API', () => {
    service.getHeroContent('pt').subscribe(data => {
      expect(data).toBeTruthy();
      expect(data.title).toBe('Título Customizado');
      expect(data.videoUrl).toBe('https://video.mp4');
    });

    const req = httpTesting.expectOne(`${environment.apiUrl}/public/content/HOME_HERO?lang=pt`);
    expect(req.request.method).toBe('GET');
    req.flush({
      sectionKey: 'HOME_HERO',
      payload: {
        title: 'Título Customizado',
        videoUrl: 'https://video.mp4'
      },
      lang: 'pt'
    });
  });

  it('should fallback to default hero content on HTTP error', () => {
    service.getHeroContent('en').subscribe(data => {
      expect(data).toBeTruthy();
      expect(data.title).toBe('High Fashion & Scouting');
    });

    const req = httpTesting.expectOne(`${environment.apiUrl}/public/content/HOME_HERO?lang=en`);
    req.error(new ProgressEvent('error'));
  });

  it('should get contact channels from API', () => {
    service.getContactChannels('pt').subscribe(data => {
      expect(data).toBeTruthy();
      expect(data.email).toBe('contato@wbscouting.com');
      expect(data.whatsappNumber).toBe('5511999999999');
    });

    const req = httpTesting.expectOne(`${environment.apiUrl}/public/contact-channels?lang=pt`);
    expect(req.request.method).toBe('GET');
    req.flush({
      email: 'contato@wbscouting.com',
      whatsappNumber: '5511999999999',
      whatsappUrl: 'https://wa.me/5511999999999',
      instagramHandle: '@wbscouting',
      address: 'São Paulo - SP',
      officeHours: '09h às 18h'
    });
  });
});

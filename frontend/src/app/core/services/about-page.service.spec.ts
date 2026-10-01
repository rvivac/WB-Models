import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AboutPageService } from './about-page.service';
import { AboutPage } from '../../shared/models/about-page.interface';

describe('AboutPageService', () => {
  let service: AboutPageService;
  let httpMock: HttpTestingController;

  const mockAboutPage: AboutPage = {
    title: 'A Nova Estética do Scouting Global',
    subtitle: 'MANIFESTO INSTITUCIONAL',
    description: 'Descrição de teste',
    heroQuote: 'Citação hero de teste',
    manifestoTitle: 'Nossa Filosofia',
    manifestoText: 'Texto do manifesto de teste',
    pillarsTitle: 'Nossos Pilares & Valores',
    pillars: [
      {
        order: 1,
        titulo: 'Curadoria & Autenticidade',
        descricao: 'Descrição pilar 1'
      }
    ],
    seo: {
      metaTitle: 'Sobre Nós | WB Agency',
      metaDescription: 'Descrição SEO'
    }
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        AboutPageService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });

    service = TestBed.inject(AboutPageService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('deve ser instanciado com sucesso', () => {
    expect(service).toBeTruthy();
  });

  it('deve buscar dados públicos da página Sobre Nós com sucesso', () => {
    service.getPublicAboutPage().subscribe(res => {
      expect(res.title).toBe(mockAboutPage.title);
      expect(res.heroQuote).toBe(mockAboutPage.heroQuote);
      expect(res.pillars.length).toBe(1);
    });

    const req = httpMock.expectOne(request => request.url.includes('/public/institutional/about'));
    expect(req.request.method).toBe('GET');
    req.flush(mockAboutPage);
  });

  it('deve retornar fallback em caso de erro na rota pública', () => {
    service.getPublicAboutPage().subscribe(res => {
      expect(res.title).toBe(service.defaultAboutPage.title);
    });

    const req = httpMock.expectOne(request => request.url.includes('/public/institutional/about'));
    req.error(new ProgressEvent('error'));
  });

  it('deve buscar dados admin com sucesso', () => {
    service.getAdminAboutPage().subscribe(res => {
      expect(res.title).toBe(mockAboutPage.title);
    });

    const req = httpMock.expectOne(request => request.url.includes('/admin/institutional/about'));
    expect(req.request.method).toBe('GET');
    req.flush(mockAboutPage);
  });

  it('deve atualizar dados da página Sobre Nós via PUT', () => {
    service.updateAboutPage(mockAboutPage).subscribe(res => {
      expect(res.title).toBe(mockAboutPage.title);
    });

    const req = httpMock.expectOne(request => request.url.includes('/admin/institutional/about'));
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(mockAboutPage);
    req.flush(mockAboutPage);
  });
});

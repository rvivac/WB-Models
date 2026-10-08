import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { HttpEventType } from '@angular/common/http';
import { HomeContentManagerComponent, HomeContentData } from './home-content-manager.component';
import { environment } from '../../../../environments/environment';

describe('HomeContentManagerComponent', () => {
  let component: HomeContentManagerComponent;
  let fixture: ComponentFixture<HomeContentManagerComponent>;
  let httpTesting: HttpTestingController;

  const mockHomeData: HomeContentData = {
    heroTitle: 'WB AGENCY',
    heroSubtitle: 'EDITORIAL & HIGH FASHION SCOUTING',
    heroDescription: 'Representação exclusiva e curadoria estética conectada ao mercado global.',
    scrollLabel: 'SCROLL',
    metaTitle: 'WB Agency | Scouting Internacional e Alta Moda',
    metaDescription: 'Agência de scouting e modelos com foco editorial.',
    videoUrl: 'https://storage.wb.agency/hero.mp4',
    posterUrl: 'https://storage.wb.agency/poster.webp'
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HomeContentManagerComponent, ReactiveFormsModule, HttpClientTestingModule]
    }).compileComponents();

    fixture = TestBed.createComponent(HomeContentManagerComponent);
    component = fixture.componentInstance;
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('deve carregar os dados vigentes da Home via GET', () => {
    fixture.detectChanges();

    const req = httpTesting.expectOne(`${environment.apiUrl}/admin/institutional/home`);
    expect(req.request.method).toBe('GET');
    req.flush(mockHomeData);

    expect(component.form.get('heroTitle')?.value).toBe('WB AGENCY');
    expect(component.form.get('heroSubtitle')?.value).toBe('EDITORIAL & HIGH FASHION SCOUTING');
    expect(component.currentVideoUrl).toBe('https://storage.wb.agency/hero.mp4');
    expect(component.currentPosterUrl).toBe('https://storage.wb.agency/poster.webp');
  });

  it('deve utilizar fallback mock caso a API retorne erro no carregamento', () => {
    fixture.detectChanges();

    const req = httpTesting.expectOne(`${environment.apiUrl}/admin/institutional/home`);
    req.flush('API Error', { status: 500, statusText: 'Server Error' });

    expect(component.form.get('heroTitle')?.value).toBe('WB AGENCY');
    expect(component.currentVideoUrl).toContain('wb-presentation.mp4');
  });

  it('deve atualizar textos institucionais e metadados SEO via PUT', () => {
    fixture.detectChanges();
    httpTesting.expectOne(`${environment.apiUrl}/admin/institutional/home`).flush(mockHomeData);

    component.form.patchValue({
      heroTitle: 'NOVO TITULO',
      scrollLabel: 'EXPLORE'
    });

    component.saveTextContent();
    expect(component.isSaving).toBeTrue();

    const putReq = httpTesting.expectOne(`${environment.apiUrl}/admin/institutional/home`);
    expect(putReq.request.method).toBe('PUT');
    expect(putReq.request.body.heroTitle).toBe('NOVO TITULO');
    expect(putReq.request.body.scrollLabel).toBe('EXPLORE');

    putReq.flush({ ...mockHomeData, heroTitle: 'NOVO TITULO', scrollLabel: 'EXPLORE' });

    expect(component.isSaving).toBeFalse();
    expect(component.feedbackMessage).toContain('atualizados com sucesso');
  });

  it('deve permitir salvar formulário com textos vazios', () => {
    fixture.detectChanges();
    httpTesting.expectOne(`${environment.apiUrl}/admin/institutional/home`).flush(mockHomeData);

    component.form.patchValue({ heroTitle: '' });
    component.saveTextContent();

    const putReq = httpTesting.expectOne(`${environment.apiUrl}/admin/institutional/home`);
    expect(putReq.request.method).toBe('PUT');
    expect(putReq.request.body.heroTitle).toBe('');
    putReq.flush({ ...mockHomeData, heroTitle: '' });
  });

  it('deve realizar upload do vídeo hero com monitoramento de progresso', () => {
    fixture.detectChanges();
    httpTesting.expectOne(`${environment.apiUrl}/admin/institutional/home`).flush(mockHomeData);

    const videoFile = new File(['dummy video content'], 'hero.mp4', { type: 'video/mp4' });
    const event = { target: { files: [videoFile] } } as unknown as Event;

    component.onVideoSelected(event);
    expect(component.isVideoUploading).toBeTrue();

    const uploadReq = httpTesting.expectOne(`${environment.apiUrl}/admin/institutional/home/video`);
    expect(uploadReq.request.method).toBe('PUT');

    // Simula evento de progresso
    uploadReq.event({
      type: HttpEventType.UploadProgress,
      loaded: 50,
      total: 100
    });
    expect(component.videoUploadProgress).toBe(50);

    // Resposta final
    uploadReq.flush({ videoUrl: 'https://storage.wb.agency/new-hero.mp4' });

    expect(component.isVideoUploading).toBeFalse();
    expect(component.currentVideoUrl).toBe('https://storage.wb.agency/new-hero.mp4');
    expect(component.feedbackMessage).toContain('atualizado com sucesso');
  });

  it('deve rejeitar vídeo com tamanho superior a 50MB', () => {
    fixture.detectChanges();
    httpTesting.expectOne(`${environment.apiUrl}/admin/institutional/home`).flush(mockHomeData);

    const bigBlob = new Blob([new Uint8Array(51 * 1024 * 1024)]);
    const bigFile = new File([bigBlob], 'huge.mp4', { type: 'video/mp4' });
    const event = { target: { files: [bigFile] } } as unknown as Event;

    component.onVideoSelected(event);

    httpTesting.expectNone(`${environment.apiUrl}/admin/institutional/home/video`);
    expect(component.feedbackMessage).toContain('50MB');
  });

  it('deve realizar upload do poster de contingência', () => {
    fixture.detectChanges();
    httpTesting.expectOne(`${environment.apiUrl}/admin/institutional/home`).flush(mockHomeData);

    const posterFile = new File(['dummy poster content'], 'poster.webp', { type: 'image/webp' });
    const event = { target: { files: [posterFile] } } as unknown as Event;

    component.onPosterSelected(event);

    const uploadReq = httpTesting.expectOne(`${environment.apiUrl}/admin/institutional/home/poster`);
    expect(uploadReq.request.method).toBe('PUT');
    uploadReq.flush({ posterUrl: 'https://storage.wb.agency/new-poster.webp' });

    expect(component.currentPosterUrl).toBe('https://storage.wb.agency/new-poster.webp');
    expect(component.feedbackMessage).toContain('atualizado com sucesso');
  });

  it('deve rejeitar imagem poster com tamanho superior a 10MB', () => {
    fixture.detectChanges();
    httpTesting.expectOne(`${environment.apiUrl}/admin/institutional/home`).flush(mockHomeData);

    const bigBlob = new Blob([new Uint8Array(11 * 1024 * 1024)]);
    const bigFile = new File([bigBlob], 'huge.jpg', { type: 'image/jpeg' });
    const event = { target: { files: [bigFile] } } as unknown as Event;

    component.onPosterSelected(event);

    httpTesting.expectNone(`${environment.apiUrl}/admin/institutional/home/poster`);
    expect(component.feedbackMessage).toContain('10MB');
  });
});

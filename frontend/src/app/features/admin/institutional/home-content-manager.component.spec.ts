import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { provideRouter } from '@angular/router';
import { HttpEventType } from '@angular/common/http';
import { HomeContentManagerComponent } from './home-content-manager.component';
import { environment } from '../../../../environments/environment';

describe('HomeContentManagerComponent', () => {
  let component: HomeContentManagerComponent;
  let fixture: ComponentFixture<HomeContentManagerComponent>;
  let httpTesting: HttpTestingController;

  const mockHomeData = {
    heroTitle: 'WB AGENCY',
    heroSubtitle: 'EDITORIAL & HIGH FASHION SCOUTING',
    scrollLabel: 'SCROLL',
    metaTitle: 'WB Agency | Scouting Internacional e Alta Moda',
    metaDescription: 'Agência de scouting e modelos com foco editorial.',
    videoUrl: 'https://storage.wb.agency/hero.mp4',
    bannerImageUrl: 'https://storage.wb.agency/poster.webp',
    disclaimerActive: true,
    disclaimerTitle: 'AVISO IMPORTANTE',
    disclaimerText: 'Casting internacional aberto.',
    disclaimerLinkUrl: 'https://wb.agency/casting',
    disclaimerLinkLabel: 'PARTICIPAR'
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HomeContentManagerComponent, ReactiveFormsModule, HttpClientTestingModule],
      providers: [provideRouter([])]
    }).compileComponents();

    fixture = TestBed.createComponent(HomeContentManagerComponent);
    component = fixture.componentInstance;
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  function flushAdminSettings(data: any = mockHomeData) {
    const reqs = httpTesting.match((r) => r.url.startsWith(`${environment.apiUrl}/admin/institutional/home`));
    reqs.forEach((r) => r.flush(data));
  }

  it('deve carregar os dados vigentes da Home via GET /admin/institutional/home', () => {
    fixture.detectChanges();

    flushAdminSettings();

    expect(component.form.get('heroTitle')?.value).toBe('WB AGENCY');
    expect(component.form.get('heroSubtitle')?.value).toBe('EDITORIAL & HIGH FASHION SCOUTING');
    expect(component.form.get('scrollLabel')?.value).toBe('SCROLL');
    expect(component.form.get('disclaimerActive')?.value).toBeTrue();
    expect(component.form.get('disclaimerTitle')?.value).toBe('AVISO IMPORTANTE');
    expect(component.form.get('disclaimerText')?.value).toBe('Casting internacional aberto.');
    expect(component.currentVideoUrl).toBe('https://storage.wb.agency/hero.mp4');
    expect(component.currentPosterUrl).toBe('https://storage.wb.agency/poster.webp');
  });

  it('deve atualizar textos institucionais e Disclaimer da Home via PUT', () => {
    fixture.detectChanges();
    flushAdminSettings();

    component.form.patchValue({
      heroTitle: 'NOVO TITULO',
      scrollLabel: 'EXPLORAR',
      disclaimerActive: true,
      disclaimerTitle: 'ALERTA ATUALIZADO',
      disclaimerText: 'Novas inscrições abertas.'
    });

    component.saveTextContent();
    expect(component.isSaving).toBeTrue();

    const putReq = httpTesting.expectOne(`${environment.apiUrl}/admin/institutional/home`);
    expect(putReq.request.method).toBe('PUT');
    expect(putReq.request.body.heroTitle).toBe('NOVO TITULO');
    expect(putReq.request.body.scrollLabel).toBe('EXPLORAR');
    expect(putReq.request.body.disclaimerActive).toBeTrue();
    expect(putReq.request.body.disclaimerTitle).toBe('ALERTA ATUALIZADO');

    putReq.flush({ ...mockHomeData, heroTitle: 'NOVO TITULO', scrollLabel: 'EXPLORAR' });

    expect(component.isSaving).toBeFalse();
    expect(component.feedbackMessage).toContain('salvas com sucesso');
  });

  it('deve realizar upload do vídeo hero com monitoramento de progresso', () => {
    fixture.detectChanges();
    flushAdminSettings();

    const videoFile = new File(['dummy video content'], 'hero.mp4', { type: 'video/mp4' });
    const event = { target: { files: [videoFile] } } as unknown as Event;

    component.onVideoSelected(event);
    expect(component.isVideoUploading).toBeTrue();

    const uploadReq = httpTesting.expectOne(`${environment.apiUrl}/admin/institutional/home/video`);
    expect(uploadReq.request.method).toBe('PUT');

    uploadReq.event({
      type: HttpEventType.UploadProgress,
      loaded: 50,
      total: 100
    });
    expect(component.videoUploadProgress).toBe(50);

    uploadReq.flush({ videoUrl: 'https://storage.wb.agency/new-hero.mp4' });

    expect(component.isVideoUploading).toBeFalse();
    expect(component.currentVideoUrl).toBe('https://storage.wb.agency/new-hero.mp4');
    expect(component.feedbackMessage).toContain('atualizado com sucesso');
  });

  it('deve rejeitar vídeo com tamanho superior a 50MB', () => {
    fixture.detectChanges();
    flushAdminSettings();

    const bigBlob = new Blob([new Uint8Array(51 * 1024 * 1024)]);
    const bigFile = new File([bigBlob], 'huge.mp4', { type: 'video/mp4' });
    const event = { target: { files: [bigFile] } } as unknown as Event;

    component.onVideoSelected(event);

    httpTesting.expectNone(`${environment.apiUrl}/admin/institutional/home/video`);
    expect(component.feedbackMessage).toContain('50MB');
  });

  it('deve realizar upload do poster de contingência', () => {
    fixture.detectChanges();
    flushAdminSettings();

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
    flushAdminSettings();

    const bigBlob = new Blob([new Uint8Array(11 * 1024 * 1024)]);
    const bigFile = new File([bigBlob], 'huge.jpg', { type: 'image/jpeg' });
    const event = { target: { files: [bigFile] } } as unknown as Event;

    component.onPosterSelected(event);

    httpTesting.expectNone(`${environment.apiUrl}/admin/institutional/home/poster`);
    expect(component.feedbackMessage).toContain('10MB');
  });
});

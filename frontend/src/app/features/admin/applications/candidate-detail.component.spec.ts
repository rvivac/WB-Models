import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { CandidateDetailComponent } from './candidate-detail.component';
import { CandidateDetail } from './candidate-detail.model';
import { environment } from '../../../../environments/environment';

describe('CandidateDetailComponent', () => {
  let component: CandidateDetailComponent;
  let fixture: ComponentFixture<CandidateDetailComponent>;
  let httpTesting: HttpTestingController;
  let routerSpy: jasmine.SpyObj<Router>;

  const mockCandidate: CandidateDetail = {
    id: 'test-123',
    fullName: 'Mariana Souza Fagundes',
    email: 'mariana.souza@email.com',
    phone: '+55 11 98888-7777',
    instagram: '@marianasouza',
    birthDate: '2008-05-14',
    age: 18,
    isMinor: false,
    city: 'São Paulo',
    state: 'SP',
    biometrics: {
      height: 178,
      bust: 83,
      waist: 59,
      hips: 88,
      shoes: 37,
      eyes: 'Castanho Claro',
      hair: 'Castanho Natural'
    },
    photos: [
      { id: '1', url: 'https://example.com/photo1.jpg', type: 'POLAROID_ROSTO', fileName: 'rosto.jpg', fileSizeBytes: 1200000 },
      { id: '2', url: 'https://example.com/photo2.jpg', type: 'CORPO_INTEIRO', fileName: 'corpo.jpg', fileSizeBytes: 1500000 }
    ],
    status: 'PENDING',
    internalNotes: 'Perfil interessante',
    lgpdConsent: true,
    lgpdConsentAt: '2026-09-29T14:32:00Z',
    submittedAt: '2026-09-29T14:32:00Z'
  };

  beforeEach(async () => {
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    await TestBed.configureTestingModule({
      imports: [CandidateDetailComponent, HttpClientTestingModule],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: {
                get: (key: string) => (key === 'id' ? 'test-123' : null)
              }
            }
          }
        },
        { provide: Router, useValue: routerSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CandidateDetailComponent);
    component = fixture.componentInstance;
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('deve carregar os detalhes do candidato com sucesso via GET', () => {
    fixture.detectChanges();

    const req = httpTesting.expectOne(`${environment.apiUrl}/admin/applications/test-123`);
    expect(req.request.method).toBe('GET');
    req.flush(mockCandidate);

    expect(component.candidate).toBeTruthy();
    expect(component.candidate?.fullName).toBe('Mariana Souza Fagundes');
    expect(component.internalNotes).toBe('Perfil interessante');
  });

  it('deve acionar fallback mock caso a API retorne erro', () => {
    fixture.detectChanges();

    const req = httpTesting.expectOne(`${environment.apiUrl}/admin/applications/test-123`);
    req.flush('Error loading', { status: 500, statusText: 'Server Error' });

    expect(component.candidate).toBeTruthy();
    expect(component.candidate?.id).toBe('test-123');
    expect(component.candidate?.biometrics.height).toBe(178);
  });

  it('deve atualizar status para APPROVED via PATCH /decision', () => {
    fixture.detectChanges();
    httpTesting.expectOne(`${environment.apiUrl}/admin/applications/test-123`).flush(mockCandidate);

    component.updateStatus('APPROVED');
    expect(component.isProcessing).toBeTrue();

    const patchReq = httpTesting.expectOne(`${environment.apiUrl}/admin/applications/test-123/decision`);
    expect(patchReq.request.method).toBe('PATCH');
    expect(patchReq.request.body).toEqual({
      status: 'APPROVED',
      internalNotes: 'Perfil interessante'
    });
    patchReq.flush({ ...mockCandidate, status: 'APPROVED' });

    expect(component.candidate?.status).toBe('APPROVED');
    expect(component.isProcessing).toBeFalse();
  });

  it('deve salvar anotações internas via PATCH /decision', () => {
    fixture.detectChanges();
    httpTesting.expectOne(`${environment.apiUrl}/admin/applications/test-123`).flush(mockCandidate);

    component.internalNotes = 'Nova anotação confidencial';
    component.saveNotes();

    const patchReq = httpTesting.expectOne(`${environment.apiUrl}/admin/applications/test-123/decision`);
    expect(patchReq.request.method).toBe('PATCH');
    expect(patchReq.request.body).toEqual({
      status: 'PENDING',
      internalNotes: 'Nova anotação confidencial'
    });
    patchReq.flush({ ...mockCandidate, internalNotes: 'Nova anotação confidencial' });

    expect(component.isSavingNotes).toBeFalse();
  });

  it('deve gerenciar navegação e zoom do lightbox de fotos', () => {
    fixture.detectChanges();
    httpTesting.expectOne(`${environment.apiUrl}/admin/applications/test-123`).flush(mockCandidate);

    component.openLightbox(0);
    expect(component.isLightboxOpen).toBeTrue();
    expect(component.activePhotoIndex).toBe(0);
    expect(component.currentZoom).toBe(1);

    component.toggleZoom();
    expect(component.currentZoom).toBe(2);

    component.toggleZoom();
    expect(component.currentZoom).toBe(3);

    component.toggleZoom();
    expect(component.currentZoom).toBe(1);

    component.nextPhoto();
    expect(component.activePhotoIndex).toBe(1);

    component.prevPhoto();
    expect(component.activePhotoIndex).toBe(0);

    // Testar keydown de Escape e ArrowRight/Left
    component.handleKeyDown(new KeyboardEvent('keydown', { key: 'ArrowRight' }));
    expect(component.activePhotoIndex).toBe(1);

    component.handleKeyDown(new KeyboardEvent('keydown', { key: 'ArrowLeft' }));
    expect(component.activePhotoIndex).toBe(0);

    component.handleKeyDown(new KeyboardEvent('keydown', { key: 'Escape' }));
    expect(component.isLightboxOpen).toBeFalse();
  });

  it('não deve executar exclusão se o texto digitado for diferente de EXCLUIR', () => {
    fixture.detectChanges();
    httpTesting.expectOne(`${environment.apiUrl}/admin/applications/test-123`).flush(mockCandidate);

    component.openDeleteConfirmation();
    expect(component.isDeleteModalOpen).toBeTrue();

    component.deleteConfirmInput = 'CONFIRMAR';
    component.executeSecureDelete();

    httpTesting.expectNone(`${environment.apiUrl}/admin/applications/test-123`);
    expect(component.isDeleting).toBeFalse();
  });

  it('deve executar exclusão permanente LGPD quando digitado EXCLUIR e navegar para /admin/candidaturas', () => {
    fixture.detectChanges();
    httpTesting.expectOne(`${environment.apiUrl}/admin/applications/test-123`).flush(mockCandidate);

    component.openDeleteConfirmation();
    component.deleteConfirmInput = 'EXCLUIR';
    component.executeSecureDelete();

    expect(component.isDeleting).toBeTrue();

    const deleteReq = httpTesting.expectOne(`${environment.apiUrl}/admin/applications/test-123`);
    expect(deleteReq.request.method).toBe('DELETE');
    deleteReq.flush(null, { status: 204, statusText: 'No Content' });

    expect(component.isDeleting).toBeFalse();
    expect(component.isDeleteModalOpen).toBeFalse();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/admin/candidaturas']);
  });

  it('deve sanitizar telefone e instagram e mapear legendas das fotos', () => {
    expect(component.cleanPhone('+55 (11) 98888-7777')).toBe('5511988887777');
    expect(component.cleanInstagram('@talento_fashion')).toBe('talento_fashion');
    expect(component.formatPhotoType('POLAROID_ROSTO')).toBe('Rosto Frontal Natural');
    expect(component.formatPhotoType('CORPO_INTEIRO')).toBe('Corpo Inteiro');
  });
});

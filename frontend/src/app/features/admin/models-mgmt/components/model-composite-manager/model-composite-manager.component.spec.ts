import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ModelCompositeManagerComponent, ModelComposite } from './model-composite-manager.component';
import { environment } from '../../../../../../environments/environment';

describe('ModelCompositeManagerComponent', () => {
  let component: ModelCompositeManagerComponent;
  let fixture: ComponentFixture<ModelCompositeManagerComponent>;
  let httpMock: HttpTestingController;

  const mockModelId = '123e4567-e89b-12d3-a456-426614174000';
  const mockComposite: ModelComposite = {
    id: 'comp-uuid-1',
    fileUrl: 'https://example.com/models/123/composite/sedcard.pdf',
    fileName: 'sedcard.pdf',
    fileType: 'PDF',
    fileSizeBytes: 4512300,
    updatedAt: '2026-09-29T10:00:00Z'
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ModelCompositeManagerComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ModelCompositeManagerComponent);
    component = fixture.componentInstance;
    component.modelId = mockModelId;
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('deve criar o componente', () => {
    fixture.detectChanges();
    const req = httpMock.expectOne(`${environment.apiUrl}/admin/models/${mockModelId}/composite`);
    expect(req.request.method).toBe('GET');
    req.flush(null);

    expect(component).toBeTruthy();
  });

  it('deve carregar composite existente no ngOnInit', () => {
    fixture.detectChanges();

    const req = httpMock.expectOne(`${environment.apiUrl}/admin/models/${mockModelId}/composite`);
    expect(req.request.method).toBe('GET');
    req.flush(mockComposite);

    expect(component.composite).toEqual(mockComposite);
  });

  it('deve formatar bytes corretamente', () => {
    expect(component.formatBytes(0)).toBe('0 B');
    expect(component.formatBytes(1024)).toBe('1 KB');
    expect(component.formatBytes(4512300)).toBe('4.3 MB');
  });

  it('deve rejeitar arquivo com mais de 25MB', () => {
    spyOn(window, 'alert');
    fixture.detectChanges();
    httpMock.expectOne(`${environment.apiUrl}/admin/models/${mockModelId}/composite`).flush(null);

    const oversizedBlob = new Blob(['a'.repeat(100)], { type: 'application/pdf' });
    const file = new File([oversizedBlob], 'large.pdf', { type: 'application/pdf' });
    Object.defineProperty(file, 'size', { value: 26 * 1024 * 1024 });

    component.uploadComposite(file);

    expect(window.alert).toHaveBeenCalledWith('O arquivo selecionado excede o limite máximo permitido de 25MB.');
    expect(component.isUploading).toBeFalse();
  });

  it('deve rejeitar arquivo com formato inválido', () => {
    spyOn(window, 'alert');
    fixture.detectChanges();
    httpMock.expectOne(`${environment.apiUrl}/admin/models/${mockModelId}/composite`).flush(null);

    const textBlob = new Blob(['hello'], { type: 'text/plain' });
    const file = new File([textBlob], 'document.txt', { type: 'text/plain' });

    component.uploadComposite(file);

    expect(window.alert).toHaveBeenCalledWith('Formato de arquivo inválido. Formatos permitidos: PDF, JPG, PNG e WEBP.');
    expect(component.isUploading).toBeFalse();
  });

  it('deve realizar upload e substituir atomicamente o composite', () => {
    fixture.detectChanges();
    httpMock.expectOne(`${environment.apiUrl}/admin/models/${mockModelId}/composite`).flush(null);

    const validBlob = new Blob(['dummy pdf'], { type: 'application/pdf' });
    const file = new File([validBlob], 'composite.pdf', { type: 'application/pdf' });

    component.uploadComposite(file);

    expect(component.isUploading).toBeTrue();

    const putReq = httpMock.expectOne(`${environment.apiUrl}/admin/models/${mockModelId}/composite`);
    expect(putReq.request.method).toBe('PUT');
    putReq.flush(mockComposite);

    expect(component.composite).toEqual(mockComposite);
    expect(component.isUploading).toBeFalse();
  });

  it('deve remover composite ao confirmar exclusão', () => {
    spyOn(window, 'confirm').and.returnValue(true);
    fixture.detectChanges();
    httpMock.expectOne(`${environment.apiUrl}/admin/models/${mockModelId}/composite`).flush(mockComposite);

    component.removeComposite();

    const deleteReq = httpMock.expectOne(`${environment.apiUrl}/admin/models/${mockModelId}/composite`);
    expect(deleteReq.request.method).toBe('DELETE');
    deleteReq.flush(null);

    expect(component.composite).toBeNull();
  });
});

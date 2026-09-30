import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AdminModelService } from './admin-model.service';
import { ModelAdminItem, ModelFormData } from '../../shared/models/admin-model.interface';
import { environment } from '../../../environments/environment';

describe('AdminModelService', () => {
  let service: AdminModelService;
  let httpMock: HttpTestingController;
  const baseUrl = `${environment.apiUrl}/admin/models`;

  const dummyModel: ModelAdminItem = {
    id: 'test-uuid-1',
    stageName: 'Gisele Bundchen',
    gender: 'FEMALE',
    isStar: true,
    isFeaturedHome: true,
    featuredOrder: 1,
    isActive: true,
    heightCm: 180,
    bustChestCm: 86,
    waistCm: 60,
    hipsCm: 89,
    city: 'Horizontina, RS',
    nationality: 'Brasileira'
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        AdminModelService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });

    service = TestBed.inject(AdminModelService);
    httpMock = TestBed.inject(HttpTestingController);
    localStorage.clear();
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('deve ser instanciado corretamente', () => {
    expect(service).toBeTruthy();
  });

  it('deve listar modelos via GET /admin/models', () => {
    service.getModels({ gender: 'FEMALE', isStar: true }).subscribe((res) => {
      expect(res.content.length).toBe(1);
      expect(res.content[0].stageName).toBe('Gisele Bundchen');
    });

    const req = httpMock.expectOne((r) => r.url === baseUrl && r.params.has('gender') && r.params.get('gender') === 'FEMALE');
    expect(req.request.method).toBe('GET');
    req.flush({
      content: [dummyModel],
      totalElements: 1,
      totalPages: 1,
      size: 20,
      number: 0
    });
  });

  it('deve buscar modelo por ID via GET /admin/models/{id}', () => {
    service.getModelById('test-uuid-1').subscribe((res) => {
      expect(res.id).toBe('test-uuid-1');
      expect(res.stageName).toBe('Gisele Bundchen');
    });

    const req = httpMock.expectOne(`${baseUrl}/test-uuid-1`);
    expect(req.request.method).toBe('GET');
    req.flush(dummyModel);
  });

  it('deve criar novo modelo via POST /admin/models', () => {
    const formData: ModelFormData = {
      stageName: 'Novos Talentos',
      gender: 'MALE',
      isStar: false,
      isFeaturedHome: false,
      isActive: true,
      heightCm: 186
    };

    service.createModel(formData).subscribe((res) => {
      expect(res.stageName).toBe('Novos Talentos');
      expect(res.gender).toBe('MALE');
    });

    const req = httpMock.expectOne(baseUrl);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.stageName).toBe('Novos Talentos');
    req.flush({ ...formData, id: 'created-uuid' });
  });

  it('deve atualizar modelo via PUT /admin/models/{id}', () => {
    const updateData: ModelFormData = {
      stageName: 'Gisele B. Atualizada',
      gender: 'FEMALE',
      isStar: true,
      isFeaturedHome: true,
      isActive: true,
      heightCm: 181
    };

    service.updateModel('test-uuid-1', updateData).subscribe((res) => {
      expect(res.stageName).toBe('Gisele B. Atualizada');
    });

    const req = httpMock.expectOne(`${baseUrl}/test-uuid-1`);
    expect(req.request.method).toBe('PUT');
    req.flush({ ...updateData, id: 'test-uuid-1' });
  });

  it('deve atualizar Star via PATCH /admin/models/{id}/star', () => {
    service.updateStar('test-uuid-1', true).subscribe((res) => {
      expect(res.isStar).toBeTrue();
    });

    const req = httpMock.expectOne(`${baseUrl}/test-uuid-1/star`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ isStar: true });
    req.flush({ ...dummyModel, isStar: true });
  });

  it('deve acionar fallback mock quando o backend estiver inacessível no getModels', () => {
    service.getModels().subscribe((res) => {
      expect(res).toBeTruthy();
      expect(res.content.length).toBeGreaterThan(0);
      expect(res.content[0].stageName).toBe('Isabella Fontana');
    });

    const req = httpMock.expectOne((r) => r.url === baseUrl);
    req.error(new ProgressEvent('Network error'), { status: 0, statusText: 'Unknown Error' });
  });
});

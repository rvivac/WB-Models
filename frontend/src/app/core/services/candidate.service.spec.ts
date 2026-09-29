import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { CandidateService } from './candidate.service';
import { environment } from '../../../environments/environment';

describe('CandidateService', () => {
  let service: CandidateService;
  let httpMock: HttpTestingController;
  const baseUrl = environment.apiUrl;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        CandidateService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });

    service = TestBed.inject(CandidateService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('deve ser instanciado com sucesso', () => {
    expect(service).toBeTruthy();
  });

  it('deve consultar candidaturas paginadas da API', () => {
    const mockApiResponse = {
      content: [
        {
          id: 'cand-1',
          fullName: 'Helena Ribeiro',
          status: 'PENDING',
          height: 177,
          bust: 83,
          waist: 61,
          hips: 89,
          age: 19
        }
      ],
      totalElements: 1,
      totalPages: 1,
      size: 12,
      number: 0
    };

    service.getCandidates({ status: 'PENDING', page: 0, size: 12 }).subscribe(res => {
      expect(res.content.length).toBe(1);
      expect(res.content[0].fullName).toBe('Helena Ribeiro');
      expect(res.content[0].status).toBe('PENDING');
    });

    const req = httpMock.expectOne(`${baseUrl}/api/v1/admin/candidates?page=0&size=12&status=PENDING`);
    expect(req.request.method).toBe('GET');
    req.flush(mockApiResponse);
  });

  it('deve ativar fallback offline de 6 candidatos em caso de erro HTTP', () => {
    service.getCandidates({ status: 'ALL', page: 0, size: 12 }).subscribe(res => {
      expect(res.content.length).toBe(6);
      expect(res.totalElements).toBe(6);
      expect(res.content[0].fullName).toBe('Valentina Rocha');
      expect(res.content[0].isMinor).toBeTrue();
    });

    const req = httpMock.expectOne(`${baseUrl}/api/v1/admin/candidates?page=0&size=12`);
    req.error(new ProgressEvent('Network error'), { status: 0, statusText: 'Unknown Error' });
  });

  it('deve filtrar os candidatos mockados por status quando offline', () => {
    service.getCandidates({ status: 'APPROVED', page: 0, size: 12 }).subscribe(res => {
      expect(res.content.every(c => c.status === 'APPROVED')).toBeTrue();
    });

    const req = httpMock.expectOne(`${baseUrl}/api/v1/admin/candidates?page=0&size=12&status=APPROVED`);
    req.error(new ProgressEvent('Not Found'), { status: 404, statusText: 'Not Found' });
  });

  it('deve atualizar status da candidatura via PATCH', () => {
    service.updateStatus('cand-1', { status: 'APPROVED', notes: 'Aprovada para passarela' }).subscribe(res => {
      expect(res.status).toBe('APPROVED');
    });

    const req = httpMock.expectOne(`${baseUrl}/api/v1/admin/candidates/cand-1/status`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body.status).toBe('APPROVED');
    req.flush({
      id: 'cand-1',
      fullName: 'Helena Ribeiro',
      status: 'APPROVED',
      notes: 'Aprovada para passarela'
    });
  });

  it('deve promover candidatura aprovada para modelo via POST', () => {
    service.promoteToModel('cand-1', true).subscribe(res => {
      expect(res.convertedToModelId).toBe('model-123');
    });

    const req = httpMock.expectOne(`${baseUrl}/api/v1/admin/candidates/cand-1/promote-to-model?activateImmediately=true`);
    expect(req.request.method).toBe('POST');
    req.flush({
      id: 'cand-1',
      fullName: 'Helena Ribeiro',
      status: 'APPROVED',
      convertedToModelId: 'model-123'
    });
  });
});

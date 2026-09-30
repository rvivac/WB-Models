import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { CandidateTableComponent, CandidateApplicationRow } from './candidate-table.component';
import { environment } from '../../../../environments/environment';

describe('CandidateTableComponent', () => {
  let component: CandidateTableComponent;
  let fixture: ComponentFixture<CandidateTableComponent>;
  let httpMock: HttpTestingController;
  let router: Router;

  const mockRows: CandidateApplicationRow[] = [
    {
      id: 'c1a2b3c4-0001',
      fullName: 'Mariana Souza Fagundes',
      email: 'mariana.souza@email.com',
      phone: '+55 11 98888-7777',
      birthDate: '2008-05-14',
      age: 18,
      isMinor: false,
      city: 'São Paulo',
      state: 'SP',
      height: 178,
      bust: 83,
      waist: 59,
      hips: 88,
      shoes: 37,
      status: 'PENDING',
      hasPhotos: true,
      polaroidsCount: 4,
      createdAt: '2026-09-29T14:32:00Z'
    },
    {
      id: 'c1a2b3c4-0002',
      fullName: 'Lucas Gabriel Silveira',
      email: 'lucas.silveira@email.com',
      phone: '+55 21 97777-6666',
      birthDate: '2009-08-20',
      age: 17,
      isMinor: true,
      city: 'Niterói',
      state: 'RJ',
      height: 187,
      bust: 96,
      waist: 76,
      hips: 95,
      shoes: 42,
      status: 'PENDING',
      hasPhotos: true,
      polaroidsCount: 5,
      createdAt: '2026-09-29T11:15:00Z'
    }
  ];

  const mockPageResponse = {
    content: mockRows,
    totalElements: 2,
    totalPages: 1,
    size: 15,
    number: 0
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CandidateTableComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: Router,
          useValue: {
            navigate: jasmine.createSpy('navigate')
          }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CandidateTableComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('deve instanciar o componente e carregar dados na inicialização', () => {
    fixture.detectChanges();

    const req = httpMock.expectOne((r) => r.url === `${environment.apiUrl}/admin/applications`);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('page')).toBe('0');
    expect(req.request.params.get('size')).toBe('15');
    expect(req.request.params.get('sortBy')).toBe('createdAt');
    expect(req.request.params.get('sortDirection')).toBe('DESC');
    req.flush(mockPageResponse);

    const countsReq = httpMock.expectOne(`${environment.apiUrl}/admin/applications/counts`);
    countsReq.flush({ pending: 5, approved: 10, rejected: 2, total: 17 });

    expect(component).toBeTruthy();
    expect(component.applications.length).toBe(2);
    expect(component.pendingCount).toBe(5);
  });

  it('deve filtrar por status e resetar a página', () => {
    fixture.detectChanges();
    httpMock.expectOne((r) => r.url === `${environment.apiUrl}/admin/applications`).flush(mockPageResponse);
    httpMock.expectOne(`${environment.apiUrl}/admin/applications/counts`).flush({});

    component.pageIndex = 2;
    component.setStatusFilter('APPROVED');

    expect(component.pageIndex).toBe(0);
    expect(component.selectedStatus).toBe('APPROVED');

    const req = httpMock.expectOne((r) => r.url === `${environment.apiUrl}/admin/applications`);
    expect(req.request.params.get('status')).toBe('APPROVED');
    req.flush(mockPageResponse);
  });

  it('deve alternar ordenação ao clicar no cabeçalho', () => {
    fixture.detectChanges();
    httpMock.expectOne((r) => r.url === `${environment.apiUrl}/admin/applications`).flush(mockPageResponse);
    httpMock.expectOne(`${environment.apiUrl}/admin/applications/counts`).flush({});

    // Clica na mesma coluna 'createdAt' -> inverte para ASC
    component.toggleSort('createdAt');
    expect(component.sortDirection).toBe('ASC');
    httpMock.expectOne((r) => r.url === `${environment.apiUrl}/admin/applications`).flush(mockPageResponse);

    // Clica em outra coluna 'height' -> muda para DESC
    component.toggleSort('height');
    expect(component.sortBy).toBe('height');
    expect(component.sortDirection).toBe('DESC');
    httpMock.expectOne((r) => r.url === `${environment.apiUrl}/admin/applications`).flush(mockPageResponse);
  });

  it('deve navegar para a próxima página com goToPage', () => {
    fixture.detectChanges();
    httpMock.expectOne((r) => r.url === `${environment.apiUrl}/admin/applications`).flush(mockPageResponse);
    httpMock.expectOne(`${environment.apiUrl}/admin/applications/counts`).flush({});

    component.goToPage(1);
    expect(component.pageIndex).toBe(1);

    const req = httpMock.expectOne((r) => r.url === `${environment.apiUrl}/admin/applications`);
    expect(req.request.params.get('page')).toBe('1');
    req.flush(mockPageResponse);
  });

  it('deve disparar busca com debounce ao digitar', fakeAsync(() => {
    fixture.detectChanges();
    httpMock.expectOne((r) => r.url === `${environment.apiUrl}/admin/applications`).flush(mockPageResponse);
    httpMock.expectOne(`${environment.apiUrl}/admin/applications/counts`).flush({});

    component.onSearchChange('Mariana');
    tick(400);

    const req = httpMock.expectOne((r) => r.url === `${environment.apiUrl}/admin/applications`);
    expect(req.request.params.get('search')).toBe('Mariana');
    req.flush(mockPageResponse);
  }));

  it('deve abrir detalhes via openDetail', () => {
    component.openDetail('c1a2b3c4-0001');
    expect(router.navigate).toHaveBeenCalledWith(['/admin/candidaturas', 'c1a2b3c4-0001']);
  });

  it('deve carregar mock fallback se API falhar', () => {
    fixture.detectChanges();
    httpMock.expectOne((r) => r.url === `${environment.apiUrl}/admin/applications`).error(new ProgressEvent('error'));
    httpMock.expectOne(`${environment.apiUrl}/admin/applications/counts`).flush({});

    expect(component.applications.length).toBe(2);
    expect(component.totalElements).toBe(2);
  });
});

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { FeaturedModelsManagerComponent, FeaturedModelSummary } from './featured-models-manager.component';
import { environment } from '../../../../environments/environment';

describe('FeaturedModelsManagerComponent', () => {
  let component: FeaturedModelsManagerComponent;
  let fixture: ComponentFixture<FeaturedModelsManagerComponent>;
  let httpMock: HttpTestingController;

  const mockFeatured: FeaturedModelSummary[] = [
    { id: '1', artisticName: 'Isabella Viana', category: 'FASHION', height: 179, isStar: true, coverPhotoUrl: 'https://example.com/1.jpg', displayOrder: 1 },
    { id: '2', artisticName: 'Lucas Albuquerque', category: 'COMMERCIAL', height: 188, isStar: false, coverPhotoUrl: 'https://example.com/2.jpg', displayOrder: 2 },
    { id: '3', artisticName: 'Beatriz Zanin', category: 'FASHION', height: 177, isStar: true, coverPhotoUrl: 'https://example.com/3.jpg', displayOrder: 3 },
    { id: '4', artisticName: 'Gabriel Siqueira', category: 'COMMERCIAL', height: 186, isStar: false, coverPhotoUrl: 'https://example.com/4.jpg', displayOrder: 4 }
  ];

  const mockAvailable: FeaturedModelSummary[] = [
    ...mockFeatured,
    { id: '5', artisticName: 'Camila Rocha', category: 'FASHION', height: 180, isStar: true, coverPhotoUrl: 'https://example.com/5.jpg' },
    { id: '6', artisticName: 'Helena Vasconcelos', category: 'COMMERCIAL', height: 177, isStar: false, coverPhotoUrl: 'https://example.com/6.jpg' }
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FeaturedModelsManagerComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(FeaturedModelsManagerComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('deve instanciar o componente e carregar dados na inicialização', () => {
    fixture.detectChanges();

    const reqFeatured = httpMock.expectOne(`${environment.apiUrl}/admin/featured-models`);
    expect(reqFeatured.request.method).toBe('GET');
    reqFeatured.flush(mockFeatured);

    const reqAvailable = httpMock.expectOne(`${environment.apiUrl}/admin/models?status=ACTIVE&size=100`);
    expect(reqAvailable.request.method).toBe('GET');
    reqAvailable.flush(mockAvailable);

    expect(component).toBeTruthy();
    expect(component.featuredList.length).toBe(4);
    expect(component.availableModels.length).toBe(6);
  });

  it('deve filtrar modelos disponíveis excluindo os já destacados e respeitando busca', () => {
    fixture.detectChanges();
    httpMock.expectOne(`${environment.apiUrl}/admin/featured-models`).flush(mockFeatured);
    httpMock.expectOne(`${environment.apiUrl}/admin/models?status=ACTIVE&size=100`).flush(mockAvailable);

    // Sem busca: modelos 5 e 6 disponíveis
    expect(component.filteredAvailableModels.length).toBe(2);
    expect(component.filteredAvailableModels.map(m => m.id)).toEqual(['5', '6']);

    // Com busca: apenas Camila Rocha
    component.searchQuery = 'Camila';
    expect(component.filteredAvailableModels.length).toBe(1);
    expect(component.filteredAvailableModels[0].artisticName).toBe('Camila Rocha');
  });

  it('deve adicionar modelo à lista de destaques respeitando limite máximo de 8', () => {
    fixture.detectChanges();
    httpMock.expectOne(`${environment.apiUrl}/admin/featured-models`).flush(mockFeatured);
    httpMock.expectOne(`${environment.apiUrl}/admin/models?status=ACTIVE&size=100`).flush(mockAvailable);

    const newModel: FeaturedModelSummary = {
      id: '5',
      artisticName: 'Camila Rocha',
      category: 'FASHION',
      height: 180,
      isStar: true,
      coverPhotoUrl: 'https://example.com/5.jpg'
    };

    component.addToFeatured(newModel);
    expect(component.featuredList.length).toBe(5);
    expect(component.featuredList[4].id).toBe('5');

    // Preenche até 8
    component.addToFeatured({ ...newModel, id: '6' });
    component.addToFeatured({ ...newModel, id: '7' });
    component.addToFeatured({ ...newModel, id: '8' });
    expect(component.featuredList.length).toBe(8);

    // Tenta adicionar 9º item: não deve permitir
    component.addToFeatured({ ...newModel, id: '9' });
    expect(component.featuredList.length).toBe(8);
  });

  it('deve remover modelo da lista de destaques', () => {
    fixture.detectChanges();
    httpMock.expectOne(`${environment.apiUrl}/admin/featured-models`).flush(mockFeatured);
    httpMock.expectOne(`${environment.apiUrl}/admin/models?status=ACTIVE&size=100`).flush(mockAvailable);

    component.removeFromFeatured('2');
    expect(component.featuredList.length).toBe(3);
    expect(component.featuredList.find(m => m.id === '2')).toBeUndefined();
  });

  it('não deve salvar se houver menos de 4 modelos', () => {
    spyOn(window, 'alert');
    fixture.detectChanges();
    httpMock.expectOne(`${environment.apiUrl}/admin/featured-models`).flush(mockFeatured.slice(0, 3));
    httpMock.expectOne(`${environment.apiUrl}/admin/models?status=ACTIVE&size=100`).flush(mockAvailable);

    component.saveChanges();
    expect(window.alert).toHaveBeenCalledWith('A vitrine deve conter entre 4 e 8 modelos selecionados.');
    expect(component.isSubmitting).toBeFalse();
  });

  it('deve salvar vitrine com PUT /admin/featured-models ao atingir critérios válidos', () => {
    spyOn(window, 'alert');
    fixture.detectChanges();
    httpMock.expectOne(`${environment.apiUrl}/admin/featured-models`).flush(mockFeatured);
    httpMock.expectOne(`${environment.apiUrl}/admin/models?status=ACTIVE&size=100`).flush(mockAvailable);

    component.saveChanges();

    expect(component.isSubmitting).toBeTrue();

    const putReq = httpMock.expectOne(`${environment.apiUrl}/admin/featured-models`);
    expect(putReq.request.method).toBe('PUT');
    expect(putReq.request.body.items.length).toBe(4);
    expect(putReq.request.body.items[0]).toEqual({ modelId: '1', displayOrder: 1 });

    putReq.flush(mockFeatured);

    expect(component.isSubmitting).toBeFalse();
    expect(window.alert).toHaveBeenCalledWith('Curadoria da Home atualizada com sucesso!');
  });
});

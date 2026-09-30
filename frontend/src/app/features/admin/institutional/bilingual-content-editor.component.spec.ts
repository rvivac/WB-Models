import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { FormsModule } from '@angular/forms';
import { RouterTestingModule } from '@angular/router/testing';
import { BilingualContentEditorComponent, SectionTranslationsResponse } from './bilingual-content-editor.component';
import { environment } from '../../../../environments/environment';

describe('BilingualContentEditorComponent', () => {
  let component: BilingualContentEditorComponent;
  let fixture: ComponentFixture<BilingualContentEditorComponent>;
  let httpMock: HttpTestingController;

  const mockResponse: SectionTranslationsResponse = {
    sectionKey: 'ABOUT_MANIFESTO',
    title: 'Manifesto Institucional',
    translations: {
      pt: {
        headline: 'A Nova Estética do Scouting Global',
        quote: 'A beleza contemporânea nasce da singularidade e precisão.',
        body: 'Texto completo em português.'
      },
      en: {
        headline: 'The New Aesthetic of Global Scouting',
        quote: 'Contemporary beauty stems from uniqueness and precision.',
        body: 'Full text in English.'
      }
    }
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        BilingualContentEditorComponent,
        HttpClientTestingModule,
        FormsModule,
        RouterTestingModule
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(BilingualContentEditorComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('deve instanciar o componente e carregar dados da seção padrão', () => {
    fixture.detectChanges();

    const req = httpMock.expectOne(`${environment.apiUrl}/admin/institutional/translations/ABOUT_MANIFESTO`);
    expect(req.request.method).toBe('GET');
    req.flush(mockResponse);

    expect(component).toBeTruthy();
    expect(component.currentSectionTitle).toBe('Manifesto Institucional');
    expect(component.currentContent.pt.headline).toBe('A Nova Estética do Scouting Global');
    expect(component.currentContent.en.headline).toBe('The New Aesthetic of Global Scouting');
    expect(component.editingBlock).toBeNull();
  });

  it('deve iniciar e cancelar o modo de edição de um bloco específico', () => {
    fixture.detectChanges();
    httpMock.expectOne(`${environment.apiUrl}/admin/institutional/translations/ABOUT_MANIFESTO`).flush(mockResponse);

    component.startEditBlock('headline');
    expect(component.editingBlock).toBe('headline');
    expect(component.tempPt.headline).toBe('A Nova Estética do Scouting Global');
    expect(component.tempEn.headline).toBe('The New Aesthetic of Global Scouting');

    component.cancelEditBlock();
    expect(component.editingBlock).toBeNull();
  });

  it('deve alternar a seção ativa no seletor e buscar os dados da nova seção', () => {
    fixture.detectChanges();
    httpMock.expectOne(`${environment.apiUrl}/admin/institutional/translations/ABOUT_MANIFESTO`).flush(mockResponse);

    component.loadSection('SCOUTING');

    const req = httpMock.expectOne(`${environment.apiUrl}/admin/institutional/translations/SCOUTING_GUIDELINES`);
    expect(req.request.method).toBe('GET');
    req.flush({
      sectionKey: 'SCOUTING_GUIDELINES',
      title: 'Diretrizes de Scouting',
      translations: {
        pt: { headline: 'Critérios', quote: '', body: 'Orientações PT' },
        en: { headline: 'Guidelines', quote: '', body: 'Guidelines EN' }
      }
    });

    expect(component.activeSection).toBe('SCOUTING');
    expect(component.currentSectionTitle).toBe('Diretrizes de Scouting');
    expect(component.currentContent.pt.headline).toBe('Critérios');
  });

  it('deve salvar bloco individual via PUT com sucesso e exibir feedback', fakeAsync(() => {
    fixture.detectChanges();
    httpMock.expectOne(`${environment.apiUrl}/admin/institutional/translations/ABOUT_MANIFESTO`).flush(mockResponse);

    component.startEditBlock('headline');
    component.tempPt.headline = 'Novo Título PT';
    component.tempEn.headline = 'New Title EN';

    component.saveBlock('headline');

    const req = httpMock.expectOne(`${environment.apiUrl}/admin/institutional/translations/ABOUT_MANIFESTO`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body.pt.headline).toBe('Novo Título PT');
    expect(req.request.body.en.headline).toBe('New Title EN');
    req.flush({ success: true });

    expect(component.currentContent.pt.headline).toBe('Novo Título PT');
    expect(component.currentContent.en.headline).toBe('New Title EN');
    expect(component.editingBlock).toBeNull();
    expect(component.successBlock).toBe('headline');

    tick(3000);
    expect(component.successBlock).toBeNull();
  }));

  it('deve utilizar fallback mock se a API retornar erro', () => {
    fixture.detectChanges();

    const req = httpMock.expectOne(`${environment.apiUrl}/admin/institutional/translations/ABOUT_MANIFESTO`);
    req.flush('Error', { status: 500, statusText: 'Server Error' });

    expect(component.currentSectionTitle).toBe('Manifesto Institucional');
    expect(component.currentContent.pt.headline).toBe('A Nova Estética do Scouting Global');
  });
});

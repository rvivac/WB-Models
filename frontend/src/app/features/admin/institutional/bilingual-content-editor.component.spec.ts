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
        sectionTitle: 'Nossa Filosofia',
        body: 'Texto completo em português.'
      },
      en: {
        headline: 'The New Aesthetic of Global Scouting',
        quote: 'Contemporary beauty stems from uniqueness and precision.',
        sectionTitle: 'Our Philosophy',
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
    expect(component.currentSectionTitle).toBe('Sobre Nós');
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

  it('deve alternar para a seção SCOUTING e buscar os dados de FAQ', () => {
    fixture.detectChanges();
    httpMock.expectOne(`${environment.apiUrl}/admin/institutional/translations/ABOUT_MANIFESTO`).flush(mockResponse);

    component.loadSection('SCOUTING');

    const req = httpMock.expectOne(`${environment.apiUrl}/admin/apply-faq`);
    expect(req.request.method).toBe('GET');
    req.flush({
      items: [
        {
          question: 'Critérios PT',
          answer: 'Resposta PT',
          questionEn: 'Guidelines EN',
          answerEn: 'Answer EN'
        }
      ]
    });

    expect(component.activeSection).toBe('SCOUTING');
    expect(component.currentSectionTitle).toBe('Configurações do Form. de Quero ser Modelo');
    expect(component.faqList.length).toBe(1);
    expect(component.faqList[0].question).toBe('Critérios PT');
    expect(component.faqList[0].questionEn).toBe('Guidelines EN');
  });

  it('deve gerenciar itens de FAQ: adicionar até 10, mover e remover com trava de segurança', () => {
    fixture.detectChanges();
    httpMock.expectOne(`${environment.apiUrl}/admin/institutional/translations/ABOUT_MANIFESTO`).flush(mockResponse);

    component.activeSection = 'SCOUTING';
    component.faqList = [
      { question: 'Q1', answer: 'A1', questionEn: 'QE1', answerEn: 'AE1' },
      { question: 'Q2', answer: 'A2', questionEn: 'QE2', answerEn: 'AE2' }
    ];

    // Reordenar descer
    component.moveFaqDown(0);
    expect(component.faqList[0].question).toBe('Q2');
    expect(component.faqList[1].question).toBe('Q1');

    // Reordenar subir
    component.moveFaqUp(1);
    expect(component.faqList[0].question).toBe('Q1');
    expect(component.faqList[1].question).toBe('Q2');

    // Adicionar item
    component.addFaqItem();
    expect(component.faqList.length).toBe(3);

    // Remover item
    component.removeFaqItem(2);
    expect(component.faqList.length).toBe(2);

    // Trava de segurança para não deixar lista vazia
    component.removeFaqItem(1);
    expect(component.faqList.length).toBe(1);
    component.removeFaqItem(0); // Não deve remover o último
    expect(component.faqList.length).toBe(1);
  });

  it('deve salvar lista de FAQ via saveFaqList e PUT /admin/apply-faq', fakeAsync(() => {
    fixture.detectChanges();
    httpMock.expectOne(`${environment.apiUrl}/admin/institutional/translations/ABOUT_MANIFESTO`).flush(mockResponse);

    spyOn(window, 'alert');
    component.activeSection = 'SCOUTING';
    component.faqList = [
      { question: 'Pergunta Teste', answer: 'Resposta Teste', questionEn: 'Test Question', answerEn: 'Test Answer' }
    ];

    component.saveFaqList();

    const req = httpMock.expectOne(`${environment.apiUrl}/admin/apply-faq`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body.items.length).toBe(1);
    expect(req.request.body.items[0].question).toBe('Pergunta Teste');
    req.flush({ items: component.faqList });

    const faqReq = httpMock.expectOne(`${environment.apiUrl}/admin/apply-faq`);
    expect(faqReq.request.method).toBe('GET');
    faqReq.flush({ items: component.faqList });

    expect(component.isSaving).toBeFalse();
    expect(component.successBlock).toBe('faq');
    tick(3000);
    expect(component.successBlock).toBeNull();
  }));

  it('deve alternar para a seção PRIVACY e buscar dados de PRIVACY', () => {
    fixture.detectChanges();
    httpMock.expectOne(`${environment.apiUrl}/admin/institutional/translations/ABOUT_MANIFESTO`).flush(mockResponse);

    component.loadSection('PRIVACY');

    const req = httpMock.expectOne(`${environment.apiUrl}/admin/institutional/translations/PRIVACY`);
    expect(req.request.method).toBe('GET');
    req.flush({
      sectionKey: 'PRIVACY',
      title: 'Privacidade & LGPD',
      translations: {
        pt: { content: 'Texto LGPD em PT', body: 'Texto LGPD em PT' },
        en: { content: 'GDPR Text in EN', body: 'GDPR Text in EN' }
      }
    });

    expect(component.activeSection).toBe('PRIVACY');
    expect(component.currentSectionTitle).toBe('Privacidade & LGPD');
    expect(component.tempPt.content).toBe('Texto LGPD em PT');
    expect(component.tempEn.content).toBe('GDPR Text in EN');
  });

  it('deve alternar para a seção TERMS e buscar dados de TERMS', () => {
    fixture.detectChanges();
    httpMock.expectOne(`${environment.apiUrl}/admin/institutional/translations/ABOUT_MANIFESTO`).flush(mockResponse);

    component.loadSection('TERMS');

    const req = httpMock.expectOne(`${environment.apiUrl}/admin/institutional/translations/TERMS`);
    expect(req.request.method).toBe('GET');
    req.flush({
      sectionKey: 'TERMS',
      title: 'Termos de Uso',
      translations: {
        pt: { content: 'Texto Termos em PT', body: 'Texto Termos em PT' },
        en: { content: 'Terms Text in EN', body: 'Terms Text in EN' }
      }
    });

    expect(component.activeSection).toBe('TERMS');
    expect(component.currentSectionTitle).toBe('Termos de Uso');
    expect(component.tempPt.content).toBe('Texto Termos em PT');
    expect(component.tempEn.content).toBe('Terms Text in EN');
  });

  it('deve salvar seção simplificada via saveCurrentSimpleSection', fakeAsync(() => {
    fixture.detectChanges();
    httpMock.expectOne(`${environment.apiUrl}/admin/institutional/translations/ABOUT_MANIFESTO`).flush(mockResponse);

    component.activeSection = 'TERMS';
    component.tempPt.content = 'Novos Termos em Português';
    component.tempEn.content = 'New Terms in English';

    component.saveCurrentSimpleSection();

    const req = httpMock.expectOne(`${environment.apiUrl}/admin/institutional/translations/TERMS`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body.pt.content).toBe('Novos Termos em Português');
    expect(req.request.body.en.content).toBe('New Terms in English');
    req.flush({ success: true });

    expect(component.isSaving).toBeFalse();
    expect(component.successBlock).toBe('all');
    tick(3000);
    expect(component.successBlock).toBeNull();
  }));

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

    expect(component.currentSectionTitle).toBe('Sobre Nós');
    expect(component.currentContent.pt.headline).toBe('A Nova Estética do Scouting Global');
  });

  it('deve salvar todo o conteúdo via saveContent e enviar payload completo', fakeAsync(() => {
    fixture.detectChanges();
    httpMock.expectOne(`${environment.apiUrl}/admin/institutional/translations/ABOUT_MANIFESTO`).flush(mockResponse);

    component.currentContent.pt.headline = 'Título Global Atualizado';
    component.currentContent.en.headline = 'Updated Global Title';

    component.saveContent();

    const req = httpMock.expectOne(`${environment.apiUrl}/admin/institutional/translations/ABOUT_MANIFESTO`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body.pt.headline).toBe('Título Global Atualizado');
    expect(req.request.body.en.headline).toBe('Updated Global Title');
    req.flush({ success: true });

    expect(component.isSaving).toBeFalse();
    expect(component.successBlock).toBe('all');
    tick(3000);
    expect(component.successBlock).toBeNull();
  }));
});

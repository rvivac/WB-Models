import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { ApplyFaqManagerComponent } from './apply-faq-manager.component';
import { ApplyFaqService } from '../../../core/services/apply-faq.service';
import { ApplyFaq, ApplyHeader } from '../../../shared/models/apply-faq.interface';

describe('ApplyFaqManagerComponent', () => {
  let component: ApplyFaqManagerComponent;
  let fixture: ComponentFixture<ApplyFaqManagerComponent>;
  let faqServiceSpy: jasmine.SpyObj<ApplyFaqService>;

  const mockHeader: ApplyHeader = {
    title: 'QUERO SER MODELO',
    subtitle: 'WB SCOUTING DESK',
    description: 'Preencha o formulário e envie suas fotos.'
  };

  const mockFaqs: ApplyFaq[] = [
    {
      id: '00000000-0000-0000-0000-000000000001',
      question: 'Existe algum custo?',
      answer: 'Não, 100% gratuito.',
      displayOrder: 0,
      isActive: true
    },
    {
      id: '00000000-0000-0000-0000-000000000002',
      question: 'Quais fotos enviar?',
      answer: 'Polaroids naturais.',
      displayOrder: 1,
      isActive: true
    }
  ];

  beforeEach(async () => {
    faqServiceSpy = jasmine.createSpyObj('ApplyFaqService', [
      'getAdminApplyHeader',
      'updateApplyHeader',
      'getAdminFaqs',
      'createFaq',
      'updateFaq',
      'toggleStatus',
      'reorderFaqs',
      'deleteFaq'
    ], {
      defaultHeader: mockHeader
    });

    faqServiceSpy.getAdminApplyHeader.and.returnValue(of(mockHeader));
    faqServiceSpy.getAdminFaqs.and.returnValue(of(mockFaqs));

    await TestBed.configureTestingModule({
      imports: [ApplyFaqManagerComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: ApplyFaqService, useValue: faqServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ApplyFaqManagerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('deve criar o componente e carregar cabeçalho e FAQs', () => {
    expect(component).toBeTruthy();
    expect(component.headerForm.get('title')?.value).toBe('QUERO SER MODELO');
    expect(component.faqs.length).toBe(2);
  });

  it('deve salvar o cabeçalho institucional ao submeter o formulário', () => {
    faqServiceSpy.updateApplyHeader.and.returnValue(of(mockHeader));

    component.headerForm.patchValue({
      title: 'NOVO TITULO',
      subtitle: 'NOVO SUBTITULO',
      description: 'Nova descricao'
    });

    component.saveHeader();

    expect(faqServiceSpy.updateApplyHeader).toHaveBeenCalled();
    expect(component.isSavingHeader).toBeFalse();
  });

  it('deve abrir modal para criação e resetar formulário', () => {
    component.openCreateModal();
    expect(component.isModalOpen).toBeTrue();
    expect(component.editingFaqId).toBeNull();
    expect(component.faqModalForm.get('question')?.value).toBe('');
  });

  it('deve abrir modal para edição com dados preenchidos', () => {
    component.openEditModal(mockFaqs[0]);
    expect(component.isModalOpen).toBeTrue();
    expect(component.editingFaqId).toBe(mockFaqs[0].id);
    expect(component.faqModalForm.get('question')?.value).toBe(mockFaqs[0].question);
  });

  it('deve alternar status da pergunta', () => {
    const toggledFaq = { ...mockFaqs[0], isActive: false };
    faqServiceSpy.toggleStatus.and.returnValue(of(toggledFaq));

    component.toggleStatus(component.faqs[0]);
    expect(faqServiceSpy.toggleStatus).toHaveBeenCalledWith(mockFaqs[0].id);
    expect(component.faqs[0].isActive).toBeFalse();
  });

  it('deve excluir pergunta ao confirmar', () => {
    faqServiceSpy.deleteFaq.and.returnValue(of(undefined));

    component.openDeleteModal(mockFaqs[0]);
    expect(component.deleteModalOpen).toBeTrue();

    component.executeDelete();
    expect(faqServiceSpy.deleteFaq).toHaveBeenCalledWith(mockFaqs[0].id);
    expect(component.deleteModalOpen).toBeFalse();
  });

  it('deve reordenar perguntas ao mover para baixo', () => {
    faqServiceSpy.reorderFaqs.and.returnValue(of(undefined));

    component.moveDown(0);
    expect(faqServiceSpy.reorderFaqs).toHaveBeenCalled();
    expect(component.faqs[0].id).toBe(mockFaqs[1].id);
  });
});

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { BecomeModelComponent } from './become-model.component';
import { environment } from '../../../../environments/environment';

import { of } from 'rxjs';
import { ApplyFaqService } from '../../../core/services/apply-faq.service';

import { provideRouter } from '@angular/router';
import { TranslationService } from '../../../core/services/translation.service';

describe('BecomeModelComponent', () => {
  let component: BecomeModelComponent;
  let fixture: ComponentFixture<BecomeModelComponent>;
  let httpTesting: HttpTestingController;

  const mockFaqService = {
    getPublicApplyHeader: () => of({
      title: 'QUERO SER MODELO',
      subtitle: 'WB SCOUTING DESK',
      description: 'Preencha o formulário e envie suas fotos.'
    }),
    getPublicFaqs: () => of([
      {
        id: '00000000-0000-0000-0000-000000000001',
        question: 'Existe algum custo para inscrição?',
        answer: 'Não, 100% gratuito.',
        displayOrder: 0,
        isActive: true
      }
    ])
  };

  let currentLangSignal = signal<'pt' | 'en'>('pt');

  beforeEach(async () => {
    currentLangSignal = signal<'pt' | 'en'>('pt');

    await TestBed.configureTestingModule({
      imports: [BecomeModelComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ApplyFaqService, useValue: mockFaqService },
        {
          provide: TranslationService,
          useValue: {
            currentLang: currentLangSignal,
            translate: (key: string) => {
              const dict: Record<string, string> = {
                'nav.home': 'Home',
                'nav.apply': 'Quero ser modelo',
                'apply_page.header_title': 'Be a Model',
                'apply_page.header_description': 'If you wish to join the WB Agency casting...',
                'apply_page.faq_1_q': 'Is there any cost for application or evaluation?',
                'apply_page.faq_1_a': 'No. WB Agency never charges any fees...'
              };
              return dict[key] || key;
            }
          }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(BecomeModelComponent);
    component = fixture.componentInstance;
    httpTesting = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('deve instanciar o componente com 8 slots de fotos e 3 primeiras obrigatórias', () => {
    expect(component).toBeTruthy();
    expect(component.photoSlots.length).toBe(8);
    expect(component.photoSlots[0].required).toBeTrue();
    expect(component.photoSlots[1].required).toBeTrue();
    expect(component.photoSlots[2].required).toBeTrue();
    expect(component.photoSlots[3].required).toBeFalse();
    expect(component.hasRequiredPhotos).toBeFalse();
    expect(component.totalUploadedPhotos).toBe(0);
  });

  it('deve identificar candidato menor de idade e exigir CPF e dados do responsável', () => {
    // Definir data de nascimento de alguém com 16 anos
    const minorYear = new Date().getFullYear() - 16;
    const minorDate = `${minorYear}-05-15`;

    component.form.get('birthDate')?.setValue(minorDate);
    fixture.detectChanges();

    expect(component.isMinor).toBeTrue();
    expect(component.calculatedAge).toBe(16);

    const guardianCpf = component.form.get('guardianCpf');
    const guardianName = component.form.get('guardianName');

    expect(guardianCpf?.validator).not.toBeNull();
    expect(guardianName?.validator).not.toBeNull();

    // Inválido se vazio
    guardianCpf?.setValue('');
    expect(guardianCpf?.invalid).toBeTrue();

    // Inválido com CPF com dígitos iguais
    guardianCpf?.setValue('111.111.111-11');
    expect(guardianCpf?.errors?.['invalidCpf']).toBeTrue();

    // Inválido com CPF com dígito verificador incorreto
    guardianCpf?.setValue('123.456.789-00');
    expect(guardianCpf?.errors?.['invalidCpf']).toBeTrue();

    // Válido com CPF autêntico
    guardianCpf?.setValue('529.982.247-25');
    expect(guardianCpf?.valid).toBeTrue();
  });

  it('deve liberar campos de responsável para candidato maior de 18 anos', () => {
    // Definir data de nascimento de alguém com 22 anos
    const adultYear = new Date().getFullYear() - 22;
    const adultDate = `${adultYear}-01-10`;

    component.form.get('birthDate')?.setValue(adultDate);
    fixture.detectChanges();

    expect(component.isMinor).toBeFalse();
    expect(component.calculatedAge).toBe(22);

    const guardianCpf = component.form.get('guardianCpf');
    expect(guardianCpf?.validator).toBeNull();
    expect(guardianCpf?.value).toBe('');
  });

  it('deve gerenciar inclusão e remoção de fotos na grade', () => {
    const dummyFile = new File(['dummy-content'], 'rosto.jpg', { type: 'image/jpeg' });

    component.photoSlots[0].file = dummyFile;
    component.photoSlots[0].previewUrl = 'data:image/jpeg;base64,dummy';

    component.photoSlots[1].file = dummyFile;
    component.photoSlots[1].previewUrl = 'data:image/jpeg;base64,dummy';

    component.photoSlots[2].file = dummyFile;
    component.photoSlots[2].previewUrl = 'data:image/jpeg;base64,dummy';

    expect(component.hasRequiredPhotos).toBeTrue();
    expect(component.totalUploadedPhotos).toBe(3);

    // Remove foto do slot 1
    component.removePhoto(1);
    expect(component.photoSlots[1].file).toBeNull();
    expect(component.hasRequiredPhotos).toBeFalse();
    expect(component.totalUploadedPhotos).toBe(2);
  });

  it('deve bloquear submissão se o formulário for inválido ou faltar fotos obrigatórias', () => {
    component.onSubmit();
    expect(component.isSubmitting).toBeFalse();
    expect(component.form.touched).toBeTrue();
  });

  it('deve submeter FormData com sucesso para o endpoint /submissions', () => {
    // Preenche dados válidos de adulto
    component.form.patchValue({
      fullName: 'Larissa Manoela',
      email: 'larissa@wbmodels.com',
      phone: '+55 11 98888-7777',
      birthDate: '2000-01-01',
      city: 'São Paulo',
      state: 'SP',
      height: 178,
      bust: 85,
      waist: 60,
      hips: 90,
      shoes: '38',
      eyes: 'Verdes',
      hair: 'Castanho',
      lgpdConsent: true
    });

    const file = new File(['content'], 'foto.jpg', { type: 'image/jpeg' });
    component.photoSlots[0].file = file;
    component.photoSlots[1].file = file;
    component.photoSlots[2].file = file;

    component.onSubmit();
    expect(component.isSubmitting).toBeTrue();

    const req = httpTesting.expectOne(`${environment.apiUrl}/submissions`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body instanceof FormData).toBeTrue();

    req.flush({ success: true, message: 'Candidatura enviada' });

    expect(component.isSubmitting).toBeFalse();
    expect(component.submitSuccess).toBeTrue();
  });

  it('deve exibir títulos e FAQs em inglês quando o idioma ativo for en', () => {
    currentLangSignal.set('en');
    expect(component.displayHeaderTitle).toBe('Be a Model');
    expect(component.displayHeaderDescription).toBe('If you wish to join the WB Agency casting...');

    const faq = {
      id: '00000000-0000-0000-0000-000000000001',
      question: 'Existe algum custo para inscrição?',
      answer: 'Não, 100% gratuito.',
      displayOrder: 0,
      isActive: true
    };
    expect(component.getFaqQuestion(faq, 0)).toBe('Is there any cost for application or evaluation?');
    expect(component.getFaqAnswer(faq, 0)).toBe('No. WB Agency never charges any fees...');
  });

  it('deve permitir selecionar a opção NOT IN BRAZIL (EX) no campo de estado', () => {
    component.form.get('state')?.setValue('EX');
    expect(component.form.get('state')?.valid).toBeTrue();
    expect(component.form.get('state')?.value).toBe('EX');

    const selectEl: HTMLSelectElement = fixture.nativeElement.querySelector('#state');
    expect(selectEl).toBeTruthy();
    const options = Array.from(selectEl.options);
    const notInBrazilOption = options.find(opt => opt.value === 'EX');
    expect(notInBrazilOption).toBeTruthy();
  });
});

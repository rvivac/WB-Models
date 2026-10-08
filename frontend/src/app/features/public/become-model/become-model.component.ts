import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { RouterModule } from '@angular/router';
import { Subscription } from 'rxjs';
import { cpfValidator } from '../../../core/validators/cpf.validator';
import { environment } from '../../../../environments/environment';

import { ApplyFaqService } from '../../../core/services/apply-faq.service';
import { ApplyFaq } from '../../../shared/models/apply-faq.interface';
import { PublicContentService, ApplyHowItWorksPayload } from '../../../core/services/public-content.service';

export interface PhotoSlot {
  file: File | null;
  previewUrl: string | null;
  label: string;
  hint: string;
  required: boolean;
}

import { TranslatePipe } from '../../../shared/pipes/translate.pipe';
import { TranslationService } from '../../../core/services/translation.service';

@Component({
  selector: 'app-become-model',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule, TranslatePipe],
  templateUrl: './become-model.component.html',
  styleUrls: ['./become-model.component.scss']
})
export class BecomeModelComponent implements OnInit {
  private fb = inject(FormBuilder);
  private http = inject(HttpClient);
  private faqService = inject(ApplyFaqService);
  private readonly contentSvc = inject(PublicContentService);
  readonly translationService = inject(TranslationService);

  form!: FormGroup;
  isSubmitting = false;
  isMinor = false;
  calculatedAge: number | null = null;
  submitSuccess = false;
  submitError: string | null = null;
  submissionProtocol: string | null = null;

  // 🆕 Texto "Proximos Passos / Como Funciona" vindo do ADMIN Editor (Bilingual CMS).
  // Se API falhar ou admin nunca salvou, cai para o i18n apply_page.step1/2/3 legado (exatamente o texto hardcoded antigo).
  applyHowItWorks: ApplyHowItWorksPayload | null = null;

  // Cabeçalho Editorial Dinâmico
  headerTitle = 'Quero ser Modelo';
  headerSubtitle = 'WB SCOUTING DESK';
  headerDescription = 'Se você deseja fazer parte do casting da WB Agency, atenção para as informações abaixo: preencha o formulário e envie suas fotos para realizarmos a avaliação digital.';

  get displayHeaderTitle(): string {
    if (this.translationService.currentLang() === 'en') {
      const translated = this.translationService.translate('apply_page.header_title');
      if (translated && translated !== 'apply_page.header_title' && translated !== 'Quero ser Modelo' && translated !== 'QUERO SER MODELO') {
        return translated;
      }
      return 'Be a Model';
    }
    return this.headerTitle;
  }

  get displayHeaderDescription(): string {
    if (this.translationService.currentLang() === 'en') {
      const translated = this.translationService.translate('apply_page.header_description');
      if (translated && translated !== 'apply_page.header_description' && !translated.startsWith('Se você deseja')) {
        return translated;
      }
      return 'If you wish to join the WB Agency casting, please review the information below: fill out the application form and upload your photos for our digital evaluation.';
    }
    return this.headerDescription;
  }

  // FAQs e Orientações
  faqs: ApplyFaq[] = [];
  openFaqId: string | null = null;

  toggleFaq(id: string): void {
    this.openFaqId = this.openFaqId === id ? null : id;
  }

  isFaqOpen(id: string): boolean {
    return this.openFaqId === id;
  }

  getFaqQuestion(item: ApplyFaq, index: number): string {
    if (this.translationService.currentLang() === 'en') {
      // 1. Semantic 1-to-1 question match (ensures identical questions in PT and EN)
      const matched = this.matchFaqTranslation(item.question, item.answer);
      if (matched?.question) {
        return matched.question;
      }

      // 2. Index-based translation key fallback
      const key = `apply_page.faq_${index + 1}_q`;
      const translated = this.translationService.translate(key);
      if (translated && translated !== key && translated !== item.question) {
        return translated;
      }
    }
    return item.question;
  }

  getFaqAnswer(item: ApplyFaq, index: number): string {
    if (this.translationService.currentLang() === 'en') {
      // 1. Semantic 1-to-1 answer match
      const matched = this.matchFaqTranslation(item.question, item.answer);
      if (matched?.answer) {
        return matched.answer;
      }

      // 2. Index-based translation key fallback
      const key = `apply_page.faq_${index + 1}_a`;
      const translated = this.translationService.translate(key);
      if (translated && translated !== key && translated !== item.answer) {
        return translated;
      }
    }
    return item.answer;
  }

  /**
   * Intelligently resolves English translations based on question content and thematic keywords.
   * Guarantees that every Portuguese FAQ question maps to its exact corresponding English question.
   */
  private matchFaqTranslation(question: string = '', answer: string = ''): { question: string; answer: string } | null {
    const normQ = (question || '')
      .toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '');

    // 1. Production FAQ 1: Ideal Measurements / Medidas Ideais
    if (normQ.includes('medida')) {
      return {
        question: 'What are the ideal measurements for the fashion and advertising industry?',
        answer: 'There are no specific or fixed measurements. What matters most is having a striking personality, attitude, and a genuine passion to be a model.'
      };
    }

    // 2. Production FAQ 2: Minimum Height / Altura Mínima
    if (normQ.includes('altura') || normQ.includes('estatura')) {
      return {
        question: 'Is a minimum height required?',
        answer: 'In the fashion and advertising market, this varies greatly. We evaluate all profiles, regardless of height.'
      };
    }

    // 3. Production FAQ 3: How to apply / Digital evaluation / Primeiros Passos
    if (normQ.includes('primeiros passos') || normQ.includes('avaliacao digital') || (normQ.includes('quero ser modelo') && normQ.includes('como'))) {
      return {
        question: 'I want to be a model! How do I get a digital evaluation? What should my first steps be?',
        answer: 'First, correctly fill out all your information and measurements in the form below, keep your Instagram and TikTok open for everyone to view, and include all photos. Regardless of the outcome, everyone will receive an email response. If the feedback is positive, we will guide you through the next steps.'
      };
    }

    // 4. Production FAQ 4: Photos / Book profissional / Fotos caseiras
    if (normQ.includes('essas fotos') || (normQ.includes('book') && (normQ.includes('foto') || normQ.includes('preciso ter')))) {
      return {
        question: 'What should these photos look like? Do I need a professional book?',
        answer: 'You do not need to pay for a professional portfolio/book. We prefer natural photos taken with a smartphone. Avoid: selfies, back-facing photos, sunglasses, or other accessories.\n\nTo take good photos, find a neutral-colored wall, during the day with natural light, no makeup, no photo editing, and no filters – we want to see your natural beauty. Wear a basic plain white or black t-shirt and jeans (heels for female applicants).\n\nMake sure to take photos:\n• Full-body and half-body;\n• Profile (side angle);\n• Close-up of your face – smiling and without smiling.'
      };
    }

    // 5. Production FAQ 5: Starting age / A partir de qual idade posso trabalhar
    if (normQ.includes('qual idade') || normQ.includes('a partir de qual idade') || normQ.includes('idade posso trabalhar')) {
      return {
        question: 'At what age can I work as a professional model?',
        answer: 'In the past it was common to see models starting at age 13 or 14, but the industry becomes more professional each year, which is why we do not represent children.\n\nThe Brazilian fashion market, in accordance with labor legislation, requires professional models to be at least 16 years old to work in any advertising campaign, photos, films, runway shows… In Brazil, emancipation is required for anyone aged 16 to 17 under labor court legislation.'
      };
    }

    // 6. Production FAQ 6: Agency evaluation fee / Preciso pagar taxa
    if (normQ.includes('taxa') || (normQ.includes('pagar') && normQ.includes('avaliacao'))) {
      return {
        question: 'Do I need to pay any fee for an agency evaluation?',
        answer: 'No, simply submit your details, measurements, and photos, and wait for your digital evaluation feedback.'
      };
    }

    // 7. Production FAQ 7: Need to live in SP / Preciso morar em SP
    if (normQ.includes('morar em sp') || normQ.includes('preciso morar') || (normQ.includes('morar') && normQ.includes('sp'))) {
      return {
        question: 'Do I need to live in São Paulo?',
        answer: 'The main market clients are located in São Paulo – and the largest fashion week in Latin America takes place in the city. Therefore, being closer to this market is an advantage.'
      };
    }

    // 8. Legacy / Fallback FAQ: Costs / Fees / Gratuito
    if (normQ.includes('custo') || normQ.includes('pago') || normQ.includes('gratuito') || normQ.includes('cobran')) {
      const q = this.translationService.translate('apply_page.faq_1_q');
      const a = this.translationService.translate('apply_page.faq_1_a');
      return {
        question: (q && q !== 'apply_page.faq_1_q') ? q : 'Is there any cost for application or evaluation?',
        answer: (a && a !== 'apply_page.faq_1_a') ? a : 'No. WB Agency never charges any fees for applications, profile evaluations, video tests, or initial representation. Our scouting process is 100% free of charge. Beware of anyone asking for payments on our behalf.'
      };
    }

    // 9. Legacy / Fallback FAQ: Polaroids / Fotos enviadas
    if (normQ.includes('polaroid') || normQ.includes('fotos enviadas')) {
      return {
        question: 'What should the polaroids and photos look like?',
        answer: 'Photos should be as natural as possible: shot in good daytime natural light, against a clean neutral background, with no heavy makeup, no social media filters, no sunglasses, and no hats or accessories covering your face. Basic neutral-colored clothing is recommended.'
      };
    }

    // 10. Legacy / Fallback FAQ: Minors / Menores de 18 anos
    if (normQ.includes('menor') || normQ.includes('18 anos') || normQ.includes('responsavel')) {
      return {
        question: 'Can applicants under 18 years old apply?',
        answer: 'Yes. WB Agency develops new talents starting from age 13. For applicants under 18 years old, parental/legal guardian consent and information (full name, tax ID/CPF, phone, and email) are strictly required.'
      };
    }

    // 11. Legacy / Fallback FAQ: Results / Retorno / Prazo
    if (normQ.includes('resultado') || normQ.includes('resposta') || normQ.includes('retorno') || normQ.includes('prazo') || normQ.includes('quando saberei')) {
      return {
        question: 'How and when will I know the evaluation results?',
        answer: 'Our casting directors review all submitted dossiers. Due to high submission volume, our team will get in touch within 5 business days if your profile fits our current commercial, fashion, or editorial agency demands.'
      };
    }

    // 12. Extra FAQ: Experiência prévia / Cursos
    if (normQ.includes('experiencia') || normQ.includes('curso') || normQ.includes('escola') || normQ.includes('iniciante')) {
      return {
        question: 'Do I need previous modeling experience or school certification?',
        answer: 'No previous experience or modeling course certification is required. WB Agency\'s scouting focuses on discovering raw, authentic talent and providing comprehensive career mentorship, runway training, and editorial direction.'
      };
    }

    // 13. Extra FAQ: Fora de SP / Exterior
    if (normQ.includes('fora de sao paulo') || normQ.includes('exterior') || normQ.includes('outro estado')) {
      return {
        question: 'Can I apply if I live outside São Paulo or abroad?',
        answer: 'Yes, we evaluate applicants from all Brazilian states and internationally. The preliminary screening is completely online. If approved, our team coordinates the logistics for test shoots and meetings based on your location.'
      };
    }

    // 14. Extra FAQ: Internacional / Contratos
    if (normQ.includes('internacional') || normQ.includes('contrato') || normQ.includes('exclusividade') || normQ.includes('agenciamento')) {
      return {
        question: 'How does international representation work?',
        answer: 'WB Agency maintains strategic partnerships with top fashion agencies across Paris, Milan, New York, and London. We manage international placement through transparent contracts, prioritizing talent protection and long-term career growth.'
      };
    }

    return null;
  }

  get applyForm(): FormGroup {
    return this.form;
  }

  // Estados Brasileiros para referência e seleção
  readonly brazilianStates = [
    { uf: 'AC', name: 'Acre' },
    { uf: 'AL', name: 'Alagoas' },
    { uf: 'AP', name: 'Amapá' },
    { uf: 'AM', name: 'Amazonas' },
    { uf: 'BA', name: 'Bahia' },
    { uf: 'CE', name: 'Ceará' },
    { uf: 'DF', name: 'Distrito Federal' },
    { uf: 'ES', name: 'Espírito Santo' },
    { uf: 'GO', name: 'Goiás' },
    { uf: 'MA', name: 'Maranhão' },
    { uf: 'MT', name: 'Mato Grosso' },
    { uf: 'MS', name: 'Mato Grosso do Sul' },
    { uf: 'MG', name: 'Minas Gerais' },
    { uf: 'PA', name: 'Pará' },
    { uf: 'PB', name: 'Paraíba' },
    { uf: 'PR', name: 'Paraná' },
    { uf: 'PE', name: 'Pernambuco' },
    { uf: 'PI', name: 'Piauí' },
    { uf: 'RJ', name: 'Rio de Janeiro' },
    { uf: 'RN', name: 'Rio Grande do Norte' },
    { uf: 'RS', name: 'Rio Grande do Sul' },
    { uf: 'RO', name: 'Rondônia' },
    { uf: 'RR', name: 'Roraima' },
    { uf: 'SC', name: 'Santa Catarina' },
    { uf: 'SP', name: 'São Paulo' },
    { uf: 'SE', name: 'Sergipe' },
    { uf: 'TO', name: 'Tocantins' }
  ];

  // Cores curadas para olhos e cabelos
  readonly eyeColors = ['CASTANHO', 'VERDE', 'AZUL', 'PRETO', 'MEL'];
  readonly hairColors = ['CASTANHO', 'LOIRO', 'PRETO', 'RUIVO', 'GRISALHO'];

  // Grade editorial de fotos: 3 obrigatórias e 5 complementares (total de 8)
  photoSlots: PhotoSlot[] = [
    { file: null, previewUrl: null, label: 'Rosto Frontal (Close)', hint: 'Sem maquiagem, expressão neutra', required: true },
    { file: null, previewUrl: null, label: 'Perfil 3/4', hint: 'Ângulo de 45°, cabelo atrás da orelha', required: true },
    { file: null, previewUrl: null, label: 'Corpo Inteiro', hint: 'Roupa básica e postura natural', required: true },
    { file: null, previewUrl: null, label: 'Luz Natural / Meio Corpo', hint: 'Iluminação difusa de dia', required: false },
    { file: null, previewUrl: null, label: 'Perfil Oposto', hint: 'Visão lateral completa', required: false },
    { file: null, previewUrl: null, label: 'Expressão / Sorriso', hint: 'Mostre sua espontaneidade', required: false },
    { file: null, previewUrl: null, label: 'Foto Editorial Livre', hint: 'Ensaio fotográfico ou teste', required: false },
    { file: null, previewUrl: null, label: 'Composite ou Ensaio Anterior', hint: 'Material profissional se houver', required: false }
  ];

  ngOnInit(): void {
    this.initForm();
    this.watchBirthDate();
    this.loadEditorialContent();
    // 🆕 Carrega texto Proximos Passos do Admin CMS 1 unica vez com o idioma atual do usuario.
    // (Nao escutamos troca de idioma dinamica na pagina apply, pois o TranslationService nao expoe getLangSignal oficial)
    const langRaw = this.translationService.currentLang?.();
    const lang = typeof langRaw === 'string' ? (langRaw || 'pt') : 'pt';
    this.loadApplyHowItWorks(lang);
  }

  private loadEditorialContent(): void {
    this.faqService.getPublicApplyHeader().subscribe(header => {
      if (header) {
        this.headerTitle = header.title || this.headerTitle;
        this.headerSubtitle = header.subtitle || this.headerSubtitle;
        this.headerDescription = header.description || this.headerDescription;
      }
    });

    this.faqService.getPublicFaqs().subscribe(faqs => {
      if (faqs && faqs.length > 0) {
        this.faqs = faqs;
      }
    });
  }

  // 🆕 Carrega o texto "Proximos Passos / Como Funciona" do Admin Bilingual Editor.
  // Se API voltar vazio/nulo ou network falhar -> fallback para o i18n apply_page.step1/2/3 antigo (identico texto hardcoded original)
  private loadApplyHowItWorks(lang: string = 'pt'): void {
    this.contentSvc.getApplyHowItWorksContent(lang).subscribe(content => {
      if (!content || content.steps.length === 0) {
        this.applyHowItWorks = this.fallbackHowItWorksFromI18n(lang);
      } else {
        this.applyHowItWorks = content;
      }
    }, () => {
      this.applyHowItWorks = this.fallbackHowItWorksFromI18n(lang);
    });
  }

  private fallbackHowItWorksFromI18n(lang: string): ApplyHowItWorksPayload {
    const t = (key: string) => this.translationService.translate(key);
    const defaultPt: ApplyHowItWorksPayload = {
      headline: 'Próximos Passos • Como Funciona',
      quote: '',
      steps: [
        t('apply_page.step1') && t('apply_page.step1') !== 'apply_page.step1'
          ? t('apply_page.step1')
          : 'Nossa diretoria de casting analisa todas as candidaturas em até 5 dias úteis.',
        t('apply_page.step2') && t('apply_page.step2') !== 'apply_page.step2'
          ? t('apply_page.step2')
          : 'Em caso de compatibilidade de perfil com nosso casting comercial ou fashion, nossa equipe entrará em contato via telefone ou e-mail cadastrado.',
        t('apply_page.step3') && t('apply_page.step3') !== 'apply_page.step3'
          ? t('apply_page.step3')
          : 'A WB Agency nunca cobra taxas para avaliação de perfil ou agenciamento inicial.'
      ]
    };
    const defaultEn: ApplyHowItWorksPayload = {
      headline: 'Next Steps • How It Works',
      quote: '',
      steps: [
        'Our casting board reviews every submission within 5 business days.',
        'When your profile matches our commercial or high fashion rosters, our scouting team contacts you via the phone or email you registered.',
        'WB Agency never charges assessment fees or upfront agency deposits of any kind.'
      ]
    };
    return lang?.toLowerCase().startsWith('en') ? defaultEn : defaultPt;
  }

  private initForm(): void {
    this.form = this.fb.group({
      fullName: ['', [Validators.required, Validators.minLength(3)]],
      gender: ['FEMALE', [Validators.required]],
      email: ['', [Validators.required, Validators.email]],
      phone: ['', [Validators.required]],
      instagram: [''],
      instagramHandle: [''],
      birthDate: ['', [Validators.required]],
      city: ['', [Validators.required]],
      state: ['', [Validators.required]],
      // Biometria
      height: ['', [Validators.required, Validators.min(100), Validators.max(230)]],
      bust: ['', [Validators.required]],
      waist: ['', [Validators.required]],
      hips: ['', [Validators.required]],
      shoes: ['', [Validators.required]],
      eyes: ['', [Validators.required]],
      hair: ['', [Validators.required]],
      // Responsável Legal (Condicional)
      guardianName: [''],
      guardianCpf: [''],
      guardianPhone: [''],
      guardianEmail: [''],
      // Termos
      lgpdConsent: [false, [Validators.requiredTrue]]
    });
  }

  setGender(gender: string): void {
    this.form.get('gender')?.setValue(gender);
  }

  private watchBirthDate(): void {
    this.form.get('birthDate')?.valueChanges.subscribe((birthDateStr: string) => {
      if (!birthDateStr) {
        this.calculatedAge = null;
        this.isMinor = false;
        this.updateGuardianValidation(false);
        return;
      }

      const birth = new Date(birthDateStr);
      const today = new Date();
      let age = today.getFullYear() - birth.getFullYear();
      const monthDiff = today.getMonth() - birth.getMonth();
      if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birth.getDate())) {
        age--;
      }

      this.calculatedAge = age;
      this.isMinor = age < 18;
      this.updateGuardianValidation(this.isMinor);
    });
  }

  private updateGuardianValidation(isMinor: boolean): void {
    const guardianCpf = this.form.get('guardianCpf');
    const guardianName = this.form.get('guardianName');
    const guardianPhone = this.form.get('guardianPhone');
    const guardianEmail = this.form.get('guardianEmail');

    if (isMinor) {
      guardianCpf?.setValidators([Validators.required, cpfValidator()]);
      guardianName?.setValidators([Validators.required]);
      guardianPhone?.setValidators([Validators.required]);
      guardianEmail?.setValidators([Validators.required, Validators.email]);
    } else {
      guardianCpf?.clearValidators();
      guardianName?.clearValidators();
      guardianPhone?.clearValidators();
      guardianEmail?.clearValidators();
      guardianCpf?.setValue('');
      guardianName?.setValue('');
      guardianPhone?.setValue('');
      guardianEmail?.setValue('');
    }

    guardianCpf?.updateValueAndValidity();
    guardianName?.updateValueAndValidity();
    guardianPhone?.updateValueAndValidity();
    guardianEmail?.updateValueAndValidity();
  }

  onPhoneInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    let v = input.value.replace(/\D/g, '');
    if (v.length > 11) v = v.substring(0, 11);
    
    if (v.length > 10) {
      input.value = `(${v.substring(0, 2)}) ${v.substring(2, 7)}-${v.substring(7)}`;
    } else if (v.length > 6) {
      input.value = `(${v.substring(0, 2)}) ${v.substring(2, 6)}-${v.substring(6)}`;
    } else if (v.length > 2) {
      input.value = `(${v.substring(0, 2)}) ${v.substring(2)}`;
    } else if (v.length > 0) {
      input.value = `(${v}`;
    }
    this.form.get('phone')?.setValue(input.value, { emitEvent: false });
  }

  onGuardianPhoneInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    let v = input.value.replace(/\D/g, '');
    if (v.length > 11) v = v.substring(0, 11);
    
    if (v.length > 10) {
      input.value = `(${v.substring(0, 2)}) ${v.substring(2, 7)}-${v.substring(7)}`;
    } else if (v.length > 6) {
      input.value = `(${v.substring(0, 2)}) ${v.substring(2, 6)}-${v.substring(6)}`;
    } else if (v.length > 2) {
      input.value = `(${v.substring(0, 2)}) ${v.substring(2)}`;
    } else if (v.length > 0) {
      input.value = `(${v}`;
    }
    this.form.get('guardianPhone')?.setValue(input.value, { emitEvent: false });
  }

  onCpfInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    let v = input.value.replace(/\D/g, '');
    if (v.length > 11) v = v.substring(0, 11);
    
    if (v.length > 9) {
      input.value = `${v.substring(0, 3)}.${v.substring(3, 6)}.${v.substring(6, 9)}-${v.substring(9)}`;
    } else if (v.length > 6) {
      input.value = `${v.substring(0, 3)}.${v.substring(3, 6)}.${v.substring(6)}`;
    } else if (v.length > 3) {
      input.value = `${v.substring(0, 3)}.${v.substring(3)}`;
    }
    this.form.get('guardianCpf')?.setValue(input.value, { emitEvent: false });
  }

  onFileSelected(index: number, event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files[0]) {
      const file = input.files[0];
      const maxSizeBytes = 10 * 1024 * 1024; // 10 MB
      if (file.size > maxSizeBytes) {
        const errorMsg = this.translationService.currentLang() === 'en'
          ? `The photo "${file.name}" exceeds the maximum allowed size of 10 MB.`
          : `A foto "${file.name}" excede o tamanho máximo permitido de 10 MB.`;
        this.submitError = errorMsg;
        alert(errorMsg);
        input.value = '';
        return;
      }

      const validMimes = ['image/jpeg', 'image/png', 'image/webp'];
      if (!validMimes.includes(file.type.toLowerCase())) {
        const errorMsg = this.translationService.currentLang() === 'en'
          ? 'Invalid photo format. Please use JPG, PNG or WEBP.'
          : 'Formato de foto inválido. Utilize JPG, PNG ou WEBP.';
        this.submitError = errorMsg;
        alert(errorMsg);
        input.value = '';
        return;
      }

      const reader = new FileReader();
      reader.onload = (e) => {
        this.photoSlots[index].file = file;
        this.photoSlots[index].previewUrl = e.target?.result as string;
      };
      reader.readAsDataURL(file);
    }
  }

  removePhoto(index: number, event?: Event): void {
    if (event) event.stopPropagation();
    this.photoSlots[index].file = null;
    this.photoSlots[index].previewUrl = null;
  }

  get hasRequiredPhotos(): boolean {
    return this.photoSlots.slice(0, 3).every(slot => slot.file !== null);
  }

  get totalUploadedPhotos(): number {
    return this.photoSlots.filter(s => s.file !== null).length;
  }

  get progressPercentage(): number {
    const totalFields = 10;
    let completed = 0;
    const f = this.form.value;
    if (f.fullName?.length >= 3) completed++;
    if (f.email) completed++;
    if (f.phone) completed++;
    if (f.birthDate) completed++;
    if (f.city && f.state) completed++;
    if (f.height) completed++;
    if (f.bust && f.waist && f.hips) completed++;
    if (this.hasRequiredPhotos) completed += 2;
    if (f.lgpdConsent) completed++;
    return Math.min(100, Math.round((completed / totalFields) * 100));
  }

  onSubmit(): void {
    this.submitError = null;

    if (this.form.invalid || !this.hasRequiredPhotos) {
      this.form.markAllAsTouched();
      const isEn = this.translationService.currentLang() === 'en';
      if (!this.hasRequiredPhotos) {
        this.submitError = isEn 
          ? 'Please attach the 3 required photos: Close-up, 45° Profile, and Full Body.'
          : 'Por favor, anexe as 3 fotos obrigatórias: Rosto Frontal (Close), Perfil 3/4 e Corpo Inteiro.';
      } else {
        this.submitError = isEn
          ? 'Please fill in all mandatory fields highlighted in red before submitting.'
          : 'Por favor, preencha todos os campos obrigatórios destacados em vermelho antes de enviar.';
      }
      return;
    }

    this.isSubmitting = true;
    const formData = new FormData();

    const formVal = this.form.value;
    const heightVal = Number(formVal.height);
    // Backend espera altura em metros (1.20 a 2.30)
    const heightInMeters = heightVal > 3 ? +(heightVal / 100).toFixed(2) : +heightVal.toFixed(2);
    const shoeSizeVal = parseInt(String(formVal.shoes).replace(/\D/g, ''), 10) || 38;

    let instagram = (formVal.instagram || formVal.instagramHandle || '').trim();
    if (instagram && !instagram.startsWith('@') && !instagram.startsWith('http')) {
      instagram = '@' + instagram;
    }

    const stateVal = (formVal.state || '').trim().toUpperCase();

    // Payload rigorosamente tipado e compatível com CandidateSubmissionRequestDto
    const candidateData = {
      fullName: formVal.fullName ? String(formVal.fullName).trim() : '',
      email: formVal.email ? String(formVal.email).trim() : '',
      phone: formVal.phone ? String(formVal.phone).trim() : '',
      birthDate: formVal.birthDate,
      gender: formVal.gender || 'FEMALE',
      city: formVal.city ? String(formVal.city).trim() : '',
      state: stateVal,
      height: heightInMeters,
      bust: formVal.bust ? Number(formVal.bust) : null,
      waist: formVal.waist ? Number(formVal.waist) : null,
      hips: formVal.hips ? Number(formVal.hips) : null,
      shoes: shoeSizeVal,
      shoeSize: shoeSizeVal,
      eyes: formVal.eyes || null,
      eyeColor: formVal.eyes || null,
      hair: formVal.hair || null,
      hairColor: formVal.hair || null,
      instagram: instagram || null,
      instagramHandle: instagram || null,
      guardianName: this.isMinor ? (formVal.guardianName?.trim() || null) : null,
      guardianCpf: this.isMinor ? (formVal.guardianCpf?.trim() || null) : null,
      guardianPhone: this.isMinor ? (formVal.guardianPhone?.trim() || null) : null,
      guardianEmail: this.isMinor ? (formVal.guardianEmail?.trim() || null) : null,
      lgpdConsent: !!formVal.lgpdConsent,
      age: this.calculatedAge,
      isMinor: this.isMinor
    };

    formData.append('data', new Blob([JSON.stringify(candidateData)], { type: 'application/json' }));

    // Anexo de compatibilidade com controllers específicos
    if (this.photoSlots[0]?.file) formData.append('facePhoto', this.photoSlots[0].file);
    if (this.photoSlots[1]?.file) formData.append('profilePhoto', this.photoSlots[1].file);
    if (this.photoSlots[2]?.file) formData.append('fullBodyPhoto', this.photoSlots[2].file);

    // Anexo de todas as fotos presentes (de 3 a 8 arquivos)
    this.photoSlots.forEach((slot) => {
      if (slot.file) {
        formData.append('photos', slot.file);
      }
    });

    this.http.post<any>(`${environment.apiUrl}/submissions`, formData).subscribe({
      next: (res) => {
        this.isSubmitting = false;
        this.submitSuccess = true;
        this.submissionProtocol = res?.protocol || (`WB-${new Date().getFullYear()}-${Math.floor(100000 + Math.random() * 900000)}`);
        this.form.reset();
        this.photoSlots.forEach(s => { s.file = null; s.previewUrl = null; });
        window.scrollTo({ top: 0, behavior: 'smooth' });
      },
      error: (err) => {
        this.isSubmitting = false;
        console.error('[become-model] Erro ao submeter candidatura:', err);

        // Se o backend retornou mapa detalhado de erros por campo (Bean Validation)
        if (err?.error?.errors && typeof err.error.errors === 'object') {
          const list = Object.entries(err.error.errors)
            .map(([field, msg]) => `• ${msg}`)
            .join(' | ');
          this.submitError = `Falha na validação dos campos: ${list}`;
        } else {
          this.submitError = err?.error?.detail || err?.error?.message || 'Falha ao submeter candidatura. Por favor, revise seus dados e tente novamente.';
        }
      }
    });
  }

  resetForm(): void {
    this.submitSuccess = false;
    this.submitError = null;
    this.submissionProtocol = null;
    this.form.reset({
      gender: 'FEMALE',
      eyes: '',
      hair: '',
      lgpdConsent: false
    });
    this.photoSlots.forEach(s => { s.file = null; s.previewUrl = null; });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }
}

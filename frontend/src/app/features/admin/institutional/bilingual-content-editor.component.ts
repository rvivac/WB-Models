import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';

export interface TranslationBlock {
  headline?: string;
  quote?: string;
  sectionTitle?: string;
  body?: string;
  content?: string;
}

export interface BilingualContentState {
  pt: TranslationBlock;
  en: TranslationBlock;
}

export interface SectionTranslationsResponse {
  sectionKey: string;
  title: string;
  translations: BilingualContentState;
}

@Component({
  selector: 'app-bilingual-content-editor',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './bilingual-content-editor.component.html',
  styleUrls: ['./bilingual-content-editor.component.scss']
})
export class BilingualContentEditorComponent implements OnInit {
  private http = inject(HttpClient);

  private readonly SECTION_MAP: Record<string, string> = {
    'MANIFESTO': 'ABOUT_MANIFESTO',
    'ABOUT_MANIFESTO': 'ABOUT_MANIFESTO',
    'SCOUTING': 'SCOUTING_GUIDELINES',
    'SCOUTING_GUIDELINES': 'SCOUTING_GUIDELINES',
    'APPLY_FORM': 'SCOUTING_GUIDELINES',
    'APPLY': 'APPLY_HOW_IT_WORKS',
    'APPLY_HOW_IT_WORKS': 'APPLY_HOW_IT_WORKS',
    'TERMS': 'TERMS',
    'TERMS_OF_USE': 'TERMS',
    'PRIVACY': 'PRIVACY',
    'PRIVACY_POLICY': 'PRIVACY'
  };

  activeSection = 'ABOUT_MANIFESTO';
  currentSectionTitle = 'Sobre Nós';

  get selectedSectionKey(): string {
    return this.activeSection;
  }
  set selectedSectionKey(val: string) {
    this.activeSection = val;
  }

  loadSectionData(key: string): void {
    this.loadSection(key);
  }

  onSectionChange(key: string): void {
    this.loadSection(key);
  }

  currentContent: BilingualContentState = {
    pt: {
      headline: 'A Nova Estética do Scouting Global',
      quote: 'Acreditamos na autenticidade, na força da personalidade e na beleza singular de cada indivíduo.',
      sectionTitle: 'Nossa Filosofia',
      body: 'A WB Agency consolidou-se como um núcleo editorial focado no desenvolvimento integral de modelos para os principais mercados da moda internacional. Nossa metodologia rejeita a padronização e prioriza a identidade visual autêntica, conectando talentos a marcas com relevância estética global.',
      content: ''
    },
    en: {
      headline: 'The New Aesthetic of Global Scouting',
      quote: 'We believe in authenticity, personal strength, and the unique beauty of every individual.',
      sectionTitle: 'Our Philosophy',
      body: 'WB Agency has established itself as an editorial powerhouse dedicated to the comprehensive development of models for premier global fashion markets. Our scouting methodology moves beyond mass standards to foster authentic personal identity, positioning talents at the intersection of high fashion and international relevance.',
      content: ''
    }
  };

  editingBlock: 'headline' | 'quote' | 'sectionTitle' | 'body' | null = null;
  tempPt: TranslationBlock = { headline: '', quote: '', sectionTitle: '', body: '', content: '' };
  tempEn: TranslationBlock = { headline: '', quote: '', sectionTitle: '', body: '', content: '' };
  isSaving = false;
  successBlock: string | null = null;
  errorMessage: string | null = null;

  faqList: Array<{ question: string; answer: string; questionEn: string; answerEn: string }> = [];

  ngOnInit(): void {
    this.loadSection(this.activeSection);
  }

  getCanonicalKey(key: string): string {
    return this.SECTION_MAP[key] || key;
  }

  loadSection(sectionKey: string): void {
    this.activeSection = sectionKey;
    this.cancelEditBlock();
    this.currentSectionTitle = this.getSectionTitleByKey(sectionKey);

    if (sectionKey === 'SCOUTING') {
      this.loadFaqList();
      return;
    }

    if (sectionKey === 'TERMS' || sectionKey === 'PRIVACY') {
      const canonicalKey = this.getCanonicalKey(sectionKey);
      this.http.get<any>(`${environment.apiUrl}/admin/institutional/translations/${canonicalKey}`).subscribe({
        next: (res) => {
          // Extrai o texto de PT
          const ptText = 
            res?.translations?.pt?.content || 
            res?.translations?.pt?.body || 
            res?.payloadPt?.content || 
            res?.payloadPt?.body || 
            res?.content || 
            res?.body || 
            '';

          // Extrai o texto de EN
          const enText = 
            res?.translations?.en?.content || 
            res?.translations?.en?.body || 
            res?.payloadEn?.content || 
            res?.payloadEn?.body || 
            res?.contentEn || 
            '';

          this.tempPt = { ...this.tempPt, content: ptText };
          this.tempEn = { ...this.tempEn, content: enText };
          this.currentContent.pt.content = ptText;
          this.currentContent.pt.body = ptText;
          this.currentContent.en.content = enText;
          this.currentContent.en.body = enText;

          // Se um dos campos vier vazio, busca fallback pelo endpoint público
          if (!ptText || !ptText.trim()) {
            this.http.get<any>(`${environment.apiUrl}/public/content/${sectionKey}?lang=pt`).subscribe({
              next: (r) => {
                const val = r?.content || r?.body || (typeof r === 'string' ? r : '');
                if (val) {
                  this.tempPt.content = val;
                  this.currentContent.pt.content = val;
                  this.currentContent.pt.body = val;
                }
              }
            });
          }
          if (!enText || !enText.trim()) {
            this.http.get<any>(`${environment.apiUrl}/public/content/${sectionKey}?lang=en`).subscribe({
              next: (r) => {
                const val = r?.content || r?.body || (typeof r === 'string' ? r : '');
                if (val) {
                  this.tempEn.content = val;
                  this.currentContent.en.content = val;
                  this.currentContent.en.body = val;
                }
              }
            });
          }
        },
        error: (err) => {
          console.warn(`[I18N] Erro ao carregar traduções admin para ${sectionKey}. Aplicando fallback público:`, err);
          this.http.get<any>(`${environment.apiUrl}/public/content/${sectionKey}?lang=pt`).subscribe({
            next: (r) => {
              const val = r?.content || r?.body || (typeof r === 'string' ? r : '');
              this.tempPt = { ...this.tempPt, content: val };
              this.currentContent.pt.content = val;
              this.currentContent.pt.body = val;
            }
          });
          this.http.get<any>(`${environment.apiUrl}/public/content/${sectionKey}?lang=en`).subscribe({
            next: (r) => {
              const val = r?.content || r?.body || (typeof r === 'string' ? r : '');
              this.tempEn = { ...this.tempEn, content: val };
              this.currentContent.en.content = val;
              this.currentContent.en.body = val;
            }
          });
        }
      });
      return;
    }

    const canonicalKey = this.getCanonicalKey(sectionKey);

    this.http.get<SectionTranslationsResponse>(`${environment.apiUrl}/admin/institutional/translations/${canonicalKey}`).subscribe({
      next: (res) => {
        if (res && res.translations) {
          const isAbout = sectionKey.includes('ABOUT') || sectionKey.includes('MANIFESTO');
          this.currentSectionTitle = this.getSectionTitleByKey(sectionKey) || res.title;
          const ptContent = res.translations.pt?.content || res.translations.pt?.body || '';
          const enContent = res.translations.en?.content || res.translations.en?.body || '';

          this.currentContent = {
            pt: {
              headline: res.translations.pt?.headline || '',
              quote: res.translations.pt?.quote || (isAbout ? 'Acreditamos na autenticidade, na força da personalidade e na beleza singular de cada indivíduo.' : ''),
              sectionTitle: res.translations.pt?.sectionTitle || (isAbout ? 'Nossa Filosofia' : ''),
              body: res.translations.pt?.body || '',
              content: ptContent
            },
            en: {
              headline: res.translations.en?.headline || '',
              quote: res.translations.en?.quote || (isAbout ? 'We believe in authenticity, personal strength, and the unique beauty of every individual.' : ''),
              sectionTitle: res.translations.en?.sectionTitle || (isAbout ? 'Our Philosophy' : ''),
              body: res.translations.en?.body || '',
              content: enContent
            }
          };

          if (canonicalKey === 'TERMS' || canonicalKey === 'PRIVACY') {
            this.tempPt.content = ptContent;
            this.tempEn.content = enContent;
          }
        } else {
          this.errorMessage = `Nenhuma tradução encontrada para a seção ${sectionKey} no banco de dados.`;
        }
      },
      error: (err) => {
        console.warn(`[I18N] Carregando estrutura padrão para ${sectionKey}:`, err);
        if (canonicalKey === 'TERMS' || canonicalKey === 'PRIVACY') {
          this.http.get<any>(`${environment.apiUrl}/public/content/${canonicalKey}?lang=pt`).subscribe({
            next: (r) => {
              const val = r?.content || r?.payload?.content || '';
              this.tempPt.content = val;
              this.currentContent.pt.content = val;
            }
          });
          this.http.get<any>(`${environment.apiUrl}/public/content/${canonicalKey}?lang=en`).subscribe({
            next: (r) => {
              const val = r?.content || r?.payload?.content || '';
              this.tempEn.content = val;
              this.currentContent.en.content = val;
            }
          });
        } else {
          this.resetOrSetDefaultSection(sectionKey);
        }
      }
    });
  }

  startEditBlock(block: 'headline' | 'quote' | 'sectionTitle' | 'body'): void {
    this.editingBlock = block;
    this.tempPt = { ...this.currentContent.pt };
    this.tempEn = { ...this.currentContent.en };
    this.errorMessage = null;
    setTimeout(() => {
      const el = document.getElementById('field-pt-' + block);
      if (el) el.focus();
    }, 50);
  }

  cancelEditBlock(): void {
    this.editingBlock = null;
    const ptContent = this.tempPt.content;
    const enContent = this.tempEn.content;
    this.tempPt = { headline: '', quote: '', sectionTitle: '', body: '', content: ptContent };
    this.tempEn = { headline: '', quote: '', sectionTitle: '', body: '', content: enContent };
    this.errorMessage = null;
  }

  isBlockEmpty(block: 'headline' | 'quote' | 'sectionTitle' | 'body'): string | boolean {
    const pt = this.currentContent.pt[block];
    const en = this.currentContent.en[block];
    return (!pt || pt.trim() === '') && (!en || en.trim() === '');
  }

  getBlockStatusClass(block: 'headline' | 'quote' | 'sectionTitle' | 'body'): string {
    if (this.editingBlock === block) {
      return 'status-editing';
    }
    if (this.isBlockEmpty(block)) {
      return 'status-empty';
    }
    return 'status-ready';
  }

  getBlockStatusLabel(block: 'headline' | 'quote' | 'sectionTitle' | 'body'): string {
    if (this.editingBlock === block) {
      return 'Editando';
    }
    if (this.isBlockEmpty(block)) {
      return 'Vazio';
    }
    return 'Salvo';
  }

  saveBlock(block: 'headline' | 'quote' | 'sectionTitle' | 'body'): void {
    this.isSaving = true;
    this.errorMessage = null;

    if (this.editingBlock !== block) {
      this.tempPt[block] = this.currentContent.pt[block] || '';
      this.tempEn[block] = this.currentContent.en[block] || '';
    }

    const canonicalKey = this.getCanonicalKey(this.activeSection);

    // Atualiza apenas o bloco selecionado mantendo o restante
    const updatedPt: TranslationBlock = {
      ...this.currentContent.pt,
      [block]: this.tempPt[block]
    };

    const updatedEn: TranslationBlock = {
      ...this.currentContent.en,
      [block]: this.tempEn[block]
    };

    const payload = {
      pt: updatedPt,
      en: updatedEn
    };

    this.http.put(`${environment.apiUrl}/admin/institutional/translations/${canonicalKey}`, payload).subscribe({
      next: () => {
        this.isSaving = false;
        this.currentContent.pt[block] = this.tempPt[block];
        this.currentContent.en[block] = this.tempEn[block];
        this.showSuccessFeedback(block);
        this.cancelEditBlock();
      },
      error: (err) => {
        this.isSaving = false;
        const msg = err?.error?.message || err?.message || 'Falha ao sincronizar com o banco de dados.';
        this.errorMessage = `Erro ao salvar tradução: ${msg}`;
        console.error('Falha ao persistir tradução no servidor:', err);
      }
    });
  }

  saveCurrentSimpleSection(): void {
    this.isSaving = true;
    this.errorMessage = null;

    const canonicalKey = this.getCanonicalKey(this.activeSection);
    const contentPt = this.tempPt.content || '';
    const contentEn = this.tempEn.content || '';

    this.currentContent.pt.content = contentPt;
    this.currentContent.pt.body = contentPt;
    this.currentContent.en.content = contentEn;
    this.currentContent.en.body = contentEn;

    const payload = {
      translations: {
        pt: { content: contentPt },
        en: { content: contentEn }
      },
      pt: {
        content: contentPt
      },
      en: {
        content: contentEn
      }
    };

    this.http.put(`${environment.apiUrl}/admin/institutional/translations/${canonicalKey}`, payload).subscribe({
      next: () => {
        this.isSaving = false;
        this.showSuccessFeedback('all');
      },
      error: (err) => {
        this.isSaving = false;
        const msg = err?.error?.message || err?.message || 'Falha ao sincronizar com o banco de dados.';
        this.errorMessage = `Erro ao salvar conteúdo: ${msg}`;
        console.error('Falha ao persistir traduções:', err);
      }
    });
  }

  saveCurrentSection(): void {
    if (this.activeSection === 'TERMS' || this.activeSection === 'PRIVACY') {
      this.saveCurrentSimpleSection();
      return;
    }
    this.saveContent();
  }

  saveSimpleSection(): void {
    this.saveCurrentSimpleSection();
  }

  saveContent(): void {
    if (this.activeSection === 'SCOUTING') {
      this.saveFaqList();
      return;
    }

    if (this.activeSection === 'TERMS' || this.activeSection === 'PRIVACY') {
      this.saveCurrentSimpleSection();
      return;
    }

    this.isSaving = true;
    this.errorMessage = null;

    if (this.editingBlock) {
      this.currentContent.pt[this.editingBlock] = this.tempPt[this.editingBlock];
      this.currentContent.en[this.editingBlock] = this.tempEn[this.editingBlock];
    }

    const canonicalKey = this.getCanonicalKey(this.activeSection);
    const payload = {
      pt: this.currentContent.pt,
      en: this.currentContent.en
    };

    this.http.put(`${environment.apiUrl}/admin/institutional/translations/${canonicalKey}`, payload).subscribe({
      next: () => {
        this.isSaving = false;
        this.showSuccessFeedback('all');
        this.cancelEditBlock();
      },
      error: (err) => {
        this.isSaving = false;
        const msg = err?.error?.message || err?.message || 'Falha ao sincronizar com o banco de dados.';
        this.errorMessage = `Erro ao salvar conteúdo: ${msg}`;
        console.error('Falha ao persistir traduções:', err);
      }
    });
  }

  private showSuccessFeedback(block: string): void {
    this.successBlock = block;
    setTimeout(() => {
      if (this.successBlock === block) {
        this.successBlock = null;
      }
    }, 2500);
  }

  resetOrSetDefaultSection(sectionKey: string): void {
    const canonicalKey = this.getCanonicalKey(sectionKey);
    this.currentSectionTitle = this.getSectionTitleByKey(sectionKey);

    if (canonicalKey === 'PRIVACY' || canonicalKey === 'PRIVACY_POLICY') {
      const ptText = 'Política de Privacidade & Diretrizes LGPD (Lei nº 13.709/2018)...\n\nA WB Agency preza pela segurança, transparência e proteção rigorosa dos dados pessoais de seus modelos e visitantes.';
      const enText = 'Privacy Policy & GDPR/LGPD Compliance...\n\nWB Agency values security, transparency, and strict protection of personal data belonging to our talent and visitors.';
      this.currentContent = {
        pt: {
          headline: 'Privacidade e Proteção de Dados Pessoais',
          quote: 'Conformidade rigorosa com a LGPD e o Regulamento Geral de Proteção de Dados (GDPR).',
          sectionTitle: 'Proteção de Dados',
          body: ptText,
          content: ptText
        },
        en: {
          headline: 'Privacy Policy & Personal Data Protection',
          quote: 'Strict compliance with LGPD and General Data Protection Regulation (GDPR).',
          sectionTitle: 'Data Protection',
          body: enText,
          content: enText
        }
      };
      this.tempPt.content = ptText;
      this.tempEn.content = enText;
    } else if (canonicalKey === 'TERMS' || canonicalKey === 'TERMS_OF_USE') {
      const ptText = 'Termos e Condições de Uso da WB Agency...\n\nAo acessar e utilizar este website, você concorda com os termos aqui dispostos.';
      const enText = 'WB Agency Terms of Use...\n\nBy accessing and using this website, you agree to comply with the terms set forth herein.';
      this.currentContent = {
        pt: {
          headline: 'Termos e Condições de Uso da Plataforma',
          quote: 'Proteção patrimonial, segurança jurídica e transparência no agenciamento.',
          sectionTitle: 'Termos Gerais',
          body: ptText,
          content: ptText
        },
        en: {
          headline: 'Terms and Conditions of Platform Use',
          quote: 'Asset protection, legal compliance, and agency transparency.',
          sectionTitle: 'General Terms',
          body: enText,
          content: enText
        }
      };
      this.tempPt.content = ptText;
      this.tempEn.content = enText;
    } else if (canonicalKey === 'SCOUTING_GUIDELINES') {
      this.currentContent = {
        pt: {
          headline: 'Diretrizes de Scouting e Aplicação',
          quote: 'Critérios técnicos para submissão digital de novos talentos.',
          sectionTitle: 'Requisitos Fotográficos',
          body: 'Fotos polaroids com iluminação natural, rosto limpo sem maquiagem e roupas neutras. Não aceitamos fotos com filtros ou edições digitais.'
        },
        en: {
          headline: 'Scouting Guidelines & Application',
          quote: 'Technical criteria for digital submission of new talents.',
          sectionTitle: 'Polaroid Requirements',
          body: 'Natural light snapshots, clean face without makeup, and neutral wardrobe. We do not accept digitally filtered or altered photos.'
        }
      };
    } else {
      this.currentContent = {
        pt: {
          headline: 'A Nova Estética do Scouting Global',
          quote: 'Acreditamos na autenticidade, na força da personalidade e na beleza singular de cada indivíduo.',
          sectionTitle: 'Nossa Filosofia',
          body: 'A WB Agency consolidou-se como um núcleo editorial focado no desenvolvimento integral de modelos para os principais mercados da moda internacional. Nossa metodologia rejeita a padronização e prioriza a identidade visual autêntica, conectando talentos a marcas com relevância estética global.'
        },
        en: {
          headline: 'The New Aesthetic of Global Scouting',
          quote: 'We believe in authenticity, personal strength, and the unique beauty of every individual.',
          sectionTitle: 'Our Philosophy',
          body: 'WB Agency has established itself as an editorial powerhouse dedicated to the comprehensive development of models for premier global fashion markets. Our scouting methodology moves beyond mass standards to foster authentic personal identity, positioning talents at the intersection of high fashion and international relevance.'
        }
      };
    }
  }

  getSectionTitleByKey(key: string): string {
    switch (key) {
      case 'MANIFESTO':
      case 'ABOUT_MANIFESTO':
        return 'Sobre Nós';
      case 'SCOUTING':
      case 'SCOUTING_GUIDELINES':
      case 'APPLY_FORM':
        return 'Configurações do Form. de Quero ser Modelo';
      case 'APPLY':
      case 'APPLY_HOW_IT_WORKS':
        return 'Próximos Passos Apply (Como Funciona)';
      case 'TERMS':
      case 'TERMS_OF_USE':
        return 'Termos de Uso';
      case 'PRIVACY':
      case 'PRIVACY_POLICY':
        return 'Privacidade & LGPD';
      default:
        return 'Conteúdos Bilíngues';
    }
  }

  addFaqItem(): void {
    if (this.faqList.length < 10) {
      this.faqList.push({ question: '', answer: '', questionEn: '', answerEn: '' });
    }
  }

  removeFaqItem(index: number): void {
    if (this.faqList.length <= 1) {
      alert('A lista deve conter ao menos uma pergunta e resposta.');
      return;
    }
    this.faqList.splice(index, 1);
  }

  moveFaqUp(index: number): void {
    if (index > 0) {
      const item = this.faqList.splice(index, 1)[0];
      this.faqList.splice(index - 1, 0, item);
    }
  }

  moveFaqDown(index: number): void {
    if (index < this.faqList.length - 1) {
      const item = this.faqList.splice(index, 1)[0];
      this.faqList.splice(index + 1, 0, item);
    }
  }

  saveFaqList(): void {
    this.isSaving = true;
    this.errorMessage = null;

    this.http.put(`${environment.apiUrl}/admin/apply-faq`, { items: this.faqList }).subscribe({
      next: () => {
        this.isSaving = false;
        this.showSuccessFeedback('faq');
      },
      error: (err) => {
        this.isSaving = false;
        const msg = err?.error?.message || err?.message || 'Falha ao sincronizar FAQ com o servidor.';
        this.errorMessage = `Erro ao salvar FAQ: ${msg}`;
        console.error('Erro ao salvar FAQ:', err);
      }
    });
  }

  loadFaqList(): void {
    this.http.get<any>(`${environment.apiUrl}/admin/apply-faq`).subscribe({
      next: (res) => {
        if (res && res.items && Array.isArray(res.items) && res.items.length > 0) {
          this.faqList = res.items.map((item: any) => ({
            question: item.question || '',
            answer: item.answer || '',
            questionEn: item.questionEn || item.question_en || '',
            answerEn: item.answerEn || item.answer_en || ''
          }));
        } else if (Array.isArray(res) && res.length > 0) {
          this.faqList = res.map((item: any) => ({
            question: item.question || '',
            answer: item.answer || '',
            questionEn: item.questionEn || item.question_en || '',
            answerEn: item.answerEn || item.answer_en || ''
          }));
        } else {
          this.setDefaultFaqList();
        }
      },
      error: (err) => {
        console.warn('Erro ao carregar FAQ do servidor, utilizando lista padrão:', err);
        this.setDefaultFaqList();
      }
    });
  }

  setDefaultFaqList(): void {
    this.faqList = [
      {
        question: 'Quais são as medidas ideais para o mercado da moda e publicidade?',
        answer: 'Não existem medidas certas ou específicas. O importante é ter personalidade marcante, atitude e querer muito ser modelo.',
        questionEn: 'What are the ideal measurements for fashion and advertising?',
        answerEn: 'There are no strict specific measurements. What matters most is strong personality, attitude, and dedication.'
      },
      {
        question: 'Preciso ter experiência prévia ou curso de modelo?',
        answer: 'Não. A WB Agency desenvolve talentos desde o início, oferecendo direcionamento e preparação profissional.',
        questionEn: 'Do I need previous experience or a modeling course?',
        answerEn: 'No. WB Agency develops talent from the ground up, providing professional guidance and preparation.'
      },
      {
        question: 'Existe custo para enviar meu material de avaliação?',
        answer: 'Não cobramos nenhuma taxa para envio ou avaliação de material de novos talentos.',
        questionEn: 'Is there any fee to submit my portfolio for evaluation?',
        answerEn: 'We do not charge any fee for submitting or evaluating materials from new talent.'
      },
      {
        question: 'Como devem ser as fotos polaroids enviadas?',
        answer: 'Luz natural, sem maquiagem ou filtros, fundo neutro e roupas básicas (camiseta preta/branca e jeans).',
        questionEn: 'How should the polaroid photos be taken?',
        answerEn: 'Natural light, no makeup or filters, neutral background, and simple wardrobe (black/white tee and jeans).'
      },
      {
        question: 'Qual é o prazo de retorno após o envio do formulário?',
        answer: 'Nosso departamento de scouting avalia todos os materiais e entra em contato em até 15 dias caso haja interesse.',
        questionEn: 'What is the response timeframe after submitting the form?',
        answerEn: 'Our scouting department reviews all submissions and contacts selected candidates within 15 days.'
      }
    ];
  }
}

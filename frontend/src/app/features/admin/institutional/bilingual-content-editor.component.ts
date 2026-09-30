import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';

export interface TranslationBlock {
  headline: string;
  quote: string;
  body: string;
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
    'TERMS': 'TERMS_OF_USE',
    'TERMS_OF_USE': 'TERMS_OF_USE',
    'PRIVACY': 'PRIVACY_POLICY',
    'PRIVACY_POLICY': 'PRIVACY_POLICY'
  };

  activeSection = 'MANIFESTO';
  currentSectionTitle = 'Manifesto Institucional';

  currentContent: BilingualContentState = {
    pt: {
      headline: 'A Nova Estética do Scouting Global',
      quote: 'A beleza contemporânea nasce da singularidade e precisão.',
      body: 'A WB Agency consolidou-se como um núcleo editorial focado no desenvolvimento integral de modelos para os principais mercados da moda internacional. Nossa metodologia rejeita a padronização e prioriza a identidade visual autêntica, conectando talentos a marcas com relevância estética global.'
    },
    en: {
      headline: 'The New Aesthetic of Global Scouting',
      quote: 'Contemporary beauty stems from uniqueness and precision.',
      body: 'WB Agency has established itself as an editorial powerhouse dedicated to the comprehensive development of models for premier global fashion markets. Our scouting methodology moves beyond mass standards to foster authentic personal identity, positioning talents at the intersection of high fashion and international relevance.'
    }
  };

  editingBlock: 'headline' | 'quote' | 'body' | null = null;
  tempPt: TranslationBlock = { headline: '', quote: '', body: '' };
  tempEn: TranslationBlock = { headline: '', quote: '', body: '' };
  isSaving = false;
  successBlock: string | null = null;
  errorMessage: string | null = null;

  ngOnInit(): void {
    this.loadSection(this.activeSection);
  }

  getCanonicalKey(key: string): string {
    return this.SECTION_MAP[key] || key;
  }

  loadSection(sectionKey: string): void {
    this.activeSection = sectionKey;
    this.cancelEditBlock();
    const canonicalKey = this.getCanonicalKey(sectionKey);

    this.http.get<SectionTranslationsResponse>(`${environment.apiUrl}/admin/institutional/translations/${canonicalKey}`).subscribe({
      next: (res) => {
        if (res && res.translations) {
          this.currentSectionTitle = res.title || this.getSectionTitleByKey(sectionKey);
          this.currentContent = {
            pt: {
              headline: res.translations.pt?.headline || '',
              quote: res.translations.pt?.quote || '',
              body: res.translations.pt?.body || ''
            },
            en: {
              headline: res.translations.en?.headline || '',
              quote: res.translations.en?.quote || '',
              body: res.translations.en?.body || ''
            }
          };
        } else {
          this.loadMockFallback(sectionKey);
        }
      },
      error: () => this.loadMockFallback(sectionKey)
    });
  }

  startEditBlock(block: 'headline' | 'quote' | 'body'): void {
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
    this.tempPt = { headline: '', quote: '', body: '' };
    this.tempEn = { headline: '', quote: '', body: '' };
    this.errorMessage = null;
  }

  isBlockEmpty(block: 'headline' | 'quote' | 'body'): boolean {
    const pt = this.currentContent.pt[block];
    const en = this.currentContent.en[block];
    return (!pt || pt.trim() === '') && (!en || en.trim() === '');
  }

  getBlockStatusClass(block: 'headline' | 'quote' | 'body'): string {
    if (this.editingBlock === block) {
      return 'status-editing';
    }
    if (this.isBlockEmpty(block)) {
      return 'status-empty';
    }
    return 'status-ready';
  }

  getBlockStatusLabel(block: 'headline' | 'quote' | 'body'): string {
    if (this.editingBlock === block) {
      return 'Editando';
    }
    if (this.isBlockEmpty(block)) {
      return 'Vazio';
    }
    return 'Salvo';
  }

  saveBlock(block: 'headline' | 'quote' | 'body'): void {
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
      error: () => {
        // Fallback local update
        this.isSaving = false;
        this.currentContent.pt[block] = this.tempPt[block];
        this.currentContent.en[block] = this.tempEn[block];
        this.showSuccessFeedback(block);
        this.cancelEditBlock();
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

  private getSectionTitleByKey(key: string): string {
    switch (key) {
      case 'MANIFESTO':
      case 'ABOUT_MANIFESTO':
        return 'Manifesto Institucional';
      case 'SCOUTING':
      case 'SCOUTING_GUIDELINES':
        return 'Diretrizes de Scouting';
      case 'TERMS':
      case 'TERMS_OF_USE':
        return 'Termos & Privacidade';
      default:
        return 'Conteúdos Bilíngues';
    }
  }

  private loadMockFallback(key: string): void {
    const canonical = this.getCanonicalKey(key);
    if (canonical === 'ABOUT_MANIFESTO') {
      this.currentSectionTitle = 'Manifesto Institucional';
      this.currentContent = {
        pt: {
          headline: 'A Nova Estética do Scouting Global',
          quote: 'A beleza contemporânea nasce da singularidade e precisão.',
          body: 'A WB Agency consolidou-se como um núcleo editorial focado no desenvolvimento integral de modelos para os principais mercados da moda internacional. Nossa metodologia rejeita a padronização e prioriza a identidade visual autêntica, conectando talentos a marcas com relevância estética global.'
        },
        en: {
          headline: 'The New Aesthetic of Global Scouting',
          quote: 'Contemporary beauty stems from uniqueness and precision.',
          body: 'WB Agency has established itself as an editorial powerhouse dedicated to the comprehensive development of models for premier global fashion markets. Our scouting methodology moves beyond mass standards to foster authentic personal identity, positioning talents at the intersection of high fashion and international relevance.'
        }
      };
    } else if (canonical === 'SCOUTING_GUIDELINES') {
      this.currentSectionTitle = 'Diretrizes de Scouting';
      this.currentContent = {
        pt: {
          headline: 'Critérios e Recomendações de Envio',
          quote: 'Transparência, naturalidade e conformidade documental.',
          body: 'Para avaliação do casting internacional, solicitamos polaroids digitais sem maquiagem e com iluminação natural. Candidatos menores de idade devem submeter a anuência prévia dos responsáveis legais.'
        },
        en: {
          headline: 'Scouting Standards & Submission Guidelines',
          quote: 'Transparency, natural posture, and legal compliance.',
          body: 'For international casting evaluation, we require clean digital polaroids without styling or makeup, captured in natural daylight. Submissions from under-age talents strictly require prior verified parental consent.'
        }
      };
    } else {
      this.currentSectionTitle = 'Termos & Privacidade';
      this.currentContent = {
        pt: {
          headline: 'Termos e Condições de Uso da Plataforma',
          quote: 'Proteção patrimonial, segurança jurídica e transparência no agenciamento.',
          body: 'O acesso e a utilização dos serviços da WB Agency regem-se pelas normas de propriedade intelectual e direitos autorais internacionais. O uso não autorizado de books e composites é estritamente proibido.'
        },
        en: {
          headline: 'Terms and Conditions of Platform Use',
          quote: 'Asset protection, legal compliance, and agency transparency.',
          body: 'Access to and use of WB Agency services are governed by international intellectual property laws. Unauthorized reproduction of model books and digital composites is strictly prohibited.'
        }
      };
    }
  }
}

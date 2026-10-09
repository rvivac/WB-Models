import { Component, OnInit, inject, effect, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { TranslationService } from '../../../core/services/translation.service';
import { TranslatePipe } from '../../pipes/translate.pipe';
import { environment } from '../../../../environments/environment';

export interface SocialMediaItem {
  id?: string;
  name: string;
  url: string;
}

export interface ContactData {
  primaryEmail?: string | null;
  phone?: string | null;
  whatsapp?: string | null;
  socialMediaList?: SocialMediaItem[];
}

@Component({
  selector: 'app-footer',
  standalone: true,
  imports: [CommonModule, RouterLink, TranslatePipe],
  templateUrl: './footer.component.html',
  styleUrls: ['./footer.component.scss']
})
export class FooterComponent implements OnInit {
  private http = inject(HttpClient);
  public translate = inject(TranslationService);

  currentYear: number = new Date().getFullYear();

  // Controle de exibição do Modal Harmônico
  isModalOpen = signal<boolean>(false);
  modalTitle = signal<string>('');
  modalContent = signal<string>('');
  termsContent = signal<string>('');
  privacyContent = signal<string>('');
  isLoadingContent = signal<boolean>(false);

  contactData: ContactData = {
    primaryEmail: null,
    whatsapp: null,
    phone: null,
    socialMediaList: []
  };

  constructor() {
    // Recarrega os canais institucionais se houver troca de idioma em tempo de execução
    effect(() => {
      this.translate.currentLang();
      this.loadRealContactData();
    });
  }

  ngOnInit(): void {
    this.loadRealContactData();
  }

  loadRealContactData(): void {
    this.http.get<any>(`${environment.apiUrl}/public/institutional/contact`).subscribe({
      next: (res) => {
        if (res) {
          this.contactData = {
            primaryEmail: res.primaryEmail || res.email || null,
            whatsapp: res.whatsapp || null,
            phone: res.phone || null,
            socialMediaList: Array.isArray(res.socialMediaList) ? res.socialMediaList : []
          };
        }
      },
      error: (err) => {
        console.warn('[FOOTER] Não foi possível carregar contatos da API:', err);
        // PROIBIDO injetar e-mails ou telefones falsos aqui!
      }
    });
  }

  loadContactInfo(lang: string = 'pt'): void {
    this.loadRealContactData();
  }

  applyContactSettings(res: any): void {
    if (!res) return;
    this.contactData = {
      primaryEmail: res.primaryEmail || res.email || null,
      whatsapp: res.whatsapp || res.whatsappNumber || null,
      phone: res.phone || null,
      socialMediaList: Array.isArray(res.socialMediaList) ? res.socialMediaList : []
    };
  }

  cleanWhatsAppNumber(num?: string | null): string {
    return num ? num.replace(/\D/g, '') : '';
  }

  openInstitutionalModal(sectionKey: 'TERMS' | 'PRIVACY'): void {
    const currentLang = typeof this.translate.currentLang === 'function' ? this.translate.currentLang() : 'pt';
    const isEn = (currentLang || 'pt').startsWith('en');
    const fallbackKey = sectionKey === 'TERMS' ? 'footer.terms_content' : 'footer.privacy_content';

    // 1. Define o título do Modal
    this.modalTitle.set(
      sectionKey === 'TERMS' 
        ? (isEn ? 'Terms of Use' : 'Termos de Uso')
        : (isEn ? 'Privacy & LGPD' : 'Privacidade & LGPD')
    );

    // 2. Abre o modal com loading
    this.modalContent.set('');
    this.isLoadingContent.set(true);
    this.isModalOpen.set(true);

    const lang = isEn ? 'en' : 'pt';

    // 3. Consulta a API
    this.http.get<any>(`${environment.apiUrl}/public/content/${sectionKey}?lang=${lang}`).subscribe({
      next: (res) => {
        this.isLoadingContent.set(false);

        // 4. Desempacotamento resiliente de qualquer formato retornado pela API
        let extractedText = '';

        if (typeof res === 'string') {
          extractedText = res;
        } else if (res && typeof res === 'object') {
          extractedText = 
            res.content || 
            res.body || 
            res.text || 
            res.data?.content || 
            res.data?.body || 
            res.translations?.[lang]?.content || 
            res.translations?.[lang]?.body || 
            res.payload?.content ||
            res.payload?.body ||
            res.payload?.text ||
            res.payloadPt?.content || 
            res.payloadEn?.content || 
            '';
        }

        // 5. Se o texto extraído for válido, exibe. Se vier vazio, usa o fallback i18n:
        if (extractedText && typeof extractedText === 'string' && extractedText.trim().length > 0) {
          const trimmed = extractedText.trim();
          if (sectionKey === 'TERMS') {
            this.termsContent.set(trimmed);
          } else {
            this.privacyContent.set(trimmed);
          }
          this.modalContent.set(trimmed);
        } else {
          const fallback = this.translate.instant(fallbackKey);
          if (sectionKey === 'TERMS') {
            this.termsContent.set(fallback);
          } else {
            this.privacyContent.set(fallback);
          }
          this.modalContent.set(fallback);
        }
      },
      error: (err) => {
        console.warn(`[FOOTER] Erro ao carregar ${sectionKey} da API. Aplicando fallback local:`, err);
        this.isLoadingContent.set(false);
        const fallback = this.translate.instant(fallbackKey);
        if (sectionKey === 'TERMS') {
          this.termsContent.set(fallback);
        } else {
          this.privacyContent.set(fallback);
        }
        this.modalContent.set(fallback);
      }
    });
  }

  closeModal(): void {
    this.isModalOpen.set(false);
  }
}

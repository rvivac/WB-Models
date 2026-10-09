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
  primaryEmail: string;
  phone?: string;
  whatsapp?: string;
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
  isLoadingContent = signal<boolean>(false);

  contactData: ContactData = {
    primaryEmail: 'contato@wbscouting.com',
    whatsapp: '+55 11 99999-9999',
    socialMediaList: [
      { name: 'Instagram', url: 'https://instagram.com/wbagency' },
      { name: 'LinkedIn', url: 'https://linkedin.com/company/wbagency' }
    ]
  };

  constructor() {
    // Recarrega os canais institucionais se houver troca de idioma em tempo de execução
    effect(() => {
      const lang = this.translate.currentLang();
      this.loadContactInfo(lang);
    });
  }

  ngOnInit(): void {
    // Inicialização orquestrada reativamente pelo constructor effect()
  }

  loadContactInfo(lang: string = 'pt'): void {
    const langParam = lang === 'en' ? 'en' : 'pt';
    this.http.get<any>(`${environment.apiUrl}/public/institutional/contact?lang=${langParam}`).subscribe({
      next: (res) => {
        if (res) this.applyContactSettings(res);
      },
      error: () => {
        this.http.get<any[]>(`${environment.apiUrl}/public/contact-channels?lang=${langParam}`).subscribe({
          next: (channels) => {
            if (Array.isArray(channels) && channels.length > 0) {
              this.mapFromContactChannels(channels);
            }
          },
          error: (err) => console.warn('[FOOTER] Usando contatos padrão:', err)
        });
      }
    });
  }

  applyContactSettings(res: any): void {
    this.contactData.primaryEmail = res.primaryEmail || res.email || this.contactData.primaryEmail;
    this.contactData.whatsapp = res.whatsapp || res.whatsappNumber || this.contactData.whatsapp;
    this.contactData.phone = res.phone || this.contactData.phone;

    if (Array.isArray(res.socialMediaList) && res.socialMediaList.length > 0) {
      this.contactData.socialMediaList = res.socialMediaList;
    } else if (res.socialMedia && typeof res.socialMedia === 'object') {
      const list: SocialMediaItem[] = [];
      Object.entries(res.socialMedia).forEach(([key, val]) => {
        if (val && typeof val === 'string') {
          list.push({ name: key.toUpperCase(), url: val });
        }
      });
      if (list.length > 0) this.contactData.socialMediaList = list;
    }
  }

  mapFromContactChannels(channels: any[]): void {
    const emailChannel = channels.find(c => c.type === 'EMAIL' || c.channelName?.toLowerCase().includes('email'));
    if (emailChannel?.value) this.contactData.primaryEmail = emailChannel.value;

    const waChannel = channels.find(c => c.type === 'WHATSAPP' || c.channelName?.toLowerCase().includes('whatsapp'));
    if (waChannel?.value) this.contactData.whatsapp = waChannel.value;

    const socials = channels.filter(c => c.type === 'SOCIAL');
    if (socials.length > 0) {
      this.contactData.socialMediaList = socials.map(s => ({
        name: s.label || s.channelName || 'Link',
        url: s.value || s.url
      }));
    }
  }

  cleanWhatsAppNumber(num?: string): string {
    return num ? num.replace(/\D/g, '') : '5511999999999';
  }

  openInstitutionalModal(sectionKey: 'TERMS' | 'PRIVACY'): void {
    const currentLang = typeof this.translate.currentLang === 'function' ? this.translate.currentLang() : 'pt';
    const isEn = (currentLang || 'pt').startsWith('en');
    this.modalTitle.set(
      sectionKey === 'TERMS' 
        ? (isEn ? 'Terms of Use' : 'Termos de Uso')
        : (isEn ? 'Privacy & LGPD' : 'Privacidade & LGPD')
    );
    this.modalContent.set('');
    this.isLoadingContent.set(true);
    this.isModalOpen.set(true);

    const lang = isEn ? 'en' : 'pt';
    this.http.get<any>(`${environment.apiUrl}/public/content/${sectionKey}?lang=${lang}`).subscribe({
      next: (res) => {
        this.isLoadingContent.set(false);
        const text = res?.content || res?.payload?.content || (typeof res === 'string' ? res : '');
        this.modalContent.set(text || (isEn ? 'Content temporarily unavailable.' : 'Conteúdo temporariamente indisponível.'));
      },
      error: () => {
        this.isLoadingContent.set(false);
        this.modalContent.set(isEn ? 'Unable to load content at this time.' : 'Não foi possível carregar os dados no momento.');
      }
    });
  }

  closeModal(): void {
    this.isModalOpen.set(false);
  }
}

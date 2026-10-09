import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
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
  imports: [CommonModule, RouterLink],
  templateUrl: './footer.component.html',
  styleUrls: ['./footer.component.scss']
})
export class FooterComponent implements OnInit {
  private http = inject(HttpClient);

  currentYear: number = new Date().getFullYear();

  // Valores padrão/fallback caso a API esteja carregando
  contactData: ContactData = {
    primaryEmail: 'contato@wbscouting.com',
    whatsapp: '+55 11 99999-9999',
    socialMediaList: [
      { name: 'Instagram', url: 'https://instagram.com/wbagency' },
      { name: 'LinkedIn', url: 'https://linkedin.com/company/wbagency' }
    ]
  };

  ngOnInit(): void {
    this.loadContactInfo();
  }

  loadContactInfo(): void {
    // Tenta carregar as configurações institucionais salvas no banco
    this.http.get<any>(`${environment.apiUrl}/public/institutional/contact`).subscribe({
      next: (res) => {
        if (res) {
          this.applyContactSettings(res);
        }
      },
      error: () => {
        // Fallback: tenta a rota alternativa de canais de contato
        this.http.get<any>(`${environment.apiUrl}/public/contact-channels`).subscribe({
          next: (channels) => {
            if (Array.isArray(channels) && channels.length > 0) {
              this.mapFromContactChannels(channels);
            } else if (channels && typeof channels === 'object') {
              this.applyContactSettings(channels);
            }
          },
          error: (err) => console.warn('[FOOTER] Utilizando contatos padrão:', err)
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
}

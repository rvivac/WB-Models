import { Injectable, inject, signal } from '@angular/core';
import { PublicContentService, ContactChannelsPublicDto } from './public-content.service';

export function formatPhoneNumber(phone?: string): string {
  if (!phone) return '';
  const digits = phone.replace(/\D/g, '');
  if (digits.length === 13 && digits.startsWith('55')) {
    return `+${digits.slice(0, 2)} (${digits.slice(2, 4)}) ${digits.slice(4, 9)}-${digits.slice(9)}`;
  }
  if (digits.length === 12 && digits.startsWith('55')) {
    return `+${digits.slice(0, 2)} (${digits.slice(2, 4)}) ${digits.slice(4, 8)}-${digits.slice(8)}`;
  }
  if (digits.length === 11) {
    return `+55 (${digits.slice(0, 2)}) ${digits.slice(2, 7)}-${digits.slice(7)}`;
  }
  if (digits.length === 10) {
    return `+55 (${digits.slice(0, 2)}) ${digits.slice(2, 6)}-${digits.slice(6)}`;
  }
  return phone;
}

export function formatInstagramHandle(handle?: string): string {
  if (!handle) return '@wbagency';
  let clean = handle.trim();
  clean = clean.replace(/^https?:\/\/(www\.)?instagram\.com\//i, '').replace(/\/$/, '');
  if (!clean.startsWith('@')) {
    clean = '@' + clean;
  }
  return clean;
}

export function formatInstagramUrl(handle?: string): string {
  if (!handle) return 'https://www.instagram.com/wbagency/';
  let clean = handle.trim();
  if (clean.startsWith('http://') || clean.startsWith('https://')) {
    return clean.endsWith('/') ? clean : clean + '/';
  }
  const username = clean.replace(/^@/, '').replace(/\/$/, '');
  return `https://www.instagram.com/${username}/`;
}

@Injectable({
  providedIn: 'root'
})
export class SecureContactService {
  private readonly publicContentService = inject(PublicContentService);

  private readonly _email = signal<string>('info@wbagency.com.br');
  private readonly _phone = signal<string>('+55 (11) 97065-6003');
  private readonly _whatsappUrl = signal<string>('https://wa.me/5511970656003');
  private readonly _instagramHandle = signal<string>('@wbagency');
  private readonly _instagramUrl = signal<string>('https://www.instagram.com/wbagency/');

  constructor() {
    this.loadContactChannels();
  }

  loadContactChannels(lang: string = 'pt'): void {
    this.publicContentService.getContactChannels(lang).subscribe({
      next: (channels: ContactChannelsPublicDto) => {
        if (channels) {
          if (channels.email) {
            this._email.set(channels.email);
          }
          if (channels.whatsappNumber) {
            this._phone.set(formatPhoneNumber(channels.whatsappNumber));
          }
          if (channels.whatsappUrl) {
            this._whatsappUrl.set(channels.whatsappUrl);
          } else if (channels.whatsappNumber) {
            const raw = channels.whatsappNumber.replace(/\D/g, '');
            this._whatsappUrl.set(`https://wa.me/${raw}`);
          }
          if (channels.instagramHandle) {
            this._instagramHandle.set(formatInstagramHandle(channels.instagramHandle));
            this._instagramUrl.set(formatInstagramUrl(channels.instagramHandle));
          }
        }
      },
      error: (err) => {
        console.warn('Erro ao carregar canais institucionais no SecureContactService:', err);
      }
    });
  }

  get emailDisplay(): string {
    return this._email();
  }

  get phoneDisplay(): string {
    return this._phone();
  }

  get whatsappUrl(): string {
    return this._whatsappUrl();
  }

  get instagramDisplay(): string {
    return this._instagramHandle();
  }

  get instagramUrl(): string {
    return this._instagramUrl();
  }

  getContactChannels() {
    return {
      email: this.emailDisplay,
      whatsappNumber: this.phoneDisplay,
      whatsappUrl: this.whatsappUrl,
      instagramHandle: this.instagramDisplay
    };
  }

  openMail(): void {
    const target = this._email();
    window.location.href = `mailto:${target}?subject=Contato%20Comercial%20-%20WB%20Agency`;
  }

  openWhatsApp(): void {
    window.open(this._whatsappUrl(), '_blank', 'noopener,noreferrer');
  }

  openInstagram(): void {
    window.open(this._instagramUrl(), '_blank', 'noopener,noreferrer');
  }
}


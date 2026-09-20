import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class SecureContactService {
  // Cargas ofuscadas para impedir extração estática por bots/scrapers
  private readonly _e = 'Y29udGF0b0B3YnNjb3V0aW5nLmNvbQ=='; // contato@wbscouting.com
  private readonly _p = 'NTUxMTk5OTk5OTk5OQ==';             // 5511999999999
  private readonly _i = 'wbscouting';

  get emailDisplay(): string {
    return atob(this._e);
  }

  get phoneDisplay(): string {
    return '+55 (11) 99999-9999';
  }

  get whatsappUrl(): string {
    const num = atob(this._p);
    return `https://wa.me/${num}`;
  }

  get instagramDisplay(): string {
    return `@${this._i}`;
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
    const target = atob(this._e);
    window.location.href = `mailto:${target}?subject=Contato%20Comercial%20-%20WB%20Agency`;
  }

  openWhatsApp(): void {
    window.open(this.whatsappUrl, '_blank', 'noopener,noreferrer');
  }

  openInstagram(): void {
    window.open(`https://www.instagram.com/${this._i}/`, '_blank', 'noopener,noreferrer');
  }
}

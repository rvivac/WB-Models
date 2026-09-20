import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class SecureContactService {
  // Cargas ofuscadas para impedir extração estática por bots/scrapers
  private readonly _e = 'aW5mb3dic2NvdXRpbmdAZ21haWwuY29t'; // infowbscouting@gmail.com
  private readonly _p = 'NTUxMTk3MDY1NjAwMw==';             // 5511970656003
  private readonly _i = 'infowbagency';

  get emailDisplay(): string {
    return atob(this._e);
  }

  get phoneDisplay(): string {
    return '+55 11 97065-6003';
  }

  get instagramDisplay(): string {
    return `@${this._i}`;
  }

  openMail(): void {
    const target = atob(this._e);
    window.location.href = `mailto:${target}?subject=Contato%20Comercial%20-%20WB%20Agency`;
  }

  openWhatsApp(): void {
    const num = atob(this._p);
    const msg = encodeURIComponent('Olá! Gostaria de falar com o departamento de casting/bookers da WB Agency.');
    window.open(`https://api.whatsapp.com/send?phone=${num}&text=${msg}`, '_blank', 'noopener,noreferrer');
  }

  openInstagram(): void {
    window.open(`https://www.instagram.com/${this._i}/`, '_blank', 'noopener,noreferrer');
  }
}

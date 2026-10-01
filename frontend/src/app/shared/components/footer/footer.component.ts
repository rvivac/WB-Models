import { Component, OnInit, inject, signal, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { PublicContentService, ContactChannelsPublicDto } from '../../../core/services/public-content.service';
import { TranslationService } from '../../../core/services/translation.service';
import { TranslatePipe } from '../../pipes/translate.pipe';

@Component({
  selector: 'app-footer',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslatePipe],
  templateUrl: './footer.component.html',
  styleUrls: ['./footer.component.scss']
})
export class FooterComponent implements OnInit {
  private readonly publicContentService = inject(PublicContentService);
  private readonly translationService = inject(TranslationService);

  readonly currentYear = new Date().getFullYear();
  readonly contactChannels = signal<ContactChannelsPublicDto>({
    email: 'info@wbagency.com.br',
    whatsappNumber: '5511970656003',
    whatsappUrl: 'https://wa.me/5511970656003?text=Ol%C3%A1%21',
    instagramHandle: '@wbagency',
    address: 'São Paulo - SP, Brasil',
    officeHours: 'Segunda a Sexta, das 09h às 18h'
  });

  constructor() {
    effect(() => {
      const currentLang = this.translationService.currentLang();
      this.loadContactChannels(currentLang);
    });
  }

  ngOnInit(): void {
    this.loadContactChannels(this.translationService.currentLang());
  }

  private loadContactChannels(lang: string): void {
    this.publicContentService.getContactChannels(lang).subscribe({
      next: (data) => {
        if (data) {
          this.contactChannels.set(data);
        }
      },
      error: (err) => {
        console.warn('Erro ao carregar canais de contato no footer:', err);
      }
    });
  }

  formatPhone(phone?: string): string {
    if (!phone) return '';
    const digits = phone.replace(/\D/g, '');
    if (digits.length === 13 && digits.startsWith('55')) {
      return `+${digits.slice(0, 2)} (${digits.slice(2, 4)}) ${digits.slice(4, 9)}-${digits.slice(9)}`;
    }
    if (digits.length === 11) {
      return `+55 (${digits.slice(0, 2)}) ${digits.slice(2, 7)}-${digits.slice(7)}`;
    }
    return phone;
  }

  getInstagramUrl(): string {
    const handle = this.contactChannels().instagramHandle || '@wbagency';
    if (handle.startsWith('http://') || handle.startsWith('https://')) {
      return handle;
    }
    return `https://instagram.com/${handle.replace('@', '')}`;
  }
}

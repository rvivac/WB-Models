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
    email: 'contato@wbscouting.com',
    whatsappNumber: '5511999999999',
    whatsappUrl: 'https://wa.me/5511999999999?text=Ol%C3%A1%21',
    instagramHandle: '@wbscouting',
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

  getInstagramUrl(): string {
    const handle = this.contactChannels().instagramHandle || '@wbscouting';
    return `https://instagram.com/${handle.replace('@', '')}`;
  }
}

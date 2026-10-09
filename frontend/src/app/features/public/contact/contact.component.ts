import { Component, OnInit, inject, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { SecureContactService } from '../../../core/services/secure-contact.service';
import { TranslationService } from '../../../core/services/translation.service';
import { TranslatePipe } from '../../../shared/pipes/translate.pipe';

@Component({
  selector: 'app-contact',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslatePipe],
  templateUrl: './contact.component.html',
  styleUrls: ['./contact.component.scss']
})
export class ContactComponent implements OnInit {
  private readonly router = inject(Router);
  readonly secureContact = inject(SecureContactService);
  private readonly translationService = inject(TranslationService);

  constructor() {
    effect(() => {
      const currentLang = this.translationService.currentLang();
      this.secureContact.loadContactChannels(currentLang);
    });
  }

  ngOnInit(): void {
    this.secureContact.loadContactChannels(this.translationService.currentLang());
  }

  onWhatsAppClick(): void {
    this.secureContact.openWhatsApp();
  }

  onPhoneClick(): void {
    this.secureContact.openPhone();
  }

  onMailClick(): void {
    this.secureContact.openMail();
  }

  onInstagramClick(): void {
    this.secureContact.openInstagram();
  }

  navigateToCasting(): void {
    this.router.navigate(['/seja-modelo']);
  }
}

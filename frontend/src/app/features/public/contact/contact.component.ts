import { Component, OnInit, inject, signal, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterModule, Router } from '@angular/router';
import { ButtonComponent } from '../../../shared/components/button/button.component';
import { SecureContactService } from '../../../core/services/secure-contact.service';
import { TranslationService } from '../../../core/services/translation.service';
import { TranslatePipe } from '../../../shared/pipes/translate.pipe';

@Component({
  selector: 'app-contact',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule, ButtonComponent, TranslatePipe],
  templateUrl: './contact.component.html',
  styleUrls: ['./contact.component.scss']
})
export class ContactComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  readonly secureContact = inject(SecureContactService);
  private readonly translationService = inject(TranslationService);

  readonly isSubmitted = signal<boolean>(false);
  readonly isSending = signal<boolean>(false);

  constructor() {
    effect(() => {
      const currentLang = this.translationService.currentLang();
      this.secureContact.loadContactChannels(currentLang);
    });
  }

  ngOnInit(): void {
    this.secureContact.loadContactChannels(this.translationService.currentLang());
  }

  readonly contactForm: FormGroup = this.fb.group({
    name: ['', [Validators.required]],
    company: [''],
    email: ['', [Validators.required, Validators.email]],
    phone: ['', [Validators.required]],
    interestType: ['BOOKING', [Validators.required]],
    message: ['', [Validators.required, Validators.minLength(10)]]
  });

  onWhatsAppClick(): void {
    this.secureContact.openWhatsApp();
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

  onSubmit(): void {
    if (this.contactForm.invalid) {
      this.contactForm.markAllAsTouched();
      return;
    }

    this.isSending.set(true);
    // Simulação de envio seguro com feedback
    setTimeout(() => {
      this.isSending.set(false);
      this.isSubmitted.set(true);
      this.contactForm.reset({
        interestType: 'BOOKING'
      });
    }, 800);
  }
}

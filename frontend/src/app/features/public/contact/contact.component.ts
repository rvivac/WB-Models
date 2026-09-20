import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterModule, Router } from '@angular/router';
import { ButtonComponent } from '../../../shared/components/button/button.component';
import { SecureContactService } from '../../../core/services/secure-contact.service';

@Component({
  selector: 'app-contact',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule, ButtonComponent],
  templateUrl: './contact.component.html',
  styleUrls: ['./contact.component.scss']
})
export class ContactComponent {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  readonly secureContact = inject(SecureContactService);

  readonly isSubmitted = signal<boolean>(false);
  readonly isSending = signal<boolean>(false);

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

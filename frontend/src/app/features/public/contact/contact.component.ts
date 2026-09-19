import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ButtonComponent } from '../../../shared/components/button/button.component';

@Component({
  selector: 'app-contact',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, ButtonComponent],
  templateUrl: './contact.component.html',
  styleUrls: ['./contact.component.scss']
})
export class ContactComponent {
  readonly isSubmitted = signal<boolean>(false);
  readonly isSending = signal<boolean>(false);

  readonly contactForm: FormGroup;

  constructor(private fb: FormBuilder) {
    this.contactForm = this.fb.group({
      name: ['', [Validators.required]],
      company: [''],
      email: ['', [Validators.required, Validators.email]],
      phone: ['', [Validators.required]],
      interestType: ['BOOKING', [Validators.required]],
      message: ['', [Validators.required, Validators.minLength(10)]]
    });
  }

  onSubmit(): void {
    if (this.contactForm.invalid) {
      this.contactForm.markAllAsTouched();
      return;
    }

    this.isSending.set(true);
    // Simulação de envio
    setTimeout(() => {
      this.isSending.set(false);
      this.isSubmitted.set(true);
      this.contactForm.reset();
    }, 800);
  }
}

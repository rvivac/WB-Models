import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ApiService } from '../../../core/services/api.service';
import { ButtonComponent } from '../../../shared/components/button/button.component';
import { CandidateApplicationRequest, CandidatePhotoUpload } from '../../../shared/models/candidate.interface';

@Component({
  selector: 'app-apply',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, ButtonComponent],
  templateUrl: './apply.component.html',
  styleUrls: ['./apply.component.scss']
})
export class ApplyComponent {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(ApiService);

  readonly currentStep = signal<number>(1);
  readonly isSubmitting = signal<boolean>(false);
  readonly submissionSuccess = signal<boolean>(false);
  readonly submissionError = signal<string | null>(null);

  readonly uploadedPhotos = signal<CandidatePhotoUpload[]>([]);

  readonly applyForm: FormGroup = this.fb.group({
    fullName: ['', [Validators.required, Validators.maxLength(200)]],
    email: ['', [Validators.required, Validators.email]],
    phone: ['', [Validators.required]],
    age: [null, [Validators.required, Validators.min(1), Validators.max(99)]],
    guardianName: [''],
    gender: ['FEMALE', [Validators.required]],
    heightCm: [null, [Validators.required, Validators.min(100), Validators.max(250)]],
    weightKg: [null],
    bustChestCm: [null],
    waistCm: [null],
    hipsCm: [null],
    instagramHandle: [''],
    tiktokHandle: [''],
    lgpdAccepted: [false, [Validators.requiredTrue]]
  });

  nextStep(): void {
    if (this.currentStep() === 1) {
      const step1Fields = ['fullName', 'email', 'phone', 'age', 'gender'];
      const isValid = step1Fields.every(field => this.applyForm.get(field)?.valid);
      if (!isValid) {
        step1Fields.forEach(f => this.applyForm.get(f)?.markAsTouched());
        return;
      }
    } else if (this.currentStep() === 2) {
      const step2Fields = ['heightCm'];
      const isValid = step2Fields.every(field => this.applyForm.get(field)?.valid);
      if (!isValid) {
        step2Fields.forEach(f => this.applyForm.get(f)?.markAsTouched());
        return;
      }
    }

    this.currentStep.update(s => Math.min(s + 1, 3));
  }

  prevStep(): void {
    this.currentStep.update(s => Math.max(s - 1, 1));
  }

  onPhotoSelected(event: Event, position: number): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files[0]) {
      const file = input.files[0];
      const reader = new FileReader();

      reader.onload = () => {
        const fileUrl = reader.result as string;
        const newPhoto: CandidatePhotoUpload = {
          photoPosition: position,
          fileUrl,
          filePath: `candidates/upload_${Date.now()}_pos${position}.jpg`
        };

        this.uploadedPhotos.update(photos => {
          const filtered = photos.filter(p => p.photoPosition !== position);
          return [...filtered, newPhoto].sort((a, b) => a.photoPosition - b.photoPosition);
        });
      };

      reader.readAsDataURL(file);
    }
  }

  removePhoto(position: number): void {
    this.uploadedPhotos.update(photos => photos.filter(p => p.photoPosition !== position));
  }

  getPhotoByPosition(position: number): CandidatePhotoUpload | undefined {
    return this.uploadedPhotos().find(p => p.photoPosition === position);
  }

  onSubmit(): void {
    if (this.applyForm.invalid) {
      this.applyForm.markAllAsTouched();
      return;
    }

    if (this.uploadedPhotos().length === 0) {
      this.submissionError.set('Por favor, envie pelo menos 1 foto para avaliação.');
      return;
    }

    this.isSubmitting.set(true);
    this.submissionError.set(null);

    const payload: CandidateApplicationRequest = {
      ...this.applyForm.value,
      photos: this.uploadedPhotos()
    };

    this.api.post('/candidates', payload).subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.submissionSuccess.set(true);
      },
      error: (err) => {
        this.isSubmitting.set(false);
        console.error('Erro na submissão de candidatura:', err);
        this.submissionError.set('Ocorreu um erro ao enviar sua ficha. Por favor, revise os dados e tente novamente.');
      }
    });
  }
}

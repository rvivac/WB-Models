import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { CandidateSubmissionService } from '../../core/services/candidate-submission.service';
import {
  CandidateSubmissionModel,
  CandidateSubmissionResponse,
  SubmissionGender
} from '../../shared/models/candidate-submission.interface';

interface PhotoSlot {
  key: 'facePhoto' | 'profilePhoto' | 'fullBodyPhoto';
  label: string;
  description: string;
  file: File | null;
  previewUrl: string | null;
  error: string | null;
}

@Component({
  selector: 'app-candidate-submission-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './candidate-submission-form.component.html',
  styleUrls: ['./candidate-submission-form.component.scss']
})
export class CandidateSubmissionFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly submissionService = inject(CandidateSubmissionService);

  readonly isSubmitting = signal<boolean>(false);
  readonly submissionSuccess = signal<CandidateSubmissionResponse | null>(null);
  readonly submissionError = signal<string | null>(null);
  readonly isUnderage = signal<boolean>(false);
  readonly showLgpdModal = signal<boolean>(false);

  readonly photoSlots = signal<Record<'facePhoto' | 'profilePhoto' | 'fullBodyPhoto', PhotoSlot>>({
    facePhoto: {
      key: 'facePhoto',
      label: 'Foto de Rosto (Close-up)',
      description: 'Luz natural, sem maquiagem pesada ou filtros. Olhar direto para a câmera.',
      file: null,
      previewUrl: null,
      error: null
    },
    profilePhoto: {
      key: 'profilePhoto',
      label: 'Foto de Perfil',
      description: 'Perfil lateral claro de 90 graus, cabelo preso atrás da orelha.',
      file: null,
      previewUrl: null,
      error: null
    },
    fullBodyPhoto: {
      key: 'fullBodyPhoto',
      label: 'Foto de Corpo Inteiro',
      description: 'Roupas justas neutras (jeans e regata), postura ereta e fundo neutro.',
      file: null,
      previewUrl: null,
      error: null
    }
  });

  submissionForm!: FormGroup;

  readonly genderOptions: { label: string; value: SubmissionGender }[] = [
    { label: 'Feminino', value: 'FEMALE' },
    { label: 'Masculino', value: 'MALE' },
    { label: 'Não-Binário', value: 'NON_BINARY' },
    { label: 'Outro', value: 'OTHER' }
  ];

  ngOnInit(): void {
    this.initForm();
    this.setupAgeListener();
  }

  private initForm(): void {
    this.submissionForm = this.fb.group({
      // 1. Dados Pessoais & Contato
      fullName: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(120)]],
      email: ['', [Validators.required, Validators.email, Validators.maxLength(100)]],
      phone: ['', [
        Validators.required,
        Validators.pattern(/^(\+?[1-9]\d{1,14}|\(?\d{2}\)?\s?\d{4,5}-?\d{4})$/)
      ]],
      birthDate: ['', [Validators.required]],
      gender: ['FEMALE' as SubmissionGender, [Validators.required]],

      // 2. Responsável Legal (condicional para < 18 anos)
      guardianName: [''],
      guardianPhone: [''],
      guardianEmail: [''],

      // 3. Localização
      city: ['', [Validators.required, Validators.maxLength(80)]],
      state: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(2), Validators.pattern(/^[A-Za-z]{2}$/)]],

      // 4. Medidas Físicas
      height: [null, [Validators.required, Validators.min(1.20), Validators.max(2.30)]],
      bust: [null, [Validators.min(30), Validators.max(200)]],
      waist: [null, [Validators.min(30), Validators.max(200)]],
      hips: [null, [Validators.min(30), Validators.max(200)]],
      shoeSize: [null, [Validators.min(30), Validators.max(50)]],
      eyeColor: ['', [Validators.maxLength(50)]],
      hairColor: ['', [Validators.maxLength(50)]],
      instagramHandle: ['', [Validators.maxLength(80)]],

      // 5. LGPD
      lgpdConsent: [false, [Validators.requiredTrue]]
    });
  }

  private setupAgeListener(): void {
    this.submissionForm.get('birthDate')?.valueChanges.subscribe(value => {
      if (!value) {
        this.isUnderage.set(false);
        this.updateGuardianValidators(false);
        return;
      }

      const birth = new Date(value);
      const today = new Date();
      let age = today.getFullYear() - birth.getFullYear();
      const monthDiff = today.getMonth() - birth.getMonth();
      if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birth.getDate())) {
        age--;
      }

      const underage = age < 18 && age >= 0;
      this.isUnderage.set(underage);
      this.updateGuardianValidators(underage);
    });
  }

  private updateGuardianValidators(underage: boolean): void {
    const guardianName = this.submissionForm.get('guardianName');
    const guardianPhone = this.submissionForm.get('guardianPhone');
    const guardianEmail = this.submissionForm.get('guardianEmail');

    if (underage) {
      guardianName?.setValidators([Validators.required, Validators.maxLength(120)]);
      guardianPhone?.setValidators([Validators.required, Validators.pattern(/^(\+?[1-9]\d{1,14}|\(?\d{2}\)?\s?\d{4,5}-?\d{4})$/)]);
      guardianEmail?.setValidators([Validators.required, Validators.email, Validators.maxLength(100)]);
    } else {
      guardianName?.clearValidators();
      guardianPhone?.clearValidators();
      guardianEmail?.clearValidators();
    }

    guardianName?.updateValueAndValidity();
    guardianPhone?.updateValueAndValidity();
    guardianEmail?.updateValueAndValidity();
  }

  onFileSelected(event: Event, key: 'facePhoto' | 'profilePhoto' | 'fullBodyPhoto'): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) {
      return;
    }

    const file = input.files[0];
    this.validateAndSetPhoto(key, file);
  }

  onFileDropped(event: DragEvent, key: 'facePhoto' | 'profilePhoto' | 'fullBodyPhoto'): void {
    event.preventDefault();
    event.stopPropagation();

    if (!event.dataTransfer?.files || event.dataTransfer.files.length === 0) {
      return;
    }

    const file = event.dataTransfer.files[0];
    this.validateAndSetPhoto(key, file);
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
  }

  private validateAndSetPhoto(key: 'facePhoto' | 'profilePhoto' | 'fullBodyPhoto', file: File): void {
    const validMimes = ['image/jpeg', 'image/jpg', 'image/png'];
    const maxBytes = 5 * 1024 * 1024; // 5 MB

    let error: string | null = null;
    if (!validMimes.includes(file.type.toLowerCase())) {
      error = 'Formato inválido. Apenas imagens JPEG e PNG são aceitas.';
    } else if (file.size > maxBytes) {
      error = 'Arquivo muito grande. O limite máximo por foto é de 5 MB.';
    }

    if (error) {
      this.updatePhotoSlot(key, { file: null, previewUrl: null, error });
      return;
    }

    const reader = new FileReader();
    reader.onload = () => {
      this.updatePhotoSlot(key, {
        file,
        previewUrl: reader.result as string,
        error: null
      });
    };
    reader.readAsDataURL(file);
  }

  removePhoto(key: 'facePhoto' | 'profilePhoto' | 'fullBodyPhoto', event?: Event): void {
    if (event) {
      event.stopPropagation();
    }
    this.updatePhotoSlot(key, {
      file: null,
      previewUrl: null,
      error: null
    });
  }

  private updatePhotoSlot(key: 'facePhoto' | 'profilePhoto' | 'fullBodyPhoto', partial: Partial<PhotoSlot>): void {
    this.photoSlots.update(slots => {
      const current = slots[key];
      return {
        ...slots,
        [key]: { ...current, ...partial }
      };
    });
  }

  onSubmit(): void {
    this.submissionError.set(null);

    if (this.submissionForm.invalid) {
      this.submissionForm.markAllAsTouched();
      this.scrollToFirstError();
      return;
    }

    const slots = this.photoSlots();
    if (!slots.facePhoto.file || !slots.profilePhoto.file || !slots.fullBodyPhoto.file) {
      this.submissionError.set('O envio das 3 fotografias (Rosto, Perfil e Corpo Inteiro) é obrigatório.');
      return;
    }

    this.isSubmitting.set(true);

    const formVal = this.submissionForm.value;
    const model: CandidateSubmissionModel = {
      fullName: formVal.fullName.trim(),
      email: formVal.email.trim(),
      phone: formVal.phone.trim(),
      birthDate: formVal.birthDate,
      gender: formVal.gender,
      city: formVal.city.trim(),
      state: formVal.state.trim().toUpperCase(),
      height: Number(formVal.height),
      bust: formVal.bust ? Number(formVal.bust) : null,
      waist: formVal.waist ? Number(formVal.waist) : null,
      hips: formVal.hips ? Number(formVal.hips) : null,
      shoeSize: formVal.shoeSize ? Number(formVal.shoeSize) : null,
      eyeColor: formVal.eyeColor ? formVal.eyeColor.trim() : null,
      hairColor: formVal.hairColor ? formVal.hairColor.trim() : null,
      instagramHandle: formVal.instagramHandle ? formVal.instagramHandle.trim() : null,
      guardianName: this.isUnderage() ? formVal.guardianName?.trim() : null,
      guardianPhone: this.isUnderage() ? formVal.guardianPhone?.trim() : null,
      guardianEmail: this.isUnderage() ? formVal.guardianEmail?.trim() : null,
      lgpdConsent: Boolean(formVal.lgpdConsent)
    };

    const photos = {
      facePhoto: slots.facePhoto.file,
      profilePhoto: slots.profilePhoto.file,
      fullBodyPhoto: slots.fullBodyPhoto.file
    };

    this.submissionService.submit(model, photos).subscribe({
      next: (response) => {
        this.isSubmitting.set(false);
        this.submissionSuccess.set(response);
        window.scrollTo({ top: 0, behavior: 'smooth' });
      },
      error: (err) => {
        this.isSubmitting.set(false);
        console.error('Falha na submissão da candidatura:', err);

        let errorMessage = 'Não foi possível enviar sua candidatura no momento. Por favor, tente novamente.';
        if (err.error?.detail) {
          errorMessage = err.error.detail;
        } else if (err.error?.errors) {
          const firstField = Object.keys(err.error.errors)[0];
          errorMessage = `${firstField}: ${err.error.errors[firstField]}`;
        }
        this.submissionError.set(errorMessage);
      }
    });
  }

  resetForm(): void {
    this.submissionForm.reset({
      gender: 'FEMALE',
      lgpdConsent: false
    });
    this.removePhoto('facePhoto');
    this.removePhoto('profilePhoto');
    this.removePhoto('fullBodyPhoto');
    this.submissionSuccess.set(null);
    this.submissionError.set(null);
    this.isUnderage.set(false);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  private scrollToFirstError(): void {
    setTimeout(() => {
      const firstInvalid = document.querySelector('.ng-invalid[formControlName]');
      if (firstInvalid) {
        firstInvalid.scrollIntoView({ behavior: 'smooth', block: 'center' });
      }
    }, 100);
  }
}

import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { cpfValidator } from '../../../core/validators/cpf.validator';
import { environment } from '../../../../environments/environment';

export interface PhotoSlot {
  file: File | null;
  previewUrl: string | null;
  label: string;
  required: boolean;
}

@Component({
  selector: 'app-become-model',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './become-model.component.html',
  styleUrls: ['./become-model.component.scss']
})
export class BecomeModelComponent implements OnInit {
  private fb = inject(FormBuilder);
  private http = inject(HttpClient);

  form!: FormGroup;
  isSubmitting = false;
  isMinor = false;
  calculatedAge: number | null = null;
  submitSuccess = false;
  submitError: string | null = null;

  // Grade dinâmica de fotos: 3 obrigatórias e 5 complementares (total de até 8)
  photoSlots: PhotoSlot[] = [
    { file: null, previewUrl: null, label: 'Rosto Frontal (Natural)', required: true },
    { file: null, previewUrl: null, label: 'Perfil 3/4', required: true },
    { file: null, previewUrl: null, label: 'Corpo Inteiro', required: true },
    { file: null, previewUrl: null, label: 'Luz Natural / Meio Corpo', required: false },
    { file: null, previewUrl: null, label: 'Perfil Oposto', required: false },
    { file: null, previewUrl: null, label: 'Expressão / Sorriso', required: false },
    { file: null, previewUrl: null, label: 'Foto Editorial Livre', required: false },
    { file: null, previewUrl: null, label: 'Composite ou Ensaio Anterior', required: false }
  ];

  ngOnInit(): void {
    this.initForm();
    this.watchBirthDate();
  }

  private initForm(): void {
    this.form = this.fb.group({
      fullName: ['', [Validators.required, Validators.minLength(3)]],
      email: ['', [Validators.required, Validators.email]],
      phone: ['', [Validators.required]],
      birthDate: ['', [Validators.required]],
      city: ['', [Validators.required]],
      state: ['', [Validators.required]],
      // Biometria
      height: ['', [Validators.required, Validators.min(140), Validators.max(220)]],
      bust: ['', [Validators.required]],
      waist: ['', [Validators.required]],
      hips: ['', [Validators.required]],
      shoes: ['', [Validators.required]],
      eyes: ['', [Validators.required]],
      hair: ['', [Validators.required]],
      // Responsável Legal (Condicional)
      guardianName: [''],
      guardianCpf: [''],
      guardianPhone: [''],
      guardianEmail: [''],
      // Termos
      lgpdConsent: [false, [Validators.requiredTrue]]
    });
  }

  private watchBirthDate(): void {
    this.form.get('birthDate')?.valueChanges.subscribe((birthDateStr: string) => {
      if (!birthDateStr) {
        this.calculatedAge = null;
        this.isMinor = false;
        this.updateGuardianValidation(false);
        return;
      }

      const birth = new Date(birthDateStr);
      const today = new Date();
      let age = today.getFullYear() - birth.getFullYear();
      const monthDiff = today.getMonth() - birth.getMonth();
      if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birth.getDate())) {
        age--;
      }

      this.calculatedAge = age;
      this.isMinor = age < 18;
      this.updateGuardianValidation(this.isMinor);
    });
  }

  private updateGuardianValidation(isMinor: boolean): void {
    const guardianCpf = this.form.get('guardianCpf');
    const guardianName = this.form.get('guardianName');
    const guardianPhone = this.form.get('guardianPhone');
    const guardianEmail = this.form.get('guardianEmail');

    if (isMinor) {
      guardianCpf?.setValidators([Validators.required, cpfValidator()]);
      guardianName?.setValidators([Validators.required]);
      guardianPhone?.setValidators([Validators.required]);
      guardianEmail?.setValidators([Validators.required, Validators.email]);
    } else {
      guardianCpf?.clearValidators();
      guardianName?.clearValidators();
      guardianPhone?.clearValidators();
      guardianEmail?.clearValidators();
      guardianCpf?.setValue('');
      guardianName?.setValue('');
      guardianPhone?.setValue('');
      guardianEmail?.setValue('');
    }

    guardianCpf?.updateValueAndValidity();
    guardianName?.updateValueAndValidity();
    guardianPhone?.updateValueAndValidity();
    guardianEmail?.updateValueAndValidity();
  }

  onFileSelected(index: number, event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files[0]) {
      const file = input.files[0];
      if (file.size > 5 * 1024 * 1024) {
        alert('A foto excede o limite máximo permitido de 5MB.');
        return;
      }

      const validMimes = ['image/jpeg', 'image/png', 'image/webp'];
      if (!validMimes.includes(file.type.toLowerCase())) {
        alert('Formato de foto inválido. Utilize JPG, PNG ou WEBP.');
        return;
      }

      const reader = new FileReader();
      reader.onload = (e) => {
        this.photoSlots[index].file = file;
        this.photoSlots[index].previewUrl = e.target?.result as string;
      };
      reader.readAsDataURL(file);
    }
  }

  removePhoto(index: number): void {
    this.photoSlots[index].file = null;
    this.photoSlots[index].previewUrl = null;
  }

  get hasRequiredPhotos(): boolean {
    // Exige obrigatoriamente que as 3 primeiras fotos estejam preenchidas
    return this.photoSlots.slice(0, 3).every(slot => slot.file !== null);
  }

  get totalUploadedPhotos(): number {
    return this.photoSlots.filter(s => s.file !== null).length;
  }

  onSubmit(): void {
    this.submitError = null;

    if (this.form.invalid || !this.hasRequiredPhotos) {
      this.form.markAllAsTouched();
      return;
    }

    this.isSubmitting = true;
    const formData = new FormData();

    // Payload de dados cadastrais
    const candidateData = {
      ...this.form.value,
      age: this.calculatedAge,
      isMinor: this.isMinor
    };
    formData.append('data', new Blob([JSON.stringify(candidateData)], { type: 'application/json' }));

    // Anexo de compatibilidade com controllers específicos
    if (this.photoSlots[0]?.file) formData.append('facePhoto', this.photoSlots[0].file);
    if (this.photoSlots[1]?.file) formData.append('profilePhoto', this.photoSlots[1].file);
    if (this.photoSlots[2]?.file) formData.append('fullBodyPhoto', this.photoSlots[2].file);

    // Anexo de todas as fotos presentes (de 3 a 8 arquivos)
    this.photoSlots.forEach((slot) => {
      if (slot.file) {
        formData.append('photos', slot.file);
      }
    });

    this.http.post(`${environment.apiUrl}/submissions`, formData).subscribe({
      next: () => {
        this.isSubmitting = false;
        this.submitSuccess = true;
        this.form.reset();
        this.photoSlots.forEach(s => { s.file = null; s.previewUrl = null; });
      },
      error: (err) => {
        this.isSubmitting = false;
        this.submitError = err?.error?.detail || err?.error?.message || 'Falha ao submeter candidatura. Por favor, revise seus dados e tente novamente.';
      }
    });
  }
}

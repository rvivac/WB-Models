import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { RouterModule } from '@angular/router';
import { cpfValidator } from '../../../core/validators/cpf.validator';
import { environment } from '../../../../environments/environment';

import { ApplyFaqService } from '../../../core/services/apply-faq.service';
import { ApplyFaq } from '../../../shared/models/apply-faq.interface';

export interface PhotoSlot {
  file: File | null;
  previewUrl: string | null;
  label: string;
  hint: string;
  required: boolean;
}

import { TranslatePipe } from '../../../shared/pipes/translate.pipe';

@Component({
  selector: 'app-become-model',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule, TranslatePipe],
  templateUrl: './become-model.component.html',
  styleUrls: ['./become-model.component.scss']
})
export class BecomeModelComponent implements OnInit {
  private fb = inject(FormBuilder);
  private http = inject(HttpClient);
  private faqService = inject(ApplyFaqService);

  form!: FormGroup;
  isSubmitting = false;
  isMinor = false;
  calculatedAge: number | null = null;
  submitSuccess = false;
  submitError: string | null = null;
  submissionProtocol: string | null = null;

  // Cabeçalho Editorial Dinâmico
  headerTitle = 'QUERO SER MODELO';
  headerSubtitle = 'WB SCOUTING DESK';
  headerDescription = 'Se você deseja fazer parte do casting da WB Agency, atenção para as informações abaixo: preencha o formulário e envie suas fotos para realizarmos a avaliação digital.';

  // FAQs e Orientações
  faqs: ApplyFaq[] = [];
  openFaqId: string | null = null;

  toggleFaq(id: string): void {
    this.openFaqId = this.openFaqId === id ? null : id;
  }

  isFaqOpen(id: string): boolean {
    return this.openFaqId === id;
  }

  get applyForm(): FormGroup {
    return this.form;
  }

  // Estados Brasileiros para referência e seleção
  readonly brazilianStates = [
    { uf: 'AC', name: 'Acre' },
    { uf: 'AL', name: 'Alagoas' },
    { uf: 'AP', name: 'Amapá' },
    { uf: 'AM', name: 'Amazonas' },
    { uf: 'BA', name: 'Bahia' },
    { uf: 'CE', name: 'Ceará' },
    { uf: 'DF', name: 'Distrito Federal' },
    { uf: 'ES', name: 'Espírito Santo' },
    { uf: 'GO', name: 'Goiás' },
    { uf: 'MA', name: 'Maranhão' },
    { uf: 'MT', name: 'Mato Grosso' },
    { uf: 'MS', name: 'Mato Grosso do Sul' },
    { uf: 'MG', name: 'Minas Gerais' },
    { uf: 'PA', name: 'Pará' },
    { uf: 'PB', name: 'Paraíba' },
    { uf: 'PR', name: 'Paraná' },
    { uf: 'PE', name: 'Pernambuco' },
    { uf: 'PI', name: 'Piauí' },
    { uf: 'RJ', name: 'Rio de Janeiro' },
    { uf: 'RN', name: 'Rio Grande do Norte' },
    { uf: 'RS', name: 'Rio Grande do Sul' },
    { uf: 'RO', name: 'Rondônia' },
    { uf: 'RR', name: 'Roraima' },
    { uf: 'SC', name: 'Santa Catarina' },
    { uf: 'SP', name: 'São Paulo' },
    { uf: 'SE', name: 'Sergipe' },
    { uf: 'TO', name: 'Tocantins' }
  ];

  // Cores curadas para olhos e cabelos
  readonly eyeColors = ['CASTANHO', 'VERDE', 'AZUL', 'PRETO', 'MEL'];
  readonly hairColors = ['CASTANHO', 'LOIRO', 'PRETO', 'RUIVO', 'GRISALHO'];

  // Grade editorial de fotos: 3 obrigatórias e 5 complementares (total de 8)
  photoSlots: PhotoSlot[] = [
    { file: null, previewUrl: null, label: 'Rosto Frontal (Close)', hint: 'Sem maquiagem, expressão neutra', required: true },
    { file: null, previewUrl: null, label: 'Perfil 3/4', hint: 'Ângulo de 45°, cabelo atrás da orelha', required: true },
    { file: null, previewUrl: null, label: 'Corpo Inteiro', hint: 'Roupa básica e postura natural', required: true },
    { file: null, previewUrl: null, label: 'Luz Natural / Meio Corpo', hint: 'Iluminação difusa de dia', required: false },
    { file: null, previewUrl: null, label: 'Perfil Oposto', hint: 'Visão lateral completa', required: false },
    { file: null, previewUrl: null, label: 'Expressão / Sorriso', hint: 'Mostre sua espontaneidade', required: false },
    { file: null, previewUrl: null, label: 'Foto Editorial Livre', hint: 'Ensaio fotográfico ou teste', required: false },
    { file: null, previewUrl: null, label: 'Composite ou Ensaio Anterior', hint: 'Material profissional se houver', required: false }
  ];

  ngOnInit(): void {
    this.initForm();
    this.watchBirthDate();
    this.loadEditorialContent();
  }

  private loadEditorialContent(): void {
    this.faqService.getPublicApplyHeader().subscribe(header => {
      if (header) {
        this.headerTitle = header.title || this.headerTitle;
        this.headerSubtitle = header.subtitle || this.headerSubtitle;
        this.headerDescription = header.description || this.headerDescription;
      }
    });

    this.faqService.getPublicFaqs().subscribe(faqs => {
      if (faqs && faqs.length > 0) {
        this.faqs = faqs;
      }
    });
  }

  private initForm(): void {
    this.form = this.fb.group({
      fullName: ['', [Validators.required, Validators.minLength(3)]],
      gender: ['FEMALE', [Validators.required]],
      email: ['', [Validators.required, Validators.email]],
      phone: ['', [Validators.required]],
      instagram: [''],
      instagramHandle: [''],
      birthDate: ['', [Validators.required]],
      city: ['', [Validators.required]],
      state: ['', [Validators.required]],
      // Biometria
      height: ['', [Validators.required, Validators.min(100), Validators.max(230)]],
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

  setGender(gender: string): void {
    this.form.get('gender')?.setValue(gender);
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

  onPhoneInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    let v = input.value.replace(/\D/g, '');
    if (v.length > 11) v = v.substring(0, 11);
    
    if (v.length > 10) {
      input.value = `(${v.substring(0, 2)}) ${v.substring(2, 7)}-${v.substring(7)}`;
    } else if (v.length > 6) {
      input.value = `(${v.substring(0, 2)}) ${v.substring(2, 6)}-${v.substring(6)}`;
    } else if (v.length > 2) {
      input.value = `(${v.substring(0, 2)}) ${v.substring(2)}`;
    } else if (v.length > 0) {
      input.value = `(${v}`;
    }
    this.form.get('phone')?.setValue(input.value, { emitEvent: false });
  }

  onGuardianPhoneInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    let v = input.value.replace(/\D/g, '');
    if (v.length > 11) v = v.substring(0, 11);
    
    if (v.length > 10) {
      input.value = `(${v.substring(0, 2)}) ${v.substring(2, 7)}-${v.substring(7)}`;
    } else if (v.length > 6) {
      input.value = `(${v.substring(0, 2)}) ${v.substring(2, 6)}-${v.substring(6)}`;
    } else if (v.length > 2) {
      input.value = `(${v.substring(0, 2)}) ${v.substring(2)}`;
    } else if (v.length > 0) {
      input.value = `(${v}`;
    }
    this.form.get('guardianPhone')?.setValue(input.value, { emitEvent: false });
  }

  onCpfInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    let v = input.value.replace(/\D/g, '');
    if (v.length > 11) v = v.substring(0, 11);
    
    if (v.length > 9) {
      input.value = `${v.substring(0, 3)}.${v.substring(3, 6)}.${v.substring(6, 9)}-${v.substring(9)}`;
    } else if (v.length > 6) {
      input.value = `${v.substring(0, 3)}.${v.substring(3, 6)}.${v.substring(6)}`;
    } else if (v.length > 3) {
      input.value = `${v.substring(0, 3)}.${v.substring(3)}`;
    }
    this.form.get('guardianCpf')?.setValue(input.value, { emitEvent: false });
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

  removePhoto(index: number, event?: Event): void {
    if (event) event.stopPropagation();
    this.photoSlots[index].file = null;
    this.photoSlots[index].previewUrl = null;
  }

  get hasRequiredPhotos(): boolean {
    return this.photoSlots.slice(0, 3).every(slot => slot.file !== null);
  }

  get totalUploadedPhotos(): number {
    return this.photoSlots.filter(s => s.file !== null).length;
  }

  get progressPercentage(): number {
    const totalFields = 10;
    let completed = 0;
    const f = this.form.value;
    if (f.fullName?.length >= 3) completed++;
    if (f.email) completed++;
    if (f.phone) completed++;
    if (f.birthDate) completed++;
    if (f.city && f.state) completed++;
    if (f.height) completed++;
    if (f.bust && f.waist && f.hips) completed++;
    if (this.hasRequiredPhotos) completed += 2;
    if (f.lgpdConsent) completed++;
    return Math.min(100, Math.round((completed / totalFields) * 100));
  }

  onSubmit(): void {
    this.submitError = null;

    if (this.form.invalid || !this.hasRequiredPhotos) {
      this.form.markAllAsTouched();
      return;
    }

    this.isSubmitting = true;
    const formData = new FormData();

    const formVal = this.form.value;
    const heightVal = Number(formVal.height);
    // Backend espera altura em metros (1.20 a 2.30)
    const heightInMeters = heightVal > 3 ? +(heightVal / 100).toFixed(2) : +heightVal.toFixed(2);
    const shoeSizeVal = parseInt(String(formVal.shoes).replace(/\D/g, ''), 10) || 38;

    let instagram = (formVal.instagram || formVal.instagramHandle || '').trim();
    if (instagram && !instagram.startsWith('@') && !instagram.startsWith('http')) {
      instagram = '@' + instagram;
    }

    const stateVal = (formVal.state || '').trim().toUpperCase();

    // Payload compatível com ambos os DTOs
    const candidateData = {
      ...formVal,
      gender: formVal.gender || 'FEMALE',
      state: stateVal,
      height: heightInMeters,
      shoeSize: shoeSizeVal,
      eyeColor: formVal.eyes,
      hairColor: formVal.hair,
      instagramHandle: instagram,
      instagram: instagram,
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

    this.http.post<any>(`${environment.apiUrl}/submissions`, formData).subscribe({
      next: (res) => {
        this.isSubmitting = false;
        this.submitSuccess = true;
        this.submissionProtocol = res?.protocol || (`WB-${new Date().getFullYear()}-${Math.floor(100000 + Math.random() * 900000)}`);
        this.form.reset();
        this.photoSlots.forEach(s => { s.file = null; s.previewUrl = null; });
        window.scrollTo({ top: 0, behavior: 'smooth' });
      },
      error: (err) => {
        this.isSubmitting = false;
        this.submitError = err?.error?.detail || err?.error?.message || 'Falha ao submeter candidatura. Por favor, revise seus dados e tente novamente.';
      }
    });
  }

  resetForm(): void {
    this.submitSuccess = false;
    this.submitError = null;
    this.submissionProtocol = null;
    this.form.reset({
      gender: 'FEMALE',
      eyes: '',
      hair: '',
      lgpdConsent: false
    });
    this.photoSlots.forEach(s => { s.file = null; s.previewUrl = null; });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }
}

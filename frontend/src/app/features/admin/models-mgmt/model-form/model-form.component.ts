import {
  Component,
  OnInit,
  Input,
  Output,
  EventEmitter,
  inject,
  signal,
  computed
} from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { HttpEventType } from '@angular/common/http';
import { AdminModelService } from '../../../../core/services/admin-model.service';
import {
  ModelAdminItem,
  ModelFormData,
  ModelGender
} from '../../../../shared/models/admin-model.interface';
import { PhotoUploaderGridComponent } from '../components/photo-uploader-grid/photo-uploader-grid.component';
import { ModelCompositeManagerComponent } from '../components/model-composite-manager/model-composite-manager.component';
import { GalleryPhoto } from '../../../../shared/models/gallery.model';

@Component({
  selector: 'app-model-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule, PhotoUploaderGridComponent, ModelCompositeManagerComponent],
  templateUrl: './model-form.component.html',
  styleUrls: ['./model-form.component.scss']
})
export class ModelFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly adminModelService = inject(AdminModelService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  @Input() set modelIdInput(val: string | null | undefined) {
    if (val) {
      this.modelId.set(val);
      this.loadModelData(val);
    }
  }

  @Output() saved = new EventEmitter<ModelAdminItem>();
  @Output() cancelled = new EventEmitter<void>();

  readonly modelId = signal<string | null>(null);
  readonly isEditMode = computed(() => !!this.modelId());
  readonly isLoading = signal<boolean>(false);
  readonly isSaving = signal<boolean>(false);
  readonly toast = signal<{ message: string; type: 'success' | 'error' } | null>(null);

  // Predefinições de fotos editoriais para facilidade de demonstração
  readonly samplePhotos: string[] = [
    'https://images.unsplash.com/photo-1534528741775-53994a69daeb?q=80&w=800&auto=format&fit=crop',
    'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?q=80&w=800&auto=format&fit=crop',
    'https://images.unsplash.com/photo-1517841905240-472988babdf9?q=80&w=800&auto=format&fit=crop',
    'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?q=80&w=800&auto=format&fit=crop',
    'https://images.unsplash.com/photo-1524504388940-b1c1722653e1?q=80&w=800&auto=format&fit=crop',
    'https://images.unsplash.com/photo-1494790108377-be9c29b29330?q=80&w=800&auto=format&fit=crop'
  ];

  readonly modelForm: FormGroup = this.fb.group({
    stageName: ['', [Validators.required, Validators.maxLength(150)]],
    gender: ['FEMALE' as ModelGender, [Validators.required]],
    isStar: [false],
    isFeaturedHome: [false],
    featuredOrder: [null, [Validators.min(1), Validators.max(99)]],
    isActive: [true],
    primaryPhotoUrl: [''],
    instagramUrl: ['', [Validators.maxLength(255)]],
    birthDate: [''],
    heightCm: [null, [Validators.min(50), Validators.max(250)]],
    city: ['', [Validators.maxLength(100)]],
    nationality: ['Brasileira', [Validators.maxLength(100)]],
    dressSize: ['', [Validators.maxLength(20)]],
    shoeSize: ['', [Validators.maxLength(20)]],
    bustChestCm: [null, [Validators.min(20), Validators.max(200)]],
    waistCm: [null, [Validators.min(20), Validators.max(200)]],
    hipsCm: [null, [Validators.min(20), Validators.max(200)]],
    hairColor: ['', [Validators.maxLength(50)]],
    eyesColor: ['', [Validators.maxLength(50)]]
  });

  // Idade calculada dinamicamente
  readonly computedAge = signal<number | null>(null);

  // Sinal reativo aos valores do formulário
  readonly formValueSignal = signal(this.modelForm.value);

  // Resumo das medidas em tempo real para o card preview
  readonly computedMeasurements = computed(() => {
    const val = this.formValueSignal();
    const h = val?.heightCm;
    const b = val?.bustChestCm;
    const w = val?.waistCm;
    const hip = val?.hipsCm;
    const shoe = val?.shoeSize;
    const dress = val?.dressSize;

    const parts: string[] = [];
    if (h) parts.push(`${h} cm`);
    if (b || w || hip) parts.push(`${b || '--'} / ${w || '--'} / ${hip || '--'}`);
    if (dress) parts.push(`Man. ${dress}`);
    if (shoe) parts.push(`Sap. ${shoe}`);

    return parts.length > 0 ? parts.join(' • ') : 'Medidas não informadas';
  });

  ngOnInit(): void {
    // Sincroniza sinal reativo e cálculo de idade a cada mudança no formulário
    this.modelForm.valueChanges.subscribe((val) => {
      this.formValueSignal.set(val);
      this.updateComputedAge(val.birthDate);
    });

    // Se estiver em rota direta (ex: /admin/models/:id/edit ou /admin/models/new)
    const routeId = this.route.snapshot.paramMap.get('id');
    if (routeId && routeId !== 'new') {
      this.modelId.set(routeId);
      this.loadModelData(routeId);
    }
  }

  // Galeria de Fotos Editorial (ADM-006 / EAP 4.2.3)
  readonly galleryPhotos = signal<GalleryPhoto[]>([]);

  onPhotosChanged(updatedPhotos: GalleryPhoto[]): void {
    this.galleryPhotos.set(updatedPhotos);
    if (updatedPhotos.length > 0) {
      const coverPhoto = updatedPhotos.find((p) => p.isCover) || updatedPhotos[0];
      this.modelForm.patchValue({ primaryPhotoUrl: coverPhoto.url });
    }
  }

  onFilesUploaded(files: File[]): void {
    this.showToast(`${files.length} arquivo(s) selecionado(s). Fazendo upload via Backend para o Supabase...`, 'success');
    const current = this.modelId();
    if (!current) {
      this.showToast('Primeiro salve os DADOS BÁSICOS do modelo (modelo novo ainda sem ID no banco). Depois envie as fotos.', 'error');
      return;
    }

    files.forEach((file, idx) => {
      // Marca previa temporaria como isUploading=true (feedback visual)
      const previews = this.galleryPhotos();
      const tempPhoto = previews.find(p => p?.id && p.id.startsWith('temp-') && !(p as any).__uploadStarted);
      if (tempPhoto) {
        tempPhoto.isUploading = true;
        tempPhoto.uploadProgress = 1;
        (tempPhoto as any).__uploadStarted = true;
      }

      this.adminModelService.uploadModelMedia(current, file, 'BOOK', false).subscribe({
        next: (ev) => {
          if (ev.type === HttpEventType.UploadProgress && ev.total && tempPhoto) {
            tempPhoto.uploadProgress = Math.round((100 * ev.loaded) / ev.total);
          } else if (ev.type === HttpEventType.Response && ev.body) {
            const saved = ev.body;
            // Remove a previa temporaria temp-
            let galeriaNova = this.galleryPhotos().filter(p => !(p?.id && p.id.startsWith('temp-')));
            // Insere foto REAL do banco
            const fotoReal: GalleryPhoto = {
              id: saved.id,
              url: saved.fileUrl + (saved.fileUrl.includes('?v=') ? '' : ('?v=' + Math.floor(Date.now() / 3600_000))),
              filePath: saved.filePath,
              category: saved.mediaType as any,
              orderIndex: galeriaNova.length,
              isCover: saved.isCover || galeriaNova.length === 0,
              isActive: true,
              isUploaded: true,
              isUploading: false,
              uploadProgress: 100
            };
            galeriaNova = [...galeriaNova, fotoReal];
            // Se for a primeira foto, torna ela capa automaticamente
            if (galeriaNova.length === 1) {
              galeriaNova[0].isCover = true;
            } else if (!galeriaNova.some(p => p.isCover)) {
              galeriaNova[0].isCover = true;
            }
            galeriaNova.forEach((p, i) => { p.orderIndex = i; });

            this.galleryPhotos.set(galeriaNova);
            const cover = galeriaNova.find(p => p.isCover) || galeriaNova[0];
            if (cover) this.modelForm.patchValue({ primaryPhotoUrl: cover.url });

            this.showToast(`✅ Foto ${idx + 1}/${files.length} enviada com sucesso (já está salva no banco)`, 'success');
          }
        },
        error: (err) => {
          if (tempPhoto) { tempPhoto.isUploading = false; tempPhoto.uploadProgress = 0; }
          const msg = err?.error?.detail || err?.error?.message || `Falha ao enviar foto ${idx + 1}. Verifique tamanho e tipo.`;
          this.showToast(msg, 'error');
          console.error('Erro upload foto idx=' + idx, err);
        }
      });
    });
  }

  loadModelData(id: string): void {
    this.isLoading.set(true);
    this.adminModelService.getModelById(id).subscribe({
      next: (model) => {
        this.modelForm.patchValue({
          stageName: model.stageName,
          gender: model.gender,
          isStar: model.isStar,
          isFeaturedHome: model.isFeaturedHome,
          featuredOrder: model.featuredOrder,
          isActive: model.isActive,
          primaryPhotoUrl: model.primaryPhotoUrl || '',
          instagramUrl: model.instagramUrl || '',
          birthDate: model.birthDate || '',
          heightCm: model.heightCm,
          city: model.city || '',
          nationality: model.nationality || 'Brasileira',
          dressSize: model.dressSize || '',
          shoeSize: model.shoeSize || '',
          bustChestCm: model.bustChestCm,
          waistCm: model.waistCm,
          hipsCm: model.hipsCm,
          hairColor: model.hairColor || '',
          eyesColor: model.eyesColor || ''
        });

        // Inicializa acervo visual de imagens do modelo
        const initialGallery: GalleryPhoto[] = [];
        if (model.primaryPhotoUrl) {
          initialGallery.push({
            id: 'photo-cover',
            url: model.primaryPhotoUrl,
            category: 'BOOK',
            orderIndex: 0,
            isCover: true
          });
        }
        if (model.gender === 'FEMALE') {
          initialGallery.push(
            { id: 'p2', url: this.samplePhotos[2], category: 'POLAROID', orderIndex: 1, isCover: false },
            { id: 'p3', url: this.samplePhotos[4], category: 'BOOK', orderIndex: 2, isCover: false }
          );
        } else {
          initialGallery.push(
            { id: 'p2', url: this.samplePhotos[3], category: 'POLAROID', orderIndex: 1, isCover: false }
          );
        }
        this.galleryPhotos.set(initialGallery);

        this.updateComputedAge(model.birthDate);
        this.isLoading.set(false);
      },
      error: () => {
        this.showToast('Erro ao carregar dados do modelo.', 'error');
        this.isLoading.set(false);
      }
    });
  }

  setGender(gender: ModelGender): void {
    this.modelForm.patchValue({ gender });
  }

  selectSamplePhoto(url: string): void {
    this.modelForm.patchValue({ primaryPhotoUrl: url });
    const current = this.galleryPhotos();
    if (current.length === 0) {
      this.galleryPhotos.set([
        { id: 'cover-sample', url, category: 'BOOK', orderIndex: 0, isCover: true }
      ]);
    } else {
      current[0].url = url;
      this.galleryPhotos.set([...current]);
    }
  }

  updateComputedAge(dateString?: string | null): void {
    if (!dateString) {
      this.computedAge.set(null);
      return;
    }
    const birth = new Date(dateString);
    if (isNaN(birth.getTime())) {
      this.computedAge.set(null);
      return;
    }
    const today = new Date();
    let age = today.getFullYear() - birth.getFullYear();
    const m = today.getMonth() - birth.getMonth();
    if (m < 0 || (m === 0 && today.getDate() < birth.getDate())) {
      age--;
    }
    this.computedAge.set(age >= 0 ? age : null);
  }

  onSubmit(): void {
    if (this.modelForm.invalid) {
      this.modelForm.markAllAsTouched();
      this.showToast('Por favor, verifique os campos obrigatórios em destaque.', 'error');
      return;
    }

    this.isSaving.set(true);
    const formVal = this.modelForm.value;

    const payload: ModelFormData = {
      stageName: formVal.stageName.trim(),
      gender: formVal.gender,
      isStar: Boolean(formVal.isStar),
      isFeaturedHome: Boolean(formVal.isFeaturedHome),
      featuredOrder: formVal.isFeaturedHome && formVal.featuredOrder ? Number(formVal.featuredOrder) : null,
      isActive: Boolean(formVal.isActive),
      primaryPhotoUrl: formVal.primaryPhotoUrl ? formVal.primaryPhotoUrl.trim() : null,
      instagramUrl: formVal.instagramUrl ? formVal.instagramUrl.trim() : null,
      birthDate: formVal.birthDate || null,
      heightCm: formVal.heightCm ? Number(formVal.heightCm) : null,
      city: formVal.city ? formVal.city.trim() : null,
      nationality: formVal.nationality ? formVal.nationality.trim() : null,
      dressSize: formVal.dressSize ? formVal.dressSize.trim() : null,
      shoeSize: formVal.shoeSize ? formVal.shoeSize.trim() : null,
      bustChestCm: formVal.bustChestCm ? Number(formVal.bustChestCm) : null,
      waistCm: formVal.waistCm ? Number(formVal.waistCm) : null,
      hipsCm: formVal.hipsCm ? Number(formVal.hipsCm) : null,
      hairColor: formVal.hairColor ? formVal.hairColor.trim() : null,
      eyesColor: formVal.eyesColor ? formVal.eyesColor.trim() : null
    };

    const currentId = this.modelId();

    if (currentId) {
      this.adminModelService.updateModel(currentId, payload).subscribe({
        next: (savedModel) => {
          this.isSaving.set(false);
          this.showToast('Modelo atualizado com sucesso!', 'success');
          this.saved.emit(savedModel);
          setTimeout(() => {
            if (this.route.snapshot.paramMap.get('id')) {
              this.router.navigate(['/admin/models']);
            }
          }, 600);
        },
        error: (err) => {
          this.isSaving.set(false);
          const msg = err?.error?.detail || err?.error?.message || 'Falha ao salvar modelo.';
          this.showToast(msg, 'error');
        }
      });
    } else {
      this.adminModelService.createModel(payload).subscribe({
        next: (createdModel) => {
          this.isSaving.set(false);
          this.showToast('Modelo cadastrado com sucesso!', 'success');
          this.saved.emit(createdModel);
          setTimeout(() => {
            if (this.route.snapshot.paramMap.get('id')) {
              this.router.navigate(['/admin/models']);
            }
          }, 600);
        },
        error: (err) => {
          this.isSaving.set(false);
          const msg = err?.error?.detail || err?.error?.message || 'Falha ao cadastrar modelo.';
          this.showToast(msg, 'error');
        }
      });
    }
  }

  onCancel(): void {
    this.cancelled.emit();
    if (this.route.snapshot.paramMap.get('id') || this.route.snapshot.url.some(s => s.path === 'new')) {
      this.router.navigate(['/admin/models']);
    }
  }

  showToast(message: string, type: 'success' | 'error'): void {
    this.toast.set({ message, type });
    setTimeout(() => {
      this.toast.set(null);
    }, 4000);
  }
}

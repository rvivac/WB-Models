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
import { Observable } from 'rxjs';
import { switchMap, map, tap, catchError } from 'rxjs/operators';
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
    if (!updatedPhotos || updatedPhotos.length === 0) {
      return;
    }
    const coverPhoto = updatedPhotos.find((p) => Boolean(p.isCover)) || updatedPhotos[0];
    if (coverPhoto?.url) {
      this.modelForm.patchValue({ primaryPhotoUrl: coverPhoto.url });
    }

    // Se a foto marcada como capa tem ID REAL (nao eh previa temp-), chamar endpoint setModelCover
    // para PERSISTIR isCover=true na tabela model_media + sincronizar primary_photo_url models.
    const idAtual = this.modelId();
    const coverComId = updatedPhotos.find((p) => Boolean(p.isCover) && Boolean(p.id) && !String(p.id).startsWith('temp-'));
    if (idAtual && coverComId && coverComId.id) {
      this.adminModelService.setModelCover(idAtual, coverComId.id).subscribe({
        next: (saved) => {
          console.info('[onPhotosChanged] Capa persistida no banco. mediaId=' + saved.id);
          // Apos sucesso, recarrega galeria e sincroniza primaryPhotoUrl com valor salvo
          this._refreshGalleryFromDatabase(idAtual).subscribe(() => {
            const galAtual = this.galleryPhotos();
            const capaAtualizada = galAtual.find(x => Boolean(x.isCover)) || galAtual[0];
            if (capaAtualizada?.url) {
              this.modelForm.patchValue({ primaryPhotoUrl: capaAtualizada.url });
            }
          });
        },
        error: (err) => {
          const msg = err?.error?.detail || err?.error?.message || 'Falha ao salvar nova capa no banco.';
          console.warn('[onPhotosChanged] setModelCover falhou:', err);
          this.showToast(msg, 'error');
        }
      });
    }
  }

  /**
   * Busca a GALERIA REAL do modelo no Backend (tabela model_media),
   * substitui placeholders hardcoded (samplePhotos) que faziam as 3 fotos
   * "erradas" aparecerem no Admin enquanto no Publico apareciam 41 corretas.
   *
   * Usado SEMPRE:
   *  - no loadModelData (1a vez abrindo a tela)
   *  - depois de upload, exclusao, reorder, marcacao de capa.
   * Garante que Admin tenha a MESMA lista que Publico (1:1).
   */
  _refreshGalleryFromDatabase(forModelId: string): Observable<GalleryPhoto[]> {
    return this.adminModelService.getModelMedia(forModelId).pipe(
      tap((listaRealOrdenada) => {
        if (!listaRealOrdenada || listaRealOrdenada.length === 0) {
          // Fallback: sem nenhuma midia gravada ainda. Apenas usa a capa do formulario se existir.
          const capa = this.modelForm.get('primaryPhotoUrl')?.value;
          if (capa) {
            this.galleryPhotos.set([{
              id: 'fallback-cover',
              url: capa,
              category: 'BOOK',
              orderIndex: 0,
              isCover: true
            }]);
          } else {
            this.galleryPhotos.set([]);
          }
          return;
        }

        // Garante pelo menos 1 capa (primeira foto se nenhuma for marcada)
        let normalized = [...listaRealOrdenada].sort((a, b) => (a.orderIndex ?? 0) - (b.orderIndex ?? 0));
        if (!normalized.some(p => Boolean(p.isCover))) {
          normalized[0].isCover = true;
        }
        normalized.forEach((p, i) => { p.orderIndex = i; });

        this.galleryPhotos.set(normalized);

        // Sync final: se tiver capa real, atualiza campo primaryPhotoUrl do form
        const realCover = normalized.find(p => Boolean(p.isCover)) || normalized[0];
        if (realCover?.url) {
          this.modelForm.patchValue({ primaryPhotoUrl: realCover.url });
        }
      })
    );
  }

  loadModelData(id: string): void {
    this.isLoading.set(true);
    this.adminModelService.getModelById(id).pipe(
      // PRIMEIRO carrega dados basicos do modelo, DEPOIS busca a GALERIA REAL de midias.
      // (evita cor de condicao: galeria vinda antes do ID estar pronto)
      switchMap((model) => {
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
        this.updateComputedAge(model.birthDate);

        // 🎯 BUSCA A GALERIA REAL (41 fotos Eve). NUNCA MAIS samplePhotos hardcoded.
        return this._refreshGalleryFromDatabase(id).pipe(map(() => model));
      })
    ).subscribe({
      next: (_modeloFinal) => {
        this.isLoading.set(false);
        const total = this.galleryPhotos().length;
        if (total > 0) {
          this.showToast(`Galeria carregada: ${total} foto(s) reais do banco. Igual ao perfil público.`, 'success');
        } else {
          this.showToast('Modelo carregado. Envie as fotos do book / polaroids.', 'success');
        }
      },
      error: (err) => {
        this.isLoading.set(false);
        const msg = err?.error?.detail || err?.error?.message || 'Erro ao carregar dados do modelo.';
        this.showToast(msg, 'error');
        console.error('[loadModelData] ERRO:', err);
      }
    });
  }

  onFilesUploaded(files: File[]): void {
    if (!files || files.length === 0) return;
    this.showToast(`${files.length} arquivo(s) selecionado(s). Fazendo upload via Backend para o Supabase...`, 'success');
    const current = this.modelId();
    if (!current) {
      this.showToast('Primeiro salve os DADOS BÁSICOS do modelo (modelo novo ainda sem ID no banco). Depois envie as fotos.', 'error');
      return;
    }

    let enviadosOk = 0;
    let falhas = 0;

    const uploadEmSequencia = files.reduce((accPromessa, file, idx) => {
      // 1) Procura previa temp- correspondente MARCA como UPLOADING (IMUTAVEL)
      let previews = [...this.galleryPhotos()];
      let tempIdx = previews.findIndex(p => p?.id && String(p.id).startsWith('temp-') && !(p as any).__uploadStarted);
      const tempPhoto = tempIdx >= 0 ? { ...previews[tempIdx], _index: tempIdx } : null;
      if (tempIdx >= 0) {
        previews[tempIdx] = { ...previews[tempIdx], isUploading: true, uploadProgress: 1 };
        (previews[tempIdx] as any).__uploadStarted = true;
        this.galleryPhotos.set([...previews]);
      }

      // 2) Upload SEQUENCIAL (evita flood HTTP no bucket Supabase)
      return accPromessa.then(() => new Promise<void>((resolve) => {
        this.adminModelService.uploadModelMedia(current, file, 'BOOK', false).subscribe({
          next: (ev) => {
            if (ev.type === HttpEventType.UploadProgress && ev.total && tempPhoto) {
              const p = Math.round((100 * ev.loaded) / ev.total);
              previews = [...this.galleryPhotos()];
              if (previews[tempPhoto._index]?.id === tempPhoto.id) {
                previews[tempPhoto._index] = { ...previews[tempPhoto._index], uploadProgress: p };
                this.galleryPhotos.set([...previews]);
              }
            } else if (ev.type === HttpEventType.Response && ev.body) {
              enviadosOk++;
              const saved = ev.body;
              // Remove previa temp- (imutavel) e insere FOTO REAL do banco
              let galeriaNova = [...this.galleryPhotos()].filter(p => !(p?.id && String(p.id).startsWith('temp-')));
              const fotoReal: GalleryPhoto = {
                id: saved.id,
                url: saved.fileUrl + (saved.fileUrl.includes('?v=') ? '' : ('?v=' + Math.floor(Date.now() / 3_600_000))),
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
              if (galeriaNova.length === 1) galeriaNova[0].isCover = true;
              else if (!galeriaNova.some(p => Boolean(p.isCover))) galeriaNova[0].isCover = true;
              galeriaNova = galeriaNova.map((p, i) => ({ ...p, orderIndex: i }));

              this.galleryPhotos.set(galeriaNova);
              const cover = galeriaNova.find(p => Boolean(p.isCover)) || galeriaNova[0];
              if (cover?.url) this.modelForm.patchValue({ primaryPhotoUrl: cover.url });

              this.showToast(`✅ Foto ${idx + 1}/${files.length} enviada com sucesso (já está salva no banco)`, 'success');
              resolve();
            }
          },
          error: (err) => {
            falhas++;
            if (tempPhoto) {
              previews = [...this.galleryPhotos()];
              if (previews[tempPhoto._index]?.id === tempPhoto.id) {
                previews[tempPhoto._index] = { ...previews[tempPhoto._index], isUploading: false, uploadProgress: 0 };
                this.galleryPhotos.set([...previews]);
              }
            }
            const msg = err?.error?.detail || err?.error?.message || `Falha ao enviar foto ${idx + 1}. Verifique tamanho e tipo.`;
            this.showToast(msg, 'error');
            console.error('Erro upload foto idx=' + idx, err);
            resolve();
          }
        });
      }));
    }, Promise.resolve());

    // AO FINAL DE TODOS -> recarrega galeria REAL do banco (sincronia 100% Admin = Perfil Publico)
    uploadEmSequencia.finally(() => {
      this._refreshGalleryFromDatabase(current).subscribe(() => {
        if (falhas === 0) {
          this.showToast(`📚 Upload concluído (${enviadosOk}/${files.length}). Galeria sincronizada com o banco (idêntica ao perfil público).`, 'success');
        } else {
          this.showToast(`${enviadosOk} OK. ${falhas} falha(s). Galeria sincronizada.`, 'error');
        }
      });
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

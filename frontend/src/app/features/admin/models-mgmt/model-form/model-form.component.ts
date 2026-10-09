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
import { Observable, forkJoin, firstValueFrom, of } from 'rxjs';
import { switchMap, map, tap, catchError, finalize } from 'rxjs/operators';
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

  // ============================================================
  // 🟢 CORREÇÃO: FOTO DE CAPA APARECER NO CARD PREVIEW EDITORIAL (TOPO)
  // Se primaryPhotoUrl estiver incompleta / vazia / HTTP 400, cai
  // para a PRIMEIRA FOTO MARCADA COMO CAPA NA GALERIA REAL (ou a primeira
  // da lista, se nenhuma marcada). Nunca mais placeholder WB sozinho.
  // ============================================================
  private readonly _forcarRerenderCapa = signal<number>(0);

  readonly fotoCapaPreviewSafe = computed<string | null>(() => {
    this._forcarRerenderCapa(); // dependencia de trigger
    return this._resolveCapaParaPreview(
      this.modelForm.get('primaryPhotoUrl')?.value,
      this.galleryPhotos() || []
    );
  });

  onErroCarregarCapaPreview(): void {
    // Se navegador retornou erro ao carregar (HTTP 400 bucket, URL incompleta)
    // força nova computação e escreve fallback explícito para 1a foto da galeria
    console.warn('[Capa Preview] Falha ao carregar primaryPhotoUrl. Usando fallback da galeria REAL.');
    const galeria = this.galleryPhotos() || [];
    if (galeria.length > 0) {
      const capaGaleria = galeria.find(p => Boolean(p.isCover)) || galeria[0];
      if (capaGaleria?.url) {
        // Patch direto e força rerender
        this.modelForm.patchValue({ primaryPhotoUrl: capaGaleria.url }, { emitEvent: true });
        this._forcarRerenderCapa.update(n => n + 1);
      }
    }
  }

  /**
   * Helper monta URL FINAL segura para o avatar-preview do topo do admin.
   * 1) Se primaryPhotoUrl parecer completa, usa.
   * 2) Se incompleta ou vazia, usa 1a foto marcada isCover=true na galeria REAL.
   * 3) Senao, usa a 1a foto da galeria.
   * 4) Usa filePath para remontar se necessário (igual helpers de capa em outros services).
   */
  private _resolveCapaParaPreview(primaryRaw: any, galeria: GalleryPhoto[]): string | null {
    const SUPABASE_BASE = 'https://zmpqmdizqgnpnirqiufq.supabase.co/storage/v1/object/public/models-media';
    const bust = '?v=' + Math.floor(Date.now() / 3_600_000);

    // Helper interno valida se uma URL parece OK
    const pareceOK = (u: string) => {
      if (!u) return false;
      const s = String(u).trim();
      if (!s.startsWith('http')) return false;
      if (s.length < 40) return false;
      if (s.endsWith('/models-media/') || s.endsWith('/models-media')) return false;
      if (s.endsWith('/')) return false;
      return true;
    };
    // Helper interno: aplica fallback filePath se URL vier incompleta
    const remontarSePrecisar = (urlIn: string, fp?: string): string => {
      if (!urlIn) return '';
      let u = String(urlIn).trim();
      let caminho = fp || '';
      // Caso 1: parece completa
      if (pareceOK(u)) {
        return u + (u.includes('?v=') ? '' : bust);
      }
      // Caso 2: URL vazia, mas temos filePath
      if ((!u || u.length < 10) && caminho) {
        const clean = caminho.replace(/^\//, '');
        return `${SUPABASE_BASE}/${clean}${bust}`;
      }
      // Caso 3: URL veio tipo /public/models-media/ sem path
      if (u.includes('/models-media') && caminho) {
        const clean = caminho.replace(/^\//, '');
        if (!u.includes(clean)) {
          let base = u.substring(0, u.indexOf('/models-media') + '/models-media'.length);
          base = base.startsWith('http') ? base : SUPABASE_BASE;
          return `${base}/${clean}${bust}`.replace(/([^:]\/)\/+/g, '$1');
        }
      }
      // Retorna o que der ou null
      if (u && pareceOK(u + '/fix')) return u + bust;
      return u ? (u + bust) : '';
    };

    // 1) Tenta primaryPhotoUrl primeiro
    if (pareceOK(String(primaryRaw || '').trim())) {
      return String(primaryRaw).trim() + (String(primaryRaw).includes('?v=') ? '' : bust);
    }
    // 2) Se chegou aqui, a primary nao serve. Usa galeria REAL.
    if (galeria && galeria.length > 0) {
      const capa = galeria.find(p => Boolean(p.isCover)) || galeria[0];
      if (capa) {
        if (pareceOK(String(capa.url || ''))) {
          return String(capa.url).trim() + (String(capa.url).includes('?v=') ? '' : bust);
        }
        // Tenta remontar usando filePath da propria foto da galeria
        const remontada = remontarSePrecisar(String(capa.url || ''), String(capa.filePath || (capa as any).storagePath || ''));
        if (remontada && pareceOK(remontada.split('?')[0])) return remontada;
        // Ultimo recurso: se a foto da galeria parecer carregar, usa como esta
        if (capa.url) return String(capa.url);
      }
    }
    // 3) Ultima tentativa: remontar primary com filePath do modelo se existir em algum lugar
    const prim = String(primaryRaw || '').trim();
    if (prim) return remontarSePrecisar(prim) || null;
    // Nenhuma foto: retorna null (placeholder WB aparece no template, que eh aceitavel)
    return null;
  }

  private _atualizaFotoCapaPreviewDisparo(): void {
    this._forcarRerenderCapa.update(n => n + 1);
  }

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

  readonly galleryPhotos = signal<GalleryPhoto[]>([]);
  /**
   * IDs de midias com ID REAL (nao temp-) existentes no banco de dados NO MOMENTO
   * de carregar a pagina. Usado para detectar o que foi REMOVIDO da grid
   * (por QUALQUER motivo: botao X, drag-drop, selecao em massa, etc) e adicionar
   * em pendingDeleteIds.
   * Funciona para apagar 1 ou 50 fotos, QUANTAS QUISER.
   */
  readonly originalMediaIds = signal<Set<string>>(new Set());
  /**
   * IDs de midias com id REAL (nao temp-) que o usuario apagou da grid.
   * Serao EXCLUIDOS DEFINITIVAMENTE do banco model_media + bucket Supabase
   * SOMENTE ao clicar em SALVAR / ATUALIZAR MODELO.
   */
  readonly pendingDeleteIds = signal<Set<string>>(new Set());
  readonly pendingDeleteCount = computed(() => Array.from(this.pendingDeleteIds()).filter(id => !!id && !String(id).startsWith('temp-')).length);

  /**
   * Compara a galeria ATUAL com os IDs ORIGINAIS do banco.
   * TODO ID REAL que existia no original e NAO EXISTE mais na galeria atual
   * ENTRA em pendingDeleteIds (para exclusao definitiva ao salvar).
   * Funciona apagar 1, 10 ou 50 fotos ao mesmo tempo.
   */
  private detectIdsRemovedFromDatabase(galeriaAtual: GalleryPhoto[]): void {
    const idsAtuais = new Set(
      (galeriaAtual || []).filter(p => p?.id && !String(p.id).startsWith('temp-')).map(p => String(p.id))
    );
    const novos = new Set(this.pendingDeleteIds());
    // Todos os IDs ORIGINAIS do banco que FORAM REMOVIDOS da lista atual:
    for (const originalId of Array.from(this.originalMediaIds())) {
      if (!idsAtuais.has(originalId)) {
        novos.add(String(originalId));
      }
    }
    // Se o usuario DEFEZ (volta a foto com ID na lista), remove de pendingDeleteIds:
    for (const idAtual of Array.from(idsAtuais)) {
      if (novos.has(idAtual)) {
        novos.delete(idAtual);
      }
    }
    this.pendingDeleteIds.set(novos);
  }

  onPhotoRemoved(excluida: GalleryPhoto): void {
    if (!excluida || !excluida.id) return;
    if (String(excluida.id).startsWith('temp-')) return;
    // Evento direto: marca de qualquer jeito, e depois detectIdsRemovedFromDatabase valida.
    const nova = new Set(this.pendingDeleteIds());
    nova.add(String(excluida.id));
    this.pendingDeleteIds.set(nova);
  }

  onPhotosChanged(updatedPhotos: GalleryPhoto[]): void {
    this.galleryPhotos.set(updatedPhotos || []);
    // 🔴 DETECCAO UNIVERSAL: apagar 1, 10, 50 fotos, QUALQUER METODO. Tudo entra no pending.
    this.detectIdsRemovedFromDatabase(updatedPhotos || []);
    if (!updatedPhotos || updatedPhotos.length === 0) {
      this._atualizaFotoCapaPreviewDisparo(); // Forca atualizar mesmo que galeria ficou vazia (placeholder WB)
      return;
    }

    // 🔴 Dispara atualizacao da FOTO DE CAPA no card preview topo
    this._atualizaFotoCapaPreviewDisparo();

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
          // Seta state vazio para IDs originais do banco + zera pendentes
          this.originalMediaIds.set(new Set());
          this.pendingDeleteIds.set(new Set());
          this._atualizaFotoCapaPreviewDisparo(); // Sem fotos: mostra placeholder WB (sem falha)
          return;
        }

        // Garante pelo menos 1 capa (primeira foto se nenhuma for marcada)
        let normalized = [...listaRealOrdenada].sort((a, b) => (a.orderIndex ?? 0) - (b.orderIndex ?? 0));
        if (!normalized.some(p => Boolean(p.isCover))) {
          normalized[0].isCover = true;
        }
        normalized.forEach((p, i) => { p.orderIndex = i; });

        this.galleryPhotos.set(normalized);

        // ==========================================================
        // POPULA OS IDS REAIS do banco de dados no originalMediaIds.
        // A PARTIR DAQUI: qualquer remocao (qualquer metodo) sera detectada.
        // ==========================================================
        const reais = new Set<string>();
        for (const photo of normalized) {
          if (photo?.id && !String(photo.id).startsWith('temp-')) {
            reais.add(String(photo.id));
          }
        }
        this.originalMediaIds.set(reais);
        this.pendingDeleteIds.set(new Set()); // zera lista pendente (recarregou tudo)

        // Sync final: se tiver capa real, atualiza campo primaryPhotoUrl do form
        const realCover = normalized.find(p => Boolean(p.isCover)) || normalized[0];
        if (realCover?.url) {
          this.modelForm.patchValue({ primaryPhotoUrl: realCover.url });
        }
        // 🟢 Atualiza card preview topo com FOTO REAL de capa
        this._atualizaFotoCapaPreviewDisparo();
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

    const parseDecimal = (val: any): number | null => {
      if (val === null || val === undefined || val === '') return null;
      if (typeof val === 'number') return isNaN(val) ? null : val;
      const strVal = String(val).trim().replace(',', '.');
      const num = parseFloat(strVal);
      return isNaN(num) ? null : num;
    };

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
      heightCm: parseDecimal(formVal.heightCm),
      city: formVal.city ? formVal.city.trim() : null,
      nationality: formVal.nationality ? formVal.nationality.trim() : null,
      dressSize: formVal.dressSize ? formVal.dressSize.trim() : null,
      shoeSize: formVal.shoeSize ? formVal.shoeSize.trim() : null,
      bustChestCm: parseDecimal(formVal.bustChestCm),
      waistCm: parseDecimal(formVal.waistCm),
      hipsCm: parseDecimal(formVal.hipsCm),
      hairColor: formVal.hairColor ? formVal.hairColor.trim() : null,
      eyesColor: formVal.eyesColor ? formVal.eyesColor.trim() : null
    };

    const currentId = this.modelId();
    // 🔴 Todos os IDs reais a excluir. Nao confia so no click de photoRemoved: usa PENDING + diff original.
    const todosIdsParaApagar: string[] = Array.from(new Set([
      ...Array.from(this.pendingDeleteIds()),
      ...(this.detectIdsRemovedFromDatabase_Collect(this.galleryPhotos()) || [])
    ])).filter(id => id && !String(id).startsWith('temp-'));

    // ============================================================
    // 1) EXCLUIR DEFINITIVAMENTE TODAS as fotos em lotes de 3 (chunkSize=3).
    // CADA DELETE tem try/catch INDIVIDUAL: um falhar NAO PARA os outros!
    // (Requisito do usuario: apagar MUITAS, quantas quiser, 50+ se precisar)
    // ============================================================
    const excluirEmLotes = new Promise<{ success: number; failed: number }>((resolveAll) => {
      if (!currentId || todosIdsParaApagar.length === 0) {
        resolveAll({ success: 0, failed: 0 });
        return;
      }
      let s = 0;
      let f = 0;
      let idx = 0;
      const total = todosIdsParaApagar.length;
      // Chunk de 3 em 3 (evita HTTP 429 Too Many Requests no bucket Supabase)
      const chunkSize = 3;
      const runChunk = () => {
        if (idx >= total) {
          resolveAll({ success: s, failed: f });
          return;
        }
        const chunkIds = todosIdsParaApagar.slice(idx, idx + chunkSize);
        idx += chunkSize;
        const deleteCalls = chunkIds.map(mediaId =>
          firstValueFrom(
            this.adminModelService.deleteModelMedia(currentId, mediaId).pipe(
              tap(() => { s++; }),
              catchError((err) => {
                console.warn('[onSubmit] Falha ao apagar media=' + mediaId, err);
                f++;
                return of(void 0);
              })
            )
          )
        );
        Promise.all(deleteCalls).finally(() => runChunk());
      };
      runChunk();
    });

    const saveModelOperation = () => {
      if (currentId) {
        return firstValueFrom(
          this.adminModelService.updateModel(currentId, payload).pipe(
            tap((saved) => {
              // Após salvar DADOS: recarrega galeria REAL (confirmacao final sincronia 1:1)
              this._refreshGalleryFromDatabase(currentId).subscribe({
                next: () => {
                  this.showToast('✅ Modelo atualizado com sucesso! Exclusoes, se houver, aplicadas.', 'success');
                  this.saved.emit(saved);
                  setTimeout(() => {
                    if (this.route.snapshot.paramMap.get('id')) {
                      this.router.navigate(['/admin/models']);
                    }
                  }, 1500);
                },
                error: () => {
                  this.saved.emit(saved);
                }
              });
            })
          )
        );
      } else {
        return firstValueFrom(
          this.adminModelService.createModel(payload).pipe(
            tap((saved) => {
              this.showToast('✅ Modelo cadastrado com sucesso!', 'success');
              this.saved.emit(saved);
              setTimeout(() => {
                if (this.route.snapshot.url.some(s => s.path === 'new') || this.route.snapshot.paramMap.get('id')) {
                  this.router.navigate(['/admin/models']);
                }
              }, 1000);
            })
          )
        );
      }
    };

    if (todosIdsParaApagar.length === 0) {
      saveModelOperation().catch((err) => {
        this.isSaving.set(false);
        const msg = err?.error?.detail || err?.error?.message || err?.message || 'Falha geral ao salvar modelo.';
        this.showToast(msg, 'error');
        console.error('[onSubmit] Erro geral:', err);
      }).finally(() => {
        setTimeout(() => { this.isSaving.set(false); }, 500);
      });
      return;
    }

    excluirEmLotes.then((resultDelete) => {
      const { success, failed } = resultDelete;
      this.pendingDeleteIds.set(new Set()); // zera tudo

      // Toast info de exclusao (se teve pelo menos 1)
      if (success + failed > 0) {
        if (failed === 0) {
          this.showToast(`🗑️ ${success} foto(s) apagada(s) DEFINITIVAMENTE do banco e storage Supabase.`, 'success');
        } else {
          this.showToast(`Exclusao: ${success} OK. ${failed} falha(s). Verifique se as fotos de falha ainda existem.`, 'error');
        }
      }

      return saveModelOperation();
    }).catch((err) => {
      this.isSaving.set(false);
      const msg = err?.error?.detail || err?.error?.message || err?.message || 'Falha geral ao salvar modelo.';
      this.showToast(msg, 'error');
      console.error('[onSubmit] Erro geral:', err);
    }).finally(() => {
      setTimeout(() => { this.isSaving.set(false); }, 500);
    });
  }

  /**
   * Helper que coleta IDs a APAGAR: compara a galeria atual com os IDs ORIGINAIS
   * do banco. Retorna array de strings com os IDs que sumiram.
   * (usado no onSubmit como DUPLA CHECAGEM antes de apagar).
   */
  private detectIdsRemovedFromDatabase_Collect(galeriaAtual: GalleryPhoto[]): string[] {
    const idsAtuais = new Set(
      (galeriaAtual || []).filter(p => p?.id && !String(p.id).startsWith('temp-')).map(p => String(p.id))
    );
    const out: string[] = [];
    for (const origId of Array.from(this.originalMediaIds())) {
      if (!idsAtuais.has(origId)) {
        out.push(String(origId));
      }
    }
    return out;
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

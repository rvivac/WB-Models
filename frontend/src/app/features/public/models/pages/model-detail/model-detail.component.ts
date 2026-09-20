import { Component, OnInit, OnDestroy, inject, signal, computed, DestroyRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { ActivatedRoute, RouterModule, Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subscription } from 'rxjs';
import { PublicModelService, ModelDetailPublicDto } from '../../../../../core/services/public-model.service';
import { ModelMeasurementsComponent } from './components/model-measurements/model-measurements.component';
import { ModelLightboxComponent } from './components/model-lightbox/model-lightbox.component';
import { ModelCompositeModalComponent } from './components/model-composite-modal/model-composite-modal.component';
import { TranslatePipe } from '../../../../../shared/pipes/translate.pipe';
import { InstagramUrlPipe, sanitizeInstagramUrl, extractInstagramHandle } from '../../../../../shared/pipes/instagram-url.pipe';

@Component({
  selector: 'app-model-detail',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    ModelMeasurementsComponent,
    ModelLightboxComponent,
    ModelCompositeModalComponent,
    TranslatePipe,
    InstagramUrlPipe
  ],
  templateUrl: './model-detail.component.html',
  styleUrls: ['./model-detail.component.scss']
})
export class ModelDetailComponent implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly http = inject(HttpClient);
  private readonly publicModelService = inject(PublicModelService);
  private readonly destroyRef = inject(DestroyRef);

  // Estados Reativos do Perfil
  readonly isLoading = signal<boolean>(true);
  readonly isNotFound = signal<boolean>(false);
  readonly model = signal<ModelDetailPublicDto | null>(null);

  // Modal de Composite Oficial e Download
  readonly isCompositeModalOpen = signal<boolean>(false);
  readonly isDownloadingComposite = signal<boolean>(false);

  // URL do Composite Oficial
  readonly compositeUrl = computed<string | null>(() => {
    const m = this.model();
    return m?.compositeUrl || m?.composite?.fileUrl || null;
  });

  // Higienização segura do Instagram (suporte a @handle e URL completa)
  readonly sanitizedInstagram = computed<{ url: string; handle: string } | null>(() => {
    const m = this.model();
    const raw = m?.instagramHandle || m?.instagramUrl;
    if (!raw || !raw.trim()) return null;

    const url = sanitizeInstagramUrl(raw);
    const handle = extractInstagramHandle(raw);
    if (!url) return null;

    return { url, handle: handle || '@instagram' };
  });

  // Abas de Galeria: Book vs Polaroids
  readonly activeTab = signal<'BOOK' | 'POLAROIDS'>('BOOK');

  // Estado do Modal Lightbox
  readonly lightboxIndex = signal<number>(-1);
  readonly isLightboxOpen = computed(() => this.lightboxIndex() >= 0);

  // Fotografias da Galeria Ativa
  readonly currentGalleryPhotos = computed(() => {
    const m = this.model();
    if (!m) return [];
    if (this.activeTab() === 'BOOK') {
      return m.bookPhotos || [];
    }
    return m.polaroids || [];
  });

  // Lista de URLs de imagem para o Lightbox
  readonly lightboxImages = computed<string[]>(() => {
    return this.currentGalleryPhotos().map(p => p.fileUrl);
  });

  // Imagem de capa principal
  readonly primaryCoverImage = computed<string>(() => {
    const m = this.model();
    if (!m) return 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?q=80&w=800&auto=format&fit=crop';
    
    // Procura foto com isCover true no book ou primeira foto disponível
    const coverPhoto = m.bookPhotos?.find(p => p.isCover) || m.bookPhotos?.[0] || m.polaroids?.[0];
    return coverPhoto?.fileUrl || 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?q=80&w=800&auto=format&fit=crop';
  });

  private routeSub?: Subscription;

  ngOnInit(): void {
    this.route.paramMap
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(params => {
        const id = params.get('id');
        if (id) {
          this.loadModel(id);
        } else {
          this.isNotFound.set(true);
          this.isLoading.set(false);
        }
      });
  }

  ngOnDestroy(): void {
    this.routeSub?.unsubscribe();
  }

  loadModel(id: string): void {
    this.isLoading.set(true);
    this.isNotFound.set(false);

    this.routeSub?.unsubscribe();
    this.routeSub = this.publicModelService.getModelById(id).subscribe({
      next: (data) => {
        this.model.set(data);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.warn('Modelo público não encontrado ou erro na requisição:', err);
        this.isNotFound.set(true);
        this.model.set(null);
        this.isLoading.set(false);
      }
    });
  }

  setTab(tab: 'BOOK' | 'POLAROIDS'): void {
    this.activeTab.set(tab);
  }

  openLightbox(index: number): void {
    this.lightboxIndex.set(index);
  }

  closeLightbox(): void {
    this.lightboxIndex.set(-1);
  }

  onLightboxIndexChange(newIndex: number): void {
    this.lightboxIndex.set(newIndex);
  }

  getCompositeUrl(): string | null {
    return this.compositeUrl();
  }

  openCompositeModal(): void {
    if (this.compositeUrl()) {
      this.isCompositeModalOpen.set(true);
    }
  }

  closeCompositeModal(): void {
    this.isCompositeModalOpen.set(false);
  }

  downloadComposite(): void {
    const url = this.compositeUrl();
    const stageName = this.model()?.stageName || 'Model';
    if (!url || this.isDownloadingComposite()) return;

    this.isDownloadingComposite.set(true);

    const safeName = stageName.trim().replace(/[^a-zA-Z0-9_-]/g, '_');
    const extMatch = url.match(/\.(pdf|webp|jpe?g|png)(?:\?|$)/i);
    const ext = extMatch ? extMatch[1].toLowerCase() : (url.toLowerCase().includes('.pdf') ? 'pdf' : 'jpg');
    const filename = `Composite_${safeName}.${ext}`;

    this.http.get(url, { responseType: 'blob' }).subscribe({
      next: (blob) => {
        const objectUrl = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = objectUrl;
        link.download = filename;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        window.URL.revokeObjectURL(objectUrl);
        this.isDownloadingComposite.set(false);
      },
      error: () => {
        // Fallback direto
        const link = document.createElement('a');
        link.href = url;
        link.target = '_blank';
        link.download = filename;
        link.rel = 'noopener noreferrer';
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        this.isDownloadingComposite.set(false);
      }
    });
  }

  getBookingRouterLink(): string[] {
    return ['/contact'];
  }

  getBookingQueryParams(): Record<string, string> {
    const m = this.model();
    if (!m) return {};
    return {
      modelId: m.id,
      modelName: m.stageName
    };
  }
}

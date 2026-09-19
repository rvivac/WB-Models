import { Component, OnInit, OnDestroy, inject, signal, computed, DestroyRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule, Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subscription } from 'rxjs';
import { PublicModelService, ModelDetailPublicDto } from '../../../../../core/services/public-model.service';
import { ModelMeasurementsComponent } from './components/model-measurements/model-measurements.component';
import { ModelLightboxComponent } from './components/model-lightbox/model-lightbox.component';
import { TranslatePipe } from '../../../../../shared/pipes/translate.pipe';

@Component({
  selector: 'app-model-detail',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    ModelMeasurementsComponent,
    ModelLightboxComponent,
    TranslatePipe
  ],
  templateUrl: './model-detail.component.html',
  styleUrls: ['./model-detail.component.scss']
})
export class ModelDetailComponent implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly publicModelService = inject(PublicModelService);
  private readonly destroyRef = inject(DestroyRef);

  // Estados Reativos do Perfil
  readonly isLoading = signal<boolean>(true);
  readonly isNotFound = signal<boolean>(false);
  readonly model = signal<ModelDetailPublicDto | null>(null);

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
    const comp = this.model()?.composite;
    return comp?.fileUrl || null;
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

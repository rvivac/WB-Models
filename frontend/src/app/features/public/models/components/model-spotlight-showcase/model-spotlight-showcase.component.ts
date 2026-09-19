import { Component, OnInit, OnDestroy, inject, signal, computed, input, effect, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { PublicModelService, ModelCardPublicDto } from '../../../../../core/services/public-model.service';
import { TranslatePipe } from '../../../../../shared/pipes/translate.pipe';

@Component({
  selector: 'app-model-spotlight-showcase',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslatePipe],
  templateUrl: './model-spotlight-showcase.component.html',
  styleUrls: ['./model-spotlight-showcase.component.scss']
})
export class ModelSpotlightShowcaseComponent implements OnInit, OnDestroy {
  private readonly publicModelService = inject(PublicModelService);
  private readonly router = inject(Router);
  private readonly platformId = inject(PLATFORM_ID);

  // Signal Inputs Customizáveis
  readonly title = input<string>('home.featured_title');
  readonly subtitle = input<string>('home.featured_subtitle');
  readonly limit = input<number>(6);
  readonly filterCategory = input<'FEMALE' | 'MALE' | 'ALL'>('ALL');
  readonly autoPlay = input<boolean>(true);

  // Estados Reativos Internos
  readonly models = signal<ModelCardPublicDto[]>([]);
  readonly currentIndex = signal<number>(0);
  readonly isLoading = signal<boolean>(true);
  readonly isHovered = signal<boolean>(false);

  // Computados
  readonly hasModels = computed(() => this.models().length > 0);
  readonly currentModel = computed(() => this.models()[this.currentIndex()] || null);

  // Imagem de fallback caso coverImageUrl falhe
  readonly defaultCoverImage = 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?q=80&w=1200&auto=format&fit=crop';

  private querySub?: Subscription;
  private autoPlayTimer?: ReturnType<typeof setInterval>;
  private touchStartX = 0;

  constructor() {
    // Efeito para reiniciar busca quando filterCategory ou limit mudarem
    effect(() => {
      const category = this.filterCategory();
      const count = this.limit();
      this.loadSpotlightModels(category, count);
    }, { allowSignalWrites: true });
  }

  ngOnInit(): void {
    this.loadSpotlightModels(this.filterCategory(), this.limit());
    this.startAutoPlay();
  }

  ngOnDestroy(): void {
    this.stopAutoPlay();
    this.querySub?.unsubscribe();
  }

  loadSpotlightModels(category: 'FEMALE' | 'MALE' | 'ALL', count: number): void {
    this.isLoading.set(true);

    const genderParam = category !== 'ALL' ? category : undefined;

    this.querySub?.unsubscribe();
    this.querySub = this.publicModelService.getModels({
      isStar: true,
      gender: genderParam,
      size: count,
      sort: 'createdAt,desc'
    }).subscribe({
      next: (response) => {
        let items = response?.content || [];

        // Fallback: se não houver estrelas cadastradas, busca os primeiros ativos
        if (items.length === 0) {
          this.publicModelService.getModels({
            gender: genderParam,
            size: count,
            sort: 'createdAt,desc'
          }).subscribe({
            next: (fallbackResp) => {
              this.models.set(fallbackResp?.content || []);
              this.currentIndex.set(0);
              this.isLoading.set(false);
            },
            error: () => {
              this.models.set([]);
              this.isLoading.set(false);
            }
          });
        } else {
          this.models.set(items);
          this.currentIndex.set(0);
          this.isLoading.set(false);
        }
      },
      error: (err) => {
        console.warn('Erro ao carregar vitrine spotlight:', err);
        this.models.set([]);
        this.isLoading.set(false);
      }
    });
  }

  // -----------------------------------------------------------
  // Navegação do Carrossel
  // -----------------------------------------------------------
  nextSlide(): void {
    const total = this.models().length;
    if (total <= 1) return;
    this.currentIndex.update(idx => (idx < total - 1 ? idx + 1 : 0));
  }

  prevSlide(): void {
    const total = this.models().length;
    if (total <= 1) return;
    this.currentIndex.update(idx => (idx > 0 ? idx - 1 : total - 1));
  }

  goToSlide(index: number): void {
    if (index >= 0 && index < this.models().length) {
      this.currentIndex.set(index);
    }
  }

  // -----------------------------------------------------------
  // Controle de Autoplay com Pausa em Hover / Foco
  // -----------------------------------------------------------
  startAutoPlay(): void {
    if (!isPlatformBrowser(this.platformId) || !this.autoPlay()) return;

    this.stopAutoPlay();
    this.autoPlayTimer = setInterval(() => {
      if (!this.isHovered() && this.models().length > 1) {
        this.nextSlide();
      }
    }, 5000);
  }

  stopAutoPlay(): void {
    if (this.autoPlayTimer) {
      clearInterval(this.autoPlayTimer);
      this.autoPlayTimer = undefined;
    }
  }

  pauseAutoPlay(): void {
    this.isHovered.set(true);
  }

  resumeAutoPlay(): void {
    this.isHovered.set(false);
  }

  // -----------------------------------------------------------
  // Suporte a Touch-Swipe em Mobile
  // -----------------------------------------------------------
  onTouchStart(event: TouchEvent): void {
    this.touchStartX = event.touches[0].clientX;
  }

  onTouchEnd(event: TouchEvent): void {
    const touchEndX = event.changedTouches[0].clientX;
    const diff = touchEndX - this.touchStartX;

    if (Math.abs(diff) > 45) {
      if (diff > 0) {
        this.prevSlide();
      } else {
        this.nextSlide();
      }
    }
  }

  getCoverImage(model: ModelCardPublicDto | null): string {
    if (!model) return this.defaultCoverImage;
    return model.coverImageUrl && model.coverImageUrl.trim() !== ''
      ? model.coverImageUrl
      : this.defaultCoverImage;
  }
}

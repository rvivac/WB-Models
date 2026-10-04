import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { PublicModelService, ModelCardPublicDto } from '../../../../../core/services/public-model.service';
import { TranslatePipe } from '../../../../../shared/pipes/translate.pipe';

@Component({
  selector: 'app-home-featured-models',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslatePipe],
  templateUrl: './home-featured-models.component.html',
  styleUrls: ['./home-featured-models.component.scss']
})
export class HomeFeaturedModelsComponent implements OnInit {
  private readonly publicModelService = inject(PublicModelService);

  readonly isLoading = signal<boolean>(true);
  readonly hasError = signal<boolean>(false);
  readonly models = signal<ModelCardPublicDto[]>([]);

  readonly hasModels = computed(() => this.models().length > 0);
  readonly skeletonArray = [1, 2, 3, 4, 5, 6, 7, 8];

  // Imagem de fallback de alta moda caso o modelo não possua coverImageUrl configurada
  // ou se a imagem retornar erro HTTP 400/404/403 (URL incompleta, bucket indisponivel, etc)
  readonly defaultCoverImage =
    'https://images.unsplash.com/photo-1534528741775-53994a69daeb?q=80&w=900&auto=format&fit=crop';

  // Guardamos IDs dos modelos cuja imagem falhou no carregamento (usa fallback placeholder)
  readonly brokenImageIds = signal<Set<string>>(new Set());

  ngOnInit(): void {
    this.loadFeatured();
  }

  loadFeatured(): void {
    this.isLoading.set(true);
    this.hasError.set(false);
    this.brokenImageIds.set(new Set());

    this.publicModelService.getFeaturedModels(8).subscribe({
      next: (response) => {
        const items = response?.content || [];
        this.models.set(items);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.warn('Erro ao buscar modelos em destaque:', err);
        this.hasError.set(true);
        this.isLoading.set(false);
      }
    });
  }

  /**
   * Chamado pelo (error) da tag <img> do modelo quando a imagem falha
   * (ex: URL incompleta, bucket indisponivel, HTTP 400).
   * Marca o modelo como "falhou" para trocar pro placeholder Unsplash.
   */
  onCoverError(modelId: string): void {
    if (!modelId) return;
    this.brokenImageIds.update(prev => {
      const nxt = new Set(prev);
      nxt.add(modelId);
      return nxt;
    });
  }

  getCoverImage(model: ModelCardPublicDto): string {
    // 1) Se a imagem já falhou para este modelo, usa placeholder direto
    if (model?.id && this.brokenImageIds().has(model.id)) {
      return this.defaultCoverImage;
    }
    // 2) Se tem coverImageUrl e não parece vazia / incompleta
    if (model?.coverImageUrl && model.coverImageUrl.trim() !== '') {
      const url = model.coverImageUrl.trim();
      // Protecao: URLs incompletas (ex: "/public/models-media/" sem path de arquivo)
      // nao terminam com / ou tem o caminho menor que 30 chars apos o bucket = usamos fallback
      if (url.endsWith('/') || url.length < 40) {
        return this.defaultCoverImage;
      }
      return url;
    }
    // 3) Fallback final: placeholder editorial.
    return this.defaultCoverImage;
  }
}

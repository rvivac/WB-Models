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
  readonly defaultCoverImage = 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?q=80&w=800&auto=format&fit=crop';

  ngOnInit(): void {
    this.loadFeatured();
  }

  loadFeatured(): void {
    this.isLoading.set(true);
    this.hasError.set(false);

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

  getCoverImage(model: ModelCardPublicDto): string {
    return model.coverImageUrl && model.coverImageUrl.trim() !== ''
      ? model.coverImageUrl
      : this.defaultCoverImage;
  }
}

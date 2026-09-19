import { Component, OnInit, OnDestroy, inject, signal, computed, DestroyRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { combineLatest, Subscription } from 'rxjs';
import { PublicModelService, ModelCardPublicDto, ModelFilterParams } from '../../../core/services/public-model.service';
import { ModelCardComponent } from './components/model-card/model-card.component';
import { ModelFiltersComponent, FilterChangeEvent } from './components/model-filters/model-filters.component';
import { ModelSpotlightShowcaseComponent } from './components/model-spotlight-showcase/model-spotlight-showcase.component';
import { TranslatePipe } from '../../../shared/pipes/translate.pipe';

@Component({
  selector: 'app-model-list',
  standalone: true,
  imports: [
    CommonModule, 
    RouterModule, 
    ModelCardComponent, 
    ModelFiltersComponent, 
    ModelSpotlightShowcaseComponent,
    TranslatePipe
  ],
  templateUrl: './model-list.component.html',
  styleUrls: ['./model-list.component.scss']
})
export class ModelListComponent implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly publicModelService = inject(PublicModelService);
  private readonly destroyRef = inject(DestroyRef);

  // Estados Reativos Principais
  readonly isLoading = signal<boolean>(true);
  readonly models = signal<ModelCardPublicDto[]>([]);
  readonly totalElements = signal<number>(0);
  readonly totalPages = signal<number>(1);
  readonly currentPage = signal<number>(1); // 1-based para a URL do usuário
  readonly pageSize = 24;

  readonly search = signal<string>('');
  readonly sort = signal<string>('stageName,asc');
  readonly currentGender = signal<'FEMALE' | 'MALE' | undefined>(undefined);
  readonly isStarCategory = signal<boolean>(false);

  readonly skeletonArray = Array.from({ length: 12 }, (_, i) => i + 1);

  // Título e Subtítulo Editoriais Computados
  readonly headerTitleKey = computed(() => {
    if (this.isStarCategory()) {
      return 'models.stars_title';
    }
    if (this.currentGender() === 'MALE') {
      return 'models.male_title';
    }
    return 'models.female_title';
  });

  readonly headerSubtitleKey = computed(() => {
    if (this.isStarCategory()) {
      return 'models.stars_subtitle';
    }
    if (this.currentGender() === 'MALE') {
      return 'models.male_subtitle';
    }
    return 'models.female_subtitle';
  });

  private querySub?: Subscription;

  ngOnInit(): void {
    // Escuta sincronizada de Route Data (categoria/gênero) e QueryParams (filtros/paginação)
    combineLatest([this.route.data, this.route.queryParams])
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(([data, qp]) => {
        // Resolução de Categoria via Route Data
        const gender = data['gender'] as 'FEMALE' | 'MALE' | undefined;
        const isStar = Boolean(data['isStar']);

        this.currentGender.set(gender);
        this.isStarCategory.set(isStar);

        // Extração de Query Params (com defaults elegantes)
        const pageFromUrl = parseInt(qp['page'], 10);
        const validPage = !isNaN(pageFromUrl) && pageFromUrl > 0 ? pageFromUrl : 1;
        const searchFromUrl = typeof qp['search'] === 'string' ? qp['search'] : '';
        const sortFromUrl = typeof qp['sort'] === 'string' && qp['sort'].trim() ? qp['sort'] : 'stageName,asc';

        this.currentPage.set(validPage);
        this.search.set(searchFromUrl);
        this.sort.set(sortFromUrl);

        this.fetchModels();
      });
  }

  ngOnDestroy(): void {
    this.querySub?.unsubscribe();
  }

  fetchModels(): void {
    this.isLoading.set(true);

    const apiParams: ModelFilterParams = {
      gender: this.currentGender(),
      isStar: this.isStarCategory() ? true : undefined,
      search: this.search(),
      page: this.currentPage() - 1, // Conversão para 0-based da API
      size: this.pageSize,
      sort: this.sort()
    };

    this.querySub?.unsubscribe();
    this.querySub = this.publicModelService.getModels(apiParams).subscribe({
      next: (response) => {
        const items = response?.content || [];
        this.models.set(items);
        this.totalElements.set(response?.totalElements ?? items.length);
        this.totalPages.set(response?.totalPages && response.totalPages > 0 ? response.totalPages : 1);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.warn('Erro ao carregar casting público:', err);
        this.models.set([]);
        this.totalElements.set(0);
        this.totalPages.set(1);
        this.isLoading.set(false);
      }
    });
  }

  onFilterChange(event: FilterChangeEvent): void {
    this.updateQueryParams({
      search: event.search || null,
      sort: event.sort !== 'stageName,asc' ? event.sort : null,
      page: null // Reseta para a página 1 ao alterar filtros
    });
  }

  onResetFilters(): void {
    this.updateQueryParams({
      search: null,
      sort: null,
      page: null
    });
  }

  goToPage(page: number): void {
    if (page < 1 || page > this.totalPages() || page === this.currentPage()) {
      return;
    }
    this.updateQueryParams({
      page: page > 1 ? page : null
    });

    // Rolagem suave para o topo da grelha
    if (typeof window !== 'undefined') {
      window.scrollTo({ top: 180, behavior: 'smooth' });
    }
  }

  private updateQueryParams(params: Record<string, string | number | null>): void {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: params,
      queryParamsHandling: 'merge'
    });
  }
}

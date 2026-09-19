import { Injectable, inject } from '@angular/core';
import { Observable, catchError, map, of, switchMap } from 'rxjs';
import { ApiService } from './api.service';

export interface ModelCardPublicDto {
  id: string;
  stageName: string;
  gender: 'FEMALE' | 'MALE';
  coverImageUrl?: string;
  heightCm?: number;
  city?: string;
  isStar?: boolean;
}

export interface PageResponseDto<T> {
  content: T[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  isLast?: boolean;
  last?: boolean;
}

export interface ModelFilterParams {
  gender?: 'FEMALE' | 'MALE' | string;
  isStar?: boolean;
  search?: string;
  page?: number;
  size?: number;
  sort?: string;
}

export interface ModelMediaPublicItemDto {
  id: string;
  fileUrl: string;
  displayOrder?: number;
  isCover?: boolean;
}

export interface ModelDetailPublicDto {
  id: string;
  stageName: string;
  gender: 'FEMALE' | 'MALE';
  isStar?: boolean;
  city?: string;
  nationality?: string;
  age?: number;
  instagramUrl?: string;

  // Medidas biométricas (renderizadas apenas quando presentes)
  heightCm?: number;
  bustChestCm?: number;
  waistCm?: number;
  hipsCm?: number;
  dressSize?: string;
  shoeSize?: string;
  eyeColor?: string;
  hairColor?: string;

  // Mídias categorizadas
  bookPhotos?: ModelMediaPublicItemDto[];
  polaroids?: ModelMediaPublicItemDto[];
  composite?: ModelMediaPublicItemDto;
}

@Injectable({
  providedIn: 'root'
})
export class PublicModelService {
  private readonly api = inject(ApiService);

  /**
   * Busca modelos em destaque (Stars) para a Home Page.
   * Caso não haja modelos marcados como Star, busca os modelos ativos mais recentes.
   */
  getFeaturedModels(limit: number = 8): Observable<PageResponseDto<ModelCardPublicDto>> {
    return this.api.get<PageResponseDto<ModelCardPublicDto>>('/public/models', {
      isStar: true,
      size: limit,
      page: 0
    }).pipe(
      switchMap(response => {
        // Se a busca por Stars retornar modelos, retorna a resposta
        if (response && response.content && response.content.length > 0) {
          return of(response);
        }
        // Fallback: busca modelos ativos em geral para garantir apresentação visual rica
        return this.api.get<PageResponseDto<ModelCardPublicDto>>('/public/models', {
          size: limit,
          page: 0
        });
      }),
      catchError(err => {
        console.warn('Falha ao carregar modelos em destaque da API pública:', err);
        return of({
          content: [],
          pageNumber: 0,
          pageSize: limit,
          totalElements: 0,
          totalPages: 0,
          isLast: true
        });
      })
    );
  }

  /**
   * Busca casting de modelos com suporte a filtros por gênero, stars, pesquisa textual e paginação.
   */
  getModels(params?: ModelFilterParams): Observable<PageResponseDto<ModelCardPublicDto>> {
    const cleanParams: Record<string, string | number | boolean> = {};

    if (params) {
      if (params.gender) {
        cleanParams['gender'] = params.gender;
      }
      if (typeof params.isStar === 'boolean') {
        cleanParams['isStar'] = params.isStar;
      }
      if (params.search && params.search.trim().length > 0) {
        cleanParams['search'] = params.search.trim();
      }
      if (typeof params.page === 'number' && params.page >= 0) {
        cleanParams['page'] = params.page;
      }
      if (typeof params.size === 'number' && params.size > 0) {
        cleanParams['size'] = params.size;
      }
      if (params.sort && params.sort.trim().length > 0) {
        cleanParams['sort'] = params.sort.trim();
      }
    }

    return this.api.get<PageResponseDto<ModelCardPublicDto>>('/public/models', cleanParams);
  }

  /**
   * Consulta os detalhes completos de um modelo ativo pelo ID (UUID).
   */
  getModelById(id: string): Observable<ModelDetailPublicDto> {
    return this.api.get<ModelDetailPublicDto>(`/public/models/${id}`);
  }

  /**
   * Alias de compatibilidade retroativa para getModelById
   */
  getModelDetail(id: string): Observable<ModelDetailPublicDto> {
    return this.getModelById(id);
  }
}

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
  instagramHandle?: string;

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
  compositeUrl?: string;
}

export const MOCK_MODELS: ModelDetailPublicDto[] = [
  {
    id: 'f1a2b3c4-1111-4000-8000-000000000001',
    stageName: 'Helena Rostova',
    gender: 'FEMALE',
    isStar: true,
    city: 'São Paulo',
    nationality: 'Brasileira',
    age: 22,
    instagramHandle: '@helenarostova',
    instagramUrl: 'https://instagram.com/helenarostova',
    heightCm: 179,
    bustChestCm: 84,
    waistCm: 60,
    hipsCm: 90,
    dressSize: '36',
    shoeSize: '38',
    eyeColor: 'Verdes',
    hairColor: 'Castanho Claro',
    bookPhotos: [
      { id: 'bp-101', fileUrl: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?q=80&w=1200&auto=format&fit=crop', isCover: true, displayOrder: 1 },
      { id: 'bp-102', fileUrl: 'https://images.unsplash.com/photo-1517841905240-472988babdf9?q=80&w=1200&auto=format&fit=crop', isCover: false, displayOrder: 2 },
      { id: 'bp-103', fileUrl: 'https://images.unsplash.com/photo-1524504388940-b1c1722653e1?q=80&w=1200&auto=format&fit=crop', isCover: false, displayOrder: 3 },
      { id: 'bp-104', fileUrl: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?q=80&w=1200&auto=format&fit=crop', isCover: false, displayOrder: 4 }
    ],
    polaroids: [
      { id: 'pol-101', fileUrl: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?q=80&w=800&auto=format&fit=crop', displayOrder: 1 },
      { id: 'pol-102', fileUrl: 'https://images.unsplash.com/photo-1517841905240-472988babdf9?q=80&w=800&auto=format&fit=crop', displayOrder: 2 }
    ],
    compositeUrl: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?q=80&w=1200&auto=format&fit=crop'
  },
  {
    id: 'f1a2b3c4-2222-4000-8000-000000000002',
    stageName: 'Isabella Martins',
    gender: 'FEMALE',
    isStar: true,
    city: 'Rio de Janeiro',
    nationality: 'Brasileira',
    age: 24,
    instagramHandle: '@isabellamartins',
    instagramUrl: 'https://instagram.com/isabellamartins',
    heightCm: 180,
    bustChestCm: 86,
    waistCm: 61,
    hipsCm: 91,
    dressSize: '36',
    shoeSize: '39',
    eyeColor: 'Castanhos',
    hairColor: 'Castanho Escuro',
    bookPhotos: [
      { id: 'bp-201', fileUrl: 'https://images.unsplash.com/photo-1524504388940-b1c1722653e1?q=80&w=1200&auto=format&fit=crop', isCover: true, displayOrder: 1 },
      { id: 'bp-202', fileUrl: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?q=80&w=1200&auto=format&fit=crop', isCover: false, displayOrder: 2 },
      { id: 'bp-203', fileUrl: 'https://images.unsplash.com/photo-1529626455594-4ff0802cfb7e?q=80&w=1200&auto=format&fit=crop', isCover: false, displayOrder: 3 }
    ],
    polaroids: [
      { id: 'pol-201', fileUrl: 'https://images.unsplash.com/photo-1524504388940-b1c1722653e1?q=80&w=800&auto=format&fit=crop', displayOrder: 1 }
    ],
    compositeUrl: 'https://images.unsplash.com/photo-1524504388940-b1c1722653e1?q=80&w=1200&auto=format&fit=crop'
  },
  {
    id: 'f1a2b3c4-3333-4000-8000-000000000003',
    stageName: 'Valentina Rossi',
    gender: 'FEMALE',
    isStar: false,
    city: 'Curitiba',
    nationality: 'Brasileira',
    age: 21,
    instagramHandle: '@valentinarossi',
    instagramUrl: 'https://instagram.com/valentinarossi',
    heightCm: 178,
    bustChestCm: 83,
    waistCm: 59,
    hipsCm: 89,
    dressSize: '34',
    shoeSize: '37',
    eyeColor: 'Azuis',
    hairColor: 'Loiro',
    bookPhotos: [
      { id: 'bp-301', fileUrl: 'https://images.unsplash.com/photo-1517841905240-472988babdf9?q=80&w=1200&auto=format&fit=crop', isCover: true, displayOrder: 1 },
      { id: 'bp-302', fileUrl: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?q=80&w=1200&auto=format&fit=crop', isCover: false, displayOrder: 2 }
    ],
    polaroids: [
      { id: 'pol-301', fileUrl: 'https://images.unsplash.com/photo-1517841905240-472988babdf9?q=80&w=800&auto=format&fit=crop', displayOrder: 1 }
    ],
    compositeUrl: 'https://images.unsplash.com/photo-1517841905240-472988babdf9?q=80&w=1200&auto=format&fit=crop'
  },
  {
    id: 'f1a2b3c4-4444-4000-8000-000000000004',
    stageName: 'Camila Becker',
    gender: 'FEMALE',
    isStar: false,
    city: 'Porto Alegre',
    nationality: 'Brasileira',
    age: 23,
    instagramHandle: '@camilabecker',
    instagramUrl: 'https://instagram.com/camilabecker',
    heightCm: 177,
    bustChestCm: 85,
    waistCm: 60,
    hipsCm: 90,
    dressSize: '36',
    shoeSize: '38',
    eyeColor: 'Verdes',
    hairColor: 'Castanho',
    bookPhotos: [
      { id: 'bp-401', fileUrl: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?q=80&w=1200&auto=format&fit=crop', isCover: true, displayOrder: 1 }
    ],
    polaroids: [
      { id: 'pol-401', fileUrl: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?q=80&w=800&auto=format&fit=crop', displayOrder: 1 }
    ],
    compositeUrl: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?q=80&w=1200&auto=format&fit=crop'
  },
  {
    id: 'm1a2b3c4-5555-4000-8000-000000000005',
    stageName: 'Lucas Albuquerque',
    gender: 'MALE',
    isStar: true,
    city: 'São Paulo',
    nationality: 'Brasileiro',
    age: 25,
    instagramHandle: '@lucasalbuquerque',
    instagramUrl: 'https://instagram.com/lucasalbuquerque',
    heightCm: 188,
    bustChestCm: 98,
    waistCm: 76,
    hipsCm: 96,
    dressSize: '42',
    shoeSize: '43',
    eyeColor: 'Castanhos',
    hairColor: 'Preto',
    bookPhotos: [
      { id: 'bp-501', fileUrl: 'https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?q=80&w=1200&auto=format&fit=crop', isCover: true, displayOrder: 1 },
      { id: 'bp-502', fileUrl: 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?q=80&w=1200&auto=format&fit=crop', isCover: false, displayOrder: 2 }
    ],
    polaroids: [
      { id: 'pol-501', fileUrl: 'https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?q=80&w=800&auto=format&fit=crop', displayOrder: 1 }
    ],
    compositeUrl: 'https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?q=80&w=1200&auto=format&fit=crop'
  },
  {
    id: 'm1a2b3c4-6666-4000-8000-000000000006',
    stageName: 'Gabriel Alencar',
    gender: 'MALE',
    isStar: true,
    city: 'Florianópolis',
    nationality: 'Brasileiro',
    age: 26,
    instagramHandle: '@gabrielalencar',
    instagramUrl: 'https://instagram.com/gabrielalencar',
    heightCm: 187,
    bustChestCm: 99,
    waistCm: 77,
    hipsCm: 97,
    dressSize: '42',
    shoeSize: '43',
    eyeColor: 'Verdes',
    hairColor: 'Castanho Claro',
    bookPhotos: [
      { id: 'bp-601', fileUrl: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?q=80&w=1200&auto=format&fit=crop', isCover: true, displayOrder: 1 },
      { id: 'bp-602', fileUrl: 'https://images.unsplash.com/photo-1492562080023-ab3db95bfbce?q=80&w=1200&auto=format&fit=crop', isCover: false, displayOrder: 2 }
    ],
    polaroids: [
      { id: 'pol-601', fileUrl: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?q=80&w=800&auto=format&fit=crop', displayOrder: 1 }
    ],
    compositeUrl: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?q=80&w=1200&auto=format&fit=crop'
  },
  {
    id: 'm1a2b3c4-7777-4000-8000-000000000007',
    stageName: 'Mateus Silva',
    gender: 'MALE',
    isStar: false,
    city: 'Belo Horizonte',
    nationality: 'Brasileiro',
    age: 22,
    instagramHandle: '@mateussilva',
    instagramUrl: 'https://instagram.com/mateussilva',
    heightCm: 186,
    bustChestCm: 96,
    waistCm: 75,
    hipsCm: 94,
    dressSize: '40',
    shoeSize: '42',
    eyeColor: 'Castanhos',
    hairColor: 'Castanho Escuro',
    bookPhotos: [
      { id: 'bp-701', fileUrl: 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?q=80&w=1200&auto=format&fit=crop', isCover: true, displayOrder: 1 }
    ],
    polaroids: [
      { id: 'pol-701', fileUrl: 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?q=80&w=800&auto=format&fit=crop', displayOrder: 1 }
    ],
    compositeUrl: 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?q=80&w=1200&auto=format&fit=crop'
  },
  {
    id: 'm1a2b3c4-8888-4000-8000-000000000008',
    stageName: 'Rodrigo Vasconcelos',
    gender: 'MALE',
    isStar: false,
    city: 'Rio de Janeiro',
    nationality: 'Brasileiro',
    age: 24,
    instagramHandle: '@rodrigovasconcelos',
    instagramUrl: 'https://instagram.com/rodrigovasconcelos',
    heightCm: 189,
    bustChestCm: 100,
    waistCm: 78,
    hipsCm: 98,
    dressSize: '42',
    shoeSize: '44',
    eyeColor: 'Azuis',
    hairColor: 'Loiro Escuro',
    bookPhotos: [
      { id: 'bp-801', fileUrl: 'https://images.unsplash.com/photo-1492562080023-ab3db95bfbce?q=80&w=1200&auto=format&fit=crop', isCover: true, displayOrder: 1 }
    ],
    polaroids: [
      { id: 'pol-801', fileUrl: 'https://images.unsplash.com/photo-1492562080023-ab3db95bfbce?q=80&w=800&auto=format&fit=crop', displayOrder: 1 }
    ],
    compositeUrl: 'https://images.unsplash.com/photo-1492562080023-ab3db95bfbce?q=80&w=1200&auto=format&fit=crop'
  }
];

export function toMockModelCard(m: ModelDetailPublicDto): ModelCardPublicDto {
  return {
    id: m.id,
    stageName: m.stageName,
    gender: m.gender,
    isStar: m.isStar,
    heightCm: m.heightCm,
    city: m.city,
    coverImageUrl: m.bookPhotos?.find(p => p.isCover)?.fileUrl || m.bookPhotos?.[0]?.fileUrl || m.compositeUrl
  };
}

@Injectable({
  providedIn: 'root'
})
export class PublicModelService {
  private readonly api = inject(ApiService);

  /**
   * Constrói resposta paginada e filtrada a partir da base estática de modelos.
   */
  getMockPageResponse(params?: ModelFilterParams): PageResponseDto<ModelCardPublicDto> {
    let list = MOCK_MODELS.map(toMockModelCard);

    if (params) {
      if (params.gender) {
        const targetGender = params.gender.toUpperCase();
        list = list.filter(m => m.gender.toUpperCase() === targetGender);
      }
      if (typeof params.isStar === 'boolean') {
        list = list.filter(m => Boolean(m.isStar) === params.isStar);
      }
      if (params.search && params.search.trim().length > 0) {
        const query = params.search.trim().toLowerCase();
        list = list.filter(m => 
          m.stageName.toLowerCase().includes(query) || 
          (m.city && m.city.toLowerCase().includes(query))
        );
      }
      if (params.sort) {
        const [field, direction] = params.sort.split(',');
        const isDesc = direction?.toLowerCase() === 'desc';
        list.sort((a, b) => {
          if (field === 'heightCm' || field === 'height') {
            const hA = a.heightCm || 0;
            const hB = b.heightCm || 0;
            return isDesc ? hB - hA : hA - hB;
          }
          const nameA = a.stageName.toLowerCase();
          const nameB = b.stageName.toLowerCase();
          return isDesc ? nameB.localeCompare(nameA) : nameA.localeCompare(nameB);
        });
      }
    }

    const page = typeof params?.page === 'number' && params.page >= 0 ? params.page : 0;
    const size = typeof params?.size === 'number' && params.size > 0 ? params.size : (params?.size ?? 24);
    const totalElements = list.length;
    const totalPages = Math.ceil(totalElements / size) || 1;
    const start = page * size;
    const content = list.slice(start, start + size);
    const isLast = page >= totalPages - 1;

    return {
      content,
      pageNumber: page,
      pageSize: size,
      totalElements,
      totalPages,
      isLast,
      last: isLast
    };
  }

  /**
   * Busca modelos em destaque (Stars) para a Home Page.
   * Caso não haja modelos marcados como Star, busca os modelos ativos mais recentes.
   * Em caso de falha da API (ex: GitHub Pages estático), utiliza os dados de mock.
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
        console.warn('Falha ao carregar modelos em destaque da API pública, utilizando catálogo estático de fallback:', err);
        return of(this.getMockPageResponse({ isStar: true, size: limit, page: 0 }));
      })
    );
  }

  /**
   * Busca casting de modelos com suporte a filtros por gênero, stars, pesquisa textual e paginação.
   * Em caso de falha na requisição, retorna os modelos correspondentes do mock.
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

    return this.api.get<PageResponseDto<ModelCardPublicDto>>('/public/models', cleanParams).pipe(
      catchError(err => {
        console.warn('Falha ao buscar casting da API pública, utilizando catálogo estático de fallback:', err);
        return of(this.getMockPageResponse(params));
      })
    );
  }

  /**
   * Consulta os detalhes completos de um modelo ativo pelo ID (UUID).
   * Em caso de falha na requisição, localiza no catálogo mock ou retorna o primeiro modelo.
   */
  getModelById(id: string): Observable<ModelDetailPublicDto> {
    return this.api.get<ModelDetailPublicDto>(`/public/models/${id}`).pipe(
      catchError(err => {
        console.warn(`Falha ao buscar detalhes do modelo ${id}, utilizando mock de fallback:`, err);
        const found = MOCK_MODELS.find(m => m.id === id) || MOCK_MODELS[0];
        return of(found);
      })
    );
  }

  /**
   * Alias de compatibilidade retroativa para getModelById
   */
  getModelDetail(id: string): Observable<ModelDetailPublicDto> {
    return this.getModelById(id);
  }
}

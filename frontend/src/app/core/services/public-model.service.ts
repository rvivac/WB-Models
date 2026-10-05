import { Injectable, inject } from '@angular/core';
import { Observable, catchError, map, of, switchMap } from 'rxjs';
import { ApiService } from './api.service';

export interface ModelCardPublicDto {
  id: string;
  stageName: string;
  gender: 'FEMALE' | 'MALE';
  coverImageUrl?: string;
  /** Caminho absoluto do arquivo no Storage (ex: "models-media/eve-duppre/abc-cover.jpg"). Usado APENAS como FALLBACK caso coverImageUrl esteja incompleta (HTTP 400 Supabase). */
  filePath?: string;
  /** @deprecated Use filePath. Mantido para compatibilidade com DTOs legados. */
  storagePath?: string;
  heightCm?: number;
  city?: string;
  isStar?: boolean;
  isFeaturedHome?: boolean;
  featuredOrder?: number;
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
  /** Caminho absoluto do arquivo no Storage (ex: "models-media/eve-duppre/abc-cover.jpg"). Usado APENAS como FALLBACK caso fileUrl esteja incompleta (HTTP 400 Supabase). */
  filePath?: string;
  /** @deprecated Use filePath. Mantido para compatibilidade com DTOs legados. */
  storagePath?: string;
}

export interface ModelCompositePublicDto {
  id: string;
  fileUrl: string;
  /** Mesmo fallback: usado para remontar URL se fileUrl vier incompleta */
  filePath?: string;
  /** @deprecated Use filePath. Mantido para compatibilidade com DTOs legados. */
  storagePath?: string;
  fileName?: string;
  fileType?: 'PDF' | 'IMAGE';
  fileSizeBytes?: number;
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
  /** @deprecated Use composite?.fileUrl. Mantido para DTOs legados. */
  composite?: ModelCompositePublicDto;
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
   * Helper ANTI-FRACO para foto de capa dos cards.
   * Resolve 2 problemas que faziam Eve Duppre NAO APARECER foto 1 na Home:
   *   1) coverImageUrl incompleta (ex: "/public/models-media/" SEM path do arquivo) -> HTTP 400 no Supabase
   *   2) coverImageUrl nula / vazia
   * Solucao: se URL parecer incompleta (ou terminando em /, ou comprimento curto, ou sem path objeto),
   *          concatena filePath no final.
   * Sempre adiciona bust-cache ?v=epochHour para aparecer imediatamente novas fotos no publico.
   */
   private _resolveCoverForCard(card: ModelCardPublicDto | any): ModelCardPublicDto {
    if (!card) return card;
    // 🆕 PROJETO REAL DE PRODUÇÃO stmytwsdlonpnirqiufq (confirmado: backend, index.html, arquivos físicos!)
    const SUPABASE_PUBLIC_BASE = 'https://stmytwsdlonpnirqiufq.supabase.co/storage/v1/object/public/models-media';
    const SUPABASE_HOST = 'https://stmytwsdlonpnirqiufq.supabase.co';

    const filePath = (card as any).filePath || (card as any).storagePath || '';
    let rawUrl = (card.coverImageUrl || '').trim();

    // 1) URL vazia + filePath? remontar
    if (!rawUrl && filePath) {
      rawUrl = `${SUPABASE_PUBLIC_BASE}/${filePath.replace(/^\//, '')}`;
    }
    // 2) URL incompleta + filePath? remontar
    const seemsIncomplete = !rawUrl.startsWith('http')
      || rawUrl.endsWith('/')
      || (filePath && rawUrl.length < 40 && !rawUrl.includes(filePath.substring(filePath.lastIndexOf('/') + 1)));

    if (seemsIncomplete && filePath) {
      const pathClean = filePath.replace(/^\//, '');
      if (rawUrl && !rawUrl.startsWith('http')) {
        if (rawUrl.includes('/models-media/') && !rawUrl.includes(pathClean)) {
          const base = rawUrl.endsWith('/') ? rawUrl : (rawUrl + '/');
          rawUrl = (base.startsWith('http') ? '' : SUPABASE_HOST) + (base + pathClean).replace(/([^:]\/)\/+/g, '$1');
        } else if (!rawUrl.includes(pathClean)) {
          rawUrl = `${SUPABASE_PUBLIC_BASE}/${pathClean}`;
        }
      } else if (!rawUrl) {
        rawUrl = `${SUPABASE_PUBLIC_BASE}/${pathClean}`;
      }
    }

    // 3) Bust cache 1h — SEM PARAMETRO DUPLICADO (nunca mais ?v=A&v=B)
    if (rawUrl) {
      let cleaned = rawUrl;
      if (cleaned.includes('v=')) {
        cleaned = cleaned
          .replace(/([?&])v=[^&]*(&|$)/g, (m: any, sep: any, end: any) => (end === '&' ? sep : ''))
          .replace(/[?&]$/, '');
      }
      const sep = cleaned.includes('?') ? '&' : '?';
      rawUrl = `${cleaned}${sep}v=${Math.floor(Date.now() / 3_600_000)}`;
    }
    return { ...card, coverImageUrl: rawUrl || undefined };
  }

  /**
   * Aplica _resolveCoverForCard() em TODO array de cards (vitrine/casting/gender).
   * ✅ RECOLOCADO: Era usado em getFeaturedModels() e getModels() para capas dos cards home/casting.
   */
  private _applyCoverFallbacks<T extends ModelCardPublicDto>(items: T[]): T[] {
    if (!items || items.length === 0) return items;
    return items.map(m => this._resolveCoverForCard(m) as T);
  }

  /**
   * Aplica em PageResponseDto (casting) sem perder a paginação.
   * ✅ RECOLOCADO: Também era usado em getFeaturedModels() e getModels().
   */
  private _applyCoverFallbacksPage<T extends ModelCardPublicDto>(page: PageResponseDto<T>): PageResponseDto<T> {
    if (!page) return page;
    return { ...page, content: this._applyCoverFallbacks(page.content || []) };
  }

  /**
   * Helper ANTI-URL-INCOMPLETA para MÍDIAS em geral (Book / Polas / Composite).
   * Mesma logica do _resolveAdminCoverUrl e helper admin.
   *
   * Problema: Backend as vezes grava fileUrl incompleta (ex: "/public/models-media/" sem
   * path do objeto) → HTTP 400 no bucket Supabase. composite?.fileUrl = incompleta → Download
   * Sedcard 404 + Ver Composite iframe VAZIO.
   *
   * Solução: (1) Tenta rawFileUrl se parecer OK (começa com http, tem path objeto razoável).
   *          (2) Senao, REMONTA do zero usando filePath + base publica padrao Supabase models-media.
   * Sempre adiciona bust-cache epochHour.
   */
  private _resolveSafeMediaUrl(filePath: string | undefined | null, rawFileUrl: string | undefined | null): string {
    // 🆕 PROJETO SUPABASE REAL DE PRODUÇÃO stmytwsdlonpnirqiufq (O ANTERIOR zmpqmdi ERA O ERRADO!)
    // Confirmado por 3 fontes: (1) index.html preconnect/dns-prefetch, (2) todos os testes Java do backend,
    // (3) URL do composite que o usuario enviou que FISICAMENTE continha o arquivo PDF 72ce4144-EVE-DUPPRE.pdf
    const SUPABASE_PUBLIC_BASE = 'https://stmytwsdlonpnirqiufq.supabase.co/storage/v1/object/public/models-media';
    const CORRECT_PROJECT_HOST = 'stmytwsdlonpnirqiufq.supabase.co';
    const CORRECT_BUCKET = 'models-media';
    const cleanPath = ((filePath || '') + '').trim().replace(/^\/+/, '');
    const urlRaw = (rawFileUrl || '').trim();

    let final: string = '';

    // Helper: detecta se URL parece INVALIDA (incompleta, ou PROJETO/BUCKET LEGADO ERRADOS
    const pareceInvalida = (u: string): boolean => {
      if (!u || u.length < 10) return true;
      if (!u.startsWith('http')) return true;
      try {
        const pu = new URL(u);
        // 🆕 INVERTIDO: STMYTWSD é o CERTO agora! zmpqmdi (projeto antigo vazio) = marcado invalido
        if (pu.hostname && pu.hostname !== CORRECT_PROJECT_HOST) {
          return true;
        }
        const path = pu.pathname || '';
        const matchBucket = path.match(/object\/public\/([^/]+)/);
        const bucketNaUrl = matchBucket ? matchBucket[1] : null;
        if (bucketNaUrl && bucketNaUrl !== CORRECT_BUCKET) {
          return true;
        }
      } catch (_e) {
        // URL quebrada no construtor = invalida
        return true;
      }
      // URL incompleta, termina so no bucket sem objeto
      if (/\/models-media\/?$/.test(u)) return true;
      // Sem extensao valida
      if (!/\.(pdf|jpe?g|png|webp|gif|heic|svg|mp4|mov|avif)(\?|$)/i.test(u)) return true;
      return false;
    };

    if (!pareceInvalida(urlRaw)) {
      final = urlRaw;
    } else {
      // FALLBACK 1: filePath do DTO
      // FALLBACK 2: se filePath NAO VEIO (ex: Eve Duppre), EXTRAI path RELATIVO da PROPRIA URL mesmo que ela fosse de projeto/bucket antigo/errado
      let relative = '';
      if (cleanPath) {
        relative = cleanPath.startsWith('models-media/')
          ? cleanPath.substring('models-media/'.length)
          : cleanPath;
      } else if (urlRaw) {
        // 🆕 EXTRAI path da PROPRIA URL (ex stmytwsd/site-assets/ zmpqmdi/...) e remonta no endereco CERTO
        try {
          const pu = new URL(urlRaw);
          const pathMatch = pu.pathname.match(/object\/public\/[^/]+\/(.+)$/);
          if (pathMatch && pathMatch[1]) {
            relative = pathMatch[1];
          } else {
            const fullPath = pu.pathname.split('/').filter(Boolean).join('/');
            const idx = fullPath.indexOf('models/');
            if (idx >= 0 && /\.(pdf|jpe?g|png|webp|heic)/i.test(fullPath)) {
              relative = fullPath.substring(idx);
            }
          }
        } catch (_e2) {
          // ignora, continua com relative vazio, usa urlRaw
        }
      }
      if (relative) {
        final = `${SUPABASE_PUBLIC_BASE}/${relative}`;
      } else {
        // Sem fallback de jeito nenhum: retorna raw mesmo que falhe
        final = urlRaw;
      }
    }

    // Anti-duplicação bucket models-media
    if (final && final.length > 0) {
      final = final.replace(/(object\/public\/)(models-media\/){2,}/g, '$1models-media/');
      final = final.replace(/(object\/public\/models-media\/)\/?models-media\//g, '$1');
      final = final.replace(/(?<!:)\/\/+/g, '/').replace('https:/', 'https://');
    }

    // 3) Bust cache 1h — 🆕 SEM PARAM V DUPLICADO (nunca mais ?v=A&v=B)
    if (final && final.length > 0) {
      let cleaned = final;
      if (cleaned.includes('v=')) {
        cleaned = cleaned
          .replace(/([?&])v=[^&]*(&|$)/g, (m, sep, end) => (end === '&' ? sep : ''))
          .replace(/[?&]$/, '');
      }
      const sep = cleaned.includes('?') ? '&' : '?';
      final = `${cleaned}${sep}v=${Math.floor(Date.now() / 3_600_000)}`;
    }
    return final;
  }

  /**
   * Aplica _resolveSafeMediaUrl em TODAS as midias do DTO de detalhe.
   * Resolve de uma vez: bookPhotos, polaroids, composite.
   */
  private _applyMediaFallbacksToDetail(m: ModelDetailPublicDto): ModelDetailPublicDto {
    if (!m) return m;
    const clone: any = { ...m };

    // 1) Book Photographs
    if (clone.bookPhotos && Array.isArray(clone.bookPhotos)) {
      clone.bookPhotos = clone.bookPhotos.map((p: any) => ({
        ...p,
        fileUrl: this._resolveSafeMediaUrl((p.filePath ?? p.storagePath), p.fileUrl)
      }));
    }
    // 2) Polaroids
    if (clone.polaroids && Array.isArray(clone.polaroids)) {
      clone.polaroids = clone.polaroids.map((p: any) => ({
        ...p,
        fileUrl: this._resolveSafeMediaUrl((p.filePath ?? p.storagePath), p.fileUrl)
      }));
    }
    // 3) Composite (OBJETO NOVO, tipo ModelCompositePublicDto)
    if (clone.composite && typeof clone.composite === 'object') {
      const c = clone.composite as any;
      clone.composite = {
        ...c,
        fileUrl: this._resolveSafeMediaUrl((c.filePath ?? c.storagePath), c.fileUrl)
      };
      // 🆕 CORRECAO CRITICA (caso Eve Duppre): compositeUrl JA EXISTIA com valor ERRADO (projeto/bucket legados).
      //    Antes era: `if (!clone.compositeUrl)` — ou seja, NUNCA atualizava quando campo tinha valor errado.
      //    Agora: SEMPRE atualiza campo legado compositeUrl para o valor RESOLVIDO correto.
      clone.compositeUrl = clone.composite.fileUrl;
    }
    // 4) Campo LEGADO compositeUrl solto: se ele existir mas composite.fileUrl nao, resolve tbm:
    if (clone.compositeUrl && (!clone.composite || !clone.composite.fileUrl)) {
      clone.compositeUrl = this._resolveSafeMediaUrl((clone.composite?.filePath ?? clone.composite?.storagePath), clone.compositeUrl);
    }
    // 5) Garante sempre que compositeUrl = valor MAIS NOVO (para componentes antigos que usam esse campo)
    if (clone.composite?.fileUrl && clone.compositeUrl !== clone.composite.fileUrl) {
      clone.compositeUrl = clone.composite.fileUrl;
    }

    return clone as ModelDetailPublicDto;
  }

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
   * Busca modelos em DESTAQUE DA VITRINE HOME (campo isFeaturedHome=true).
   * Endpoint: /public/models/featured
   * (NÃO confundir com isStar: Star = modelo destaque de carreira.
   *                isFeaturedHome = aparece ou não na VITRINE INICIAL)
   *
   * O backend retorna LIST<ModelCardPublicDto> DIRETA (sem paginação).
   * Mantemos compatibilidade PageResponseDto { content } para os componentes.
   * Fallback 1: se /featured vazio, pega os STARS (isStar=true).
   * Fallback 2: falha de rede, usa catalogo estático mock.
   */
  getFeaturedModels(limit: number = 8): Observable<PageResponseDto<ModelCardPublicDto>> {
    return this.api.get<ModelCardPublicDto[]>('/public/models/featured').pipe(
      switchMap(featuredArray => {
        const items = this._applyCoverFallbacks((featuredArray || [])).slice(0, limit);
        if (items.length > 0) {
          return of({
            content: items,
            pageNumber: 0,
            pageSize: limit,
            totalElements: items.length,
            totalPages: 1,
            isLast: true
          } as PageResponseDto<ModelCardPublicDto>);
        }
        // Fallback 1: nenhum marcado como Featured Home → utiliza STARS (isStar=true)
        return this.api.get<PageResponseDto<ModelCardPublicDto>>('/public/models', {
          isStar: true,
          size: limit,
          page: 0
        }).pipe(map(p => this._applyCoverFallbacksPage(p)));
      }),
      catchError(err => {
        console.warn('[public-model] Falha em /public/models/featured, utilizando fallback isStar=true e/ou mock:', err);
        // Fallback 2: tenta catalogo /models?isStar=true, depois mock.
        return this.api.get<PageResponseDto<ModelCardPublicDto>>('/public/models', { isStar: true, size: limit, page: 0 }).pipe(
          map(p => this._applyCoverFallbacksPage(p)),
          catchError(err2 => {
            console.warn('[public-model] /public/models também falhou, utilizando catálogo estático mock:', err2);
            return of(this._applyCoverFallbacksPage(this.getMockPageResponse({ isStar: true, size: limit, page: 0 })));
          })
        );
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
      map(p => this._applyCoverFallbacksPage(p)),
      catchError(err => {
        console.warn('Falha ao buscar casting da API pública, utilizando catálogo estático de fallback:', err);
        return of(this._applyCoverFallbacksPage(this.getMockPageResponse(params)));
      })
    );
  }

  /**
   * Consulta os detalhes completos de um modelo ativo pelo ID (UUID).
   * Em caso de falha na requisição, localiza no catálogo mock ou retorna o primeiro modelo.
   *
   * 🔥 CORREÇÃO: Aplica _applyMediaFallbacksToDetail em bookPhotos / polaroids / COMPOSITE
   * antes de entregar os dados ao componente. NUNCA MAIS:
   *  - Ver Composite VAZIO (iframe com URL incompleta → 400 Supabase)
   *  - Download Sedcard / Composite → 404 Not Found
   */
  getModelById(id: string): Observable<ModelDetailPublicDto> {
    return this.api.get<ModelDetailPublicDto>(`/public/models/${id}`).pipe(
      map(rawDto => this._applyMediaFallbacksToDetail(rawDto)),
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

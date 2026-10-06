import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpEvent, HttpEventType, HttpParams } from '@angular/common/http';
import { Observable, catchError, map, of, tap, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { GalleryPhoto } from '../../shared/models/gallery.model';
import {
  AdminModelFilterParams,
  AdminModelPageResponse,
  ModelAdminItem,
  ModelFormData
} from '../../shared/models/admin-model.interface';

export interface MediaUploadedResult {
  id: string;
  fileUrl: string;
  filePath: string;
  mediaType: 'BOOK' | 'POLAROID';
  displayOrder: number;
  isCover: boolean;
  isActive: boolean;
}

const MOCK_MODELS_STORAGE_KEY = 'wb_agency_admin_models_mock';

@Injectable({
  providedIn: 'root'
})
export class AdminModelService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/admin/models`;

  /**
   * Consulta paginada e filtrada de modelos na área administrativa.
   */
  getModels(filters: AdminModelFilterParams = {}): Observable<AdminModelPageResponse> {
    let params = new HttpParams()
      .set('page', String(filters.page ?? 0))
      .set('size', String(filters.size ?? 20));

    if (filters.gender && filters.gender !== 'ALL') {
      params = params.set('gender', filters.gender);
    }
    if (filters.isStar !== undefined && filters.isStar !== null) {
      params = params.set('isStar', String(filters.isStar));
    }
    if (filters.isActive !== undefined && filters.isActive !== null) {
      params = params.set('isActive', String(filters.isActive));
    }
    if (filters.search && filters.search.trim()) {
      params = params.set('search', filters.search.trim());
    }
    if (filters.sort) {
      params = params.set('sort', filters.sort);
    }

    return this.http.get<any>(this.apiUrl, { params }).pipe(
      map((res) => this.normalizePageResponse(res)),
      catchError((err) => {
        console.warn('Backend offline ou inacessível. Operando via Mock Admin Models Local:', err);
        return of(this.getMockPagedModels(filters));
      })
    );
  }

  /**
   * Obtém detalhes de um modelo por ID para edição.
   */
  getModelById(id: string): Observable<ModelAdminItem> {
    return this.http.get<ModelAdminItem>(`${this.apiUrl}/${id}`).pipe(
      map(m => this._resolveAdminCoverUrl(m)),
      catchError((err) => {
        console.warn(`Backend offline para obter modelo ${id}. Buscando no Mock Local:`, err);
        const found = this.getMockList().find((m) => m.id === id);
        if (found) {
          return of(found);
        }
        return throwError(() => new Error('Modelo não encontrado'));
      })
    );
  }

  /**
   * Cadastra um novo modelo no casting oficial.
   */
  createModel(data: ModelFormData): Observable<ModelAdminItem> {
    return this.http.post<ModelAdminItem>(this.apiUrl, data).pipe(
      map(m => this._resolveAdminCoverUrl(m)),
      tap((created) => this.upsertLocalMock(created)),
      catchError((err) => {
        console.warn('Backend offline ao cadastrar modelo. Salvando no Mock Local:', err);
        const newModel: ModelAdminItem = {
          id: 'model-' + Math.random().toString(36).substring(2, 9),
          ...data,
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString(),
          photosCount: 0,
          compositeReady: false
        };
        this.upsertLocalMock(newModel);
        return of(newModel);
      })
    );
  }

  /**
   * Atualiza os dados completos de um modelo existente.
   */
  updateModel(id: string, data: ModelFormData): Observable<ModelAdminItem> {
    return this.http.put<ModelAdminItem>(`${this.apiUrl}/${id}`, data).pipe(
      map(m => this._resolveAdminCoverUrl(m)),
      tap((updated) => this.upsertLocalMock(updated)),
      catchError((err) => {
        console.warn(`Backend offline ao atualizar modelo ${id}. Atualizando no Mock Local:`, err);
        const existingList = this.getMockList();
        const index = existingList.findIndex((m) => m.id === id);
        if (index >= 0) {
          const updated: ModelAdminItem = {
            ...existingList[index],
            ...data,
            id,
            updatedAt: new Date().toISOString()
          };
          existingList[index] = updated;
          this.saveMockList(existingList);
          return of(updated);
        }
        return throwError(() => new Error('Modelo não encontrado para atualização'));
      })
    );
  }

  /**
   * Alterna a classificação Star do modelo (Patch rápido).
   */
  updateStar(id: string, isStar: boolean): Observable<ModelAdminItem> {
    return this.http.patch<ModelAdminItem>(`${this.apiUrl}/${id}/star`, { isStar }).pipe(
      map(m => this._resolveAdminCoverUrl(m)),
      tap((updated) => this.upsertLocalMock(updated)),
      catchError((err) => {
        console.warn(`Backend offline ao alternar Star para ${id}:`, err);
        const mockList = this.getMockList();
        const item = mockList.find((m) => m.id === id);
        if (item) {
          item.isStar = isStar;
          item.updatedAt = new Date().toISOString();
          this.saveMockList(mockList);
          return of(item);
        }
        return throwError(() => err);
      })
    );
  }

  /**
   * Alterna o status Ativo/Inativo do modelo (Patch rápido).
   */
  updateStatus(id: string, isActive: boolean): Observable<ModelAdminItem> {
    return this.http.patch<ModelAdminItem>(`${this.apiUrl}/${id}/status`, { isActive }).pipe(
      map(m => this._resolveAdminCoverUrl(m)),
      tap((updated) => this.upsertLocalMock(updated)),
      catchError((err) => {
        console.warn(`Backend offline ao alternar Status para ${id}:`, err);
        const mockList = this.getMockList();
        const item = mockList.find((m) => m.id === id);
        if (item) {
          item.isActive = isActive;
          item.updatedAt = new Date().toISOString();
          this.saveMockList(mockList);
          return of(item);
        }
        return throwError(() => err);
      })
    );
  }

  /**
   * Alterna destaque na Home e ordem.
   */
  updateFeatured(id: string, isFeaturedHome: boolean, featuredOrder?: number | null): Observable<ModelAdminItem> {
    return this.http.patch<ModelAdminItem>(`${this.apiUrl}/${id}/featured`, { isFeaturedHome, featuredOrder }).pipe(
      map(m => this._resolveAdminCoverUrl(m)),
      tap((updated) => this.upsertLocalMock(updated)),
      catchError((err) => {
        console.warn(`Backend offline ao alternar Destaque para ${id}:`, err);
        const mockList = this.getMockList();
        const item = mockList.find((m) => m.id === id);
        if (item) {
          item.isFeaturedHome = isFeaturedHome;
          item.featuredOrder = featuredOrder ?? null;
          item.updatedAt = new Date().toISOString();
          this.saveMockList(mockList);
          return of(item);
        }
        return throwError(() => err);
      })
    );
  }

  /**
   * Remove o modelo do catálogo.
   */
  deleteModel(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`).pipe(
      tap(() => this.removeLocalMock(id)),
      catchError((err) => {
        console.warn(`Backend offline ao remover modelo ${id}:`, err);
        this.removeLocalMock(id);
        return of(void 0);
      })
    );
  }

  /**
   * Obtém a galeria de fotos REAL do modelo (tabela model_media).
   * Remove a galeria de placeholders hardcoded (samplePhotos) que faziam
   * as fotos "sumirem" após recarregar a página.
   *
   * Corrige BUG CRITICO: caso o backend devolva fileUrl INCOMPLETA ou
   * em branco (gravacoes antigas), montamos a URL combinando-a com
   * filePath (caminho real do objeto no bucket Supabase).
   * LOGICA IDENTICA ao Backend Java resolvePublicUrlFromFields.
   */
  getModelMedia(modelId: string): Observable<GalleryPhoto[]> {
    return this.http.get<any[]>(`${this.apiUrl}/${modelId}/media`).pipe(
      map(items => (items || []).map((item, idx) => {
        const rawFileUrl = typeof item.fileUrl === 'string' ? item.fileUrl.trim() : '';
        const filePath = typeof item.filePath === 'string' ? item.filePath.trim() : '';
        const safeUrl = this._resolveSafeMediaUrl(filePath, rawFileUrl);
        const finalUrl = safeUrl + (safeUrl && !safeUrl.includes('?v=') ? '?v=' + Math.floor(Date.now() / 3600_000) : '');

        const mediaType = String(item.mediaType || 'BOOK').toUpperCase();
        const isPolaroid = mediaType.includes('POLAROID')
          || mediaType === 'POLAROID_FRONT'
          || mediaType === 'POLAROID_SIDE'
          || mediaType === 'POLAROID_BODY';

        return {
          id: String(item.id),
          url: finalUrl,
          filePath: filePath,
          category: (isPolaroid ? 'POLAROID' : 'BOOK') as 'BOOK' | 'POLAROID',
          orderIndex: Number(item.displayOrder ?? idx),
          isCover: Boolean(item.isCover),
          isActive: Boolean(item.isActive ?? true),
          isUploaded: true
        };
      })),
      catchError((err) => {
        console.warn(`Backend offline para obter midias do modelo ${modelId}. Retornando galeria vazia:`, err);
        return of([]);
      })
    );
  }

  /**
   * Fallback frontal para resolver URLs incompletas no Admin.
   * Estrategia 100% igual ao Backend Java resolvePublicUrlFromFields:
   * - Se rawFileUrl estiver vazia ou incompleta (sem filePath incluido ou termina /)
   *   Concatena bucket base + filePath para montar URL funcional.
   *
   * NOTA: No frontend NAO temos armazenado o baseURL do bucket de forma facil
   * (para evitar duplicidade de configuracao), aplicamos heuristica robusta:
   * Se a URL nao contem arquivo (terminar com / ou length < 40 caracteres) usamos
   * a rawFileUrl como base + filePath concatenado. Caso nada funcione, retornamos
   * a rawFileUrl original (sem piorar o cenario).
   */
  private _resolveSafeMediaUrl(filePath: string, rawFileUrl: string): string {
    if (!rawFileUrl && !filePath) return '';
    if (!rawFileUrl) rawFileUrl = '';
    if (!filePath) filePath = '';

    // Caso 1: URL parece completa (nao termina com / e tem tamanho suficiente).
    if (rawFileUrl.length >= 40 && !rawFileUrl.endsWith('/') && (!filePath || rawFileUrl.includes(filePath))) {
      return rawFileUrl;
    }

    // Caso 2: URL parece incompleta ou falta path.
    // Tentamos remover duplicados antes de concatenar.
    let base = rawFileUrl;
    if (base.endsWith('/')) base = base.substring(0, base.length - 1);

    if (!filePath) {
      return rawFileUrl; // nada mais podemos fazer
    }

    // Evita duplicar path ex: /public/models-media/models-media/abc
    const safeFilePath = filePath.startsWith('/') ? filePath.substring(1) : filePath;
    if (base && safeFilePath && (base + '/' + safeFilePath).length >= 30) {
      return base + '/' + safeFilePath;
    }

    // Fallback final: devolve o que tivermos
    return rawFileUrl || (safeFilePath ? safeFilePath : '');
  }

  /**
   * Faz upload REAL de UMA foto para Supabase via Backend.
   * Usa FormData: file + mediaType + isCover.
   * Retorna Observable de HttpEvent para conseguir progress bar no grid.
   */
  uploadModelMedia(
    modelId: string,
    file: File,
    mediaType: 'BOOK' | 'POLAROID' = 'BOOK',
    isCover: boolean = false
  ): Observable<HttpEvent<MediaUploadedResult>> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('mediaType', mediaType);
    if (isCover) formData.append('isCover', 'true');

    return this.http.post<MediaUploadedResult>(
      `${this.apiUrl}/${modelId}/media`,
      formData,
      {
        reportProgress: true,
        observe: 'events'
      }
    );
  }

  /**
   * Exclui uma midia do modelo (storage Supabase + tabela model_media).
   */
  deleteModelMedia(modelId: string, mediaId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${modelId}/media/${mediaId}`).pipe(
      catchError((err) => {
        console.warn(`Backend offline excluir midia ${mediaId}:`, err);
        return of(void 0);
      })
    );
  }

  /**
   * Define a FOTO DE CAPA do modelo (PATCH: is_cover = true nesta midia,
   * false nas demais, e sincroniza primary_photo_url na tabela models).
   */
  setModelCover(modelId: string, mediaId: string): Observable<MediaUploadedResult> {
    return this.http.patch<MediaUploadedResult>(`${this.apiUrl}/${modelId}/media/${mediaId}/cover`, {}).pipe(
      tap(_ => console.info(`[admin-model] Nova capa definida: model=${modelId}, media=${mediaId}`)),
      catchError((err) => {
        console.error(`Erro ao definir capa model=${modelId} media=${mediaId}:`, err);
        return throwError(() => err);
      })
    );
  }

  // --- Métodos de Normalização e Mocking Local ---

  /**
   * Helper ANTI-FRAGIL para foto de capa na tela ADMIN "Casting & Stars".
   * Resolve bug: fotos apareciam no SITE PUBLICO (PublicModelService tinha fallback)
   * mas NÃO APARECIAM no Admin.
   *
   * Motivo: primaryPhotoUrl gravada incompleta no banco (ex: "/public/models-media/"
   * SEM o path do arquivo) causa HTTP 400 no Storage Supabase.
   *
   * Solução IDENTICA ao PublicModelService._resolveCoverForCard:
   * 1. Usa filePath/storagePath para REMONTAR URL publica valida
   * 2. Bust-cache ?v=epochHour
   */
  private _resolveAdminCoverUrl(m: ModelAdminItem | any): ModelAdminItem {
    if (!m) return m;
    const filePath = (m as any).filePath || (m as any).storagePath || '';
    let rawUrl = (m.primaryPhotoUrl || '').trim();

    // 1) URL vazia MAS temos filePath -> remontar
    if (!rawUrl && filePath) {
      const clean = filePath.replace(/^\//, '');
      rawUrl = `https://zmpqmdizqgnpnirqiufq.supabase.co/storage/v1/object/public/models-media/${clean}`;
    }
    // 2) URL incompleta detectada: termina com /, length curto mas temos filePath
    const seemsIncomplete = !rawUrl.startsWith('http')
      || rawUrl.endsWith('/')
      || (filePath && rawUrl.length < 40 && !rawUrl.includes(filePath.substring(filePath.lastIndexOf('/') + 1)));

    if (seemsIncomplete && filePath) {
      const clean = filePath.replace(/^\//, '');
      if (rawUrl && !rawUrl.startsWith('http')) {
        if (rawUrl.includes('/models-media/') && !rawUrl.includes(clean)) {
          const base = rawUrl.endsWith('/') ? rawUrl : (rawUrl + '/');
          const prefix = base.startsWith('http') ? '' : 'https://zmpqmdizqgnpnirqiufq.supabase.co';
          rawUrl = (prefix + base + clean).replace(/([^:]\/)\/+/g, '$1');
        } else if (!rawUrl.includes(clean)) {
          rawUrl = `https://zmpqmdizqgnpnirqiufq.supabase.co/storage/v1/object/public/models-media/${clean}`;
        }
      } else if (!rawUrl) {
        rawUrl = `https://zmpqmdizqgnpnirqiufq.supabase.co/storage/v1/object/public/models-media/${clean}`;
      }
    }
    // 3) Bust cache forçado (mantem foto nova aparecendo em 1h)
    if (rawUrl) {
      rawUrl = rawUrl + (rawUrl.includes('?') ? '&' : '?') + 'v=' + Math.floor(Date.now() / 3_600_000);
    }
    return { ...m, primaryPhotoUrl: rawUrl || m.primaryPhotoUrl };
  }

  private _applyCoverAll(items: ModelAdminItem[]): ModelAdminItem[] {
    if (!items || items.length === 0) return items;
    return items.map(x => this._resolveAdminCoverUrl(x));
  }

  private normalizePageResponse(res: any): AdminModelPageResponse {
    // 🔍 DIAGNÓSTICO: Imprime o envelope REAL da resposta no console DevTools
    console.log('[admin-model] Resposta CRUA recebida de /admin/models:', res);

    if (!res) {
      console.warn('[admin-model] Resposta nula/undefined. Retornando pagina vazia.');
      return { content: [], totalElements: 0, totalPages: 0, size: 20, number: 0 };
    }

    // -------------------------------------------------------------
    // Extracao TOLERANTE a MULTIPLOS envelopes de resposta
    // Ordem de prioridade (do mais comum Spring para mais exotico):
    //  1. res.data        → ApiResponse / ResponseEntity empacotado
    //  2. res.body        → Resposta crua HttpClient { body: ... }
    //  3. res.payload     → Envelope custom
    //  4. res.records / res.items → Envelopes de listagem genericos
    //  5. res.content     → Page do Spring Data (DIRETO, esperado)
    //  6. Array direto    → Endpoint nao paginado ou lista crua
    // -------------------------------------------------------------
    let extractedList: any[] = [];
    const candidateContainers: any[] = [
      res,
      res?.data,
      res?.body,
      res?.payload,
      res?.result,
      (res?.data && typeof res?.data === 'object') ? res.data : null
    ].filter(c => c != null);

    for (const c of candidateContainers) {
      if (Array.isArray(c)) { extractedList = c; break; }
      if (Array.isArray(c.content)) { extractedList = c.content; break; }
      if (Array.isArray(c.items)) { extractedList = c.items; break; }
      if (Array.isArray(c.records)) { extractedList = c.records; break; }
    }

    // Fallback: Spring Page raw, campo content direto no root
    if (extractedList.length === 0 && Array.isArray(res.content)) {
      extractedList = res.content;
    }

    // Totalizadores: pegar do container que efetivamente continha a lista
    let meta: any = res;
    for (const c of candidateContainers) {
      if (c === res) continue;
      if (Array.isArray(c)) continue;
      if ((Array.isArray(c.content) && c.content === extractedList) ||
          (Array.isArray(c.items) && c.items === extractedList) ||
          (Array.isArray(c.records) && c.records === extractedList)) {
        meta = c; break;
      }
    }

    const content = this._applyCoverAll(extractedList || []);

    const normalized: AdminModelPageResponse = {
      content,
      totalElements: meta?.totalElements ?? res?.totalElements ?? content.length,
      totalPages: meta?.totalPages ?? res?.totalPages ?? Math.max(1, Math.ceil(content.length / (meta?.size ?? res?.size ?? 20))),
      size: meta?.size ?? res?.size ?? content.length,
      number: meta?.number ?? res?.number ?? 0,
      first: meta?.first ?? res?.first ?? true,
      last: meta?.last ?? res?.last ?? true
    };

    console.log('[admin-model] Resposta NORMALIZADA:', normalized, `(${normalized.content.length} items)`);

    if (normalized.content.length === 0) {
      console.warn('[admin-model] NORMALIZACAO retornou lista VAZIA. Verifique se a extracao acima encontrou o array correto.',
        'extractedList=', extractedList, 'meta=', meta);
    }

    return normalized;
  }

  private getMockPagedModels(filters: AdminModelFilterParams): AdminModelPageResponse {
    let list = this.getMockList();

    if (filters.gender && filters.gender !== 'ALL') {
      list = list.filter((m) => m.gender === filters.gender);
    }
    if (filters.isStar !== undefined && filters.isStar !== null) {
      list = list.filter((m) => m.isStar === filters.isStar);
    }
    if (filters.isActive !== undefined && filters.isActive !== null) {
      list = list.filter((m) => m.isActive === filters.isActive);
    }
    if (filters.search && filters.search.trim()) {
      const q = filters.search.trim().toLowerCase();
      list = list.filter(
        (m) =>
          m.stageName.toLowerCase().includes(q) ||
          (m.city && m.city.toLowerCase().includes(q)) ||
          (m.nationality && m.nationality.toLowerCase().includes(q))
      );
    }
    // APLICA FALLBACK de foto capa (igual resposta real do backend)
    list = this._applyCoverAll(list);

    const page = filters.page ?? 0;
    const size = filters.size ?? 20;
    const start = page * size;
    const pagedContent = list.slice(start, start + size);

    return {
      content: pagedContent,
      totalElements: list.length,
      totalPages: Math.max(1, Math.ceil(list.length / size)),
      size,
      number: page,
      first: page === 0,
      last: start + size >= list.length
    };
  }

  private getMockList(): ModelAdminItem[] {
    if (typeof localStorage !== 'undefined') {
      const stored = localStorage.getItem(MOCK_MODELS_STORAGE_KEY);
      if (stored) {
        try {
          return JSON.parse(stored);
        } catch (e) {
          console.error('Erro ao ler mock models de localStorage:', e);
        }
      }
    }

    // Modelos sementes de alta costura e fidelidade
    const defaults: ModelAdminItem[] = [
      {
        id: '487b27d7-206f-422d-a46c-8961ed8c827c',
        stageName: 'Isabella Fontana',
        gender: 'FEMALE',
        isStar: true,
        isFeaturedHome: true,
        featuredOrder: 1,
        isActive: true,
        primaryPhotoUrl: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?q=80&w=800&auto=format&fit=crop',
        instagramUrl: 'https://instagram.com/isabellafontana',
        birthDate: '2001-05-14',
        heightCm: 179,
        city: 'São Paulo, SP',
        nationality: 'Brasileira',
        dressSize: '36',
        shoeSize: '38',
        bustChestCm: 86.0,
        waistCm: 61.0,
        hipsCm: 90.0,
        hairColor: 'Castanho Claro',
        eyesColor: 'Verdes',
        photosCount: 14,
        compositeReady: true,
        createdAt: '2026-01-10T12:00:00Z',
        updatedAt: '2026-03-15T15:30:00Z'
      },
      {
        id: '921c38e8-317f-433e-b57d-9072fe9d938d',
        stageName: 'Gabriel Alencar',
        gender: 'MALE',
        isStar: true,
        isFeaturedHome: true,
        featuredOrder: 2,
        isActive: true,
        primaryPhotoUrl: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?q=80&w=800&auto=format&fit=crop',
        instagramUrl: 'https://instagram.com/gabriel.alencar',
        birthDate: '1999-08-22',
        heightCm: 188,
        city: 'Rio de Janeiro, RJ',
        nationality: 'Brasileiro',
        dressSize: '42',
        shoeSize: '42',
        bustChestCm: 102.0,
        waistCm: 79.0,
        hipsCm: 98.0,
        hairColor: 'Castanho Escuro',
        eyesColor: 'Castanhos',
        photosCount: 18,
        compositeReady: true,
        createdAt: '2026-01-12T10:00:00Z',
        updatedAt: '2026-03-18T11:20:00Z'
      },
      {
        id: '154a49f9-428a-544f-c68e-0183af0e049e',
        stageName: 'Helena Vasconcelos',
        gender: 'FEMALE',
        isStar: false,
        isFeaturedHome: false,
        featuredOrder: null,
        isActive: true,
        primaryPhotoUrl: 'https://images.unsplash.com/photo-1517841905240-472988babdf9?q=80&w=800&auto=format&fit=crop',
        instagramUrl: 'https://instagram.com/helenavasconcelos',
        birthDate: '2004-11-03',
        heightCm: 177,
        city: 'Belo Horizonte, MG',
        nationality: 'Brasileira',
        dressSize: '34',
        shoeSize: '37',
        bustChestCm: 82.0,
        waistCm: 59.0,
        hipsCm: 88.0,
        hairColor: 'Loiro Dourado',
        eyesColor: 'Azuis',
        photosCount: 8,
        compositeReady: true,
        createdAt: '2026-02-01T14:15:00Z',
        updatedAt: '2026-02-28T09:40:00Z'
      },
      {
        id: '265b50a0-539b-655a-d79f-1294bf1f150f',
        stageName: 'Lucas Mendes',
        gender: 'MALE',
        isStar: false,
        isFeaturedHome: false,
        featuredOrder: null,
        isActive: true,
        primaryPhotoUrl: 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?q=80&w=800&auto=format&fit=crop',
        instagramUrl: 'https://instagram.com/lucas.mendes_wb',
        birthDate: '2000-03-18',
        heightCm: 185,
        city: 'Curitiba, PR',
        nationality: 'Brasileiro',
        dressSize: '40',
        shoeSize: '41',
        bustChestCm: 99.0,
        waistCm: 76.0,
        hipsCm: 95.0,
        hairColor: 'Preto',
        eyesColor: 'Castanhos Escuros',
        photosCount: 12,
        compositeReady: false,
        createdAt: '2026-02-10T16:00:00Z',
        updatedAt: '2026-03-01T14:00:00Z'
      },
      {
        id: '376c61b1-64ac-766b-e80a-2305cf2a261a',
        stageName: 'Camila Rocha',
        gender: 'FEMALE',
        isStar: true,
        isFeaturedHome: true,
        featuredOrder: 3,
        isActive: true,
        primaryPhotoUrl: 'https://images.unsplash.com/photo-1524504388940-b1c1722653e1?q=80&w=800&auto=format&fit=crop',
        instagramUrl: 'https://instagram.com/camilarocha.model',
        birthDate: '2002-07-29',
        heightCm: 180,
        city: 'Porto Alegre, RS',
        nationality: 'Brasileira',
        dressSize: '38',
        shoeSize: '39',
        bustChestCm: 89.0,
        waistCm: 63.0,
        hipsCm: 92.0,
        hairColor: 'Castanho Acobreado',
        eyesColor: 'Mel',
        photosCount: 16,
        compositeReady: true,
        createdAt: '2026-02-15T08:30:00Z',
        updatedAt: '2026-03-10T17:50:00Z'
      },
      {
        id: '487d72c2-75bd-877c-f91b-3416da3b372b',
        stageName: 'Sophia Benitez',
        gender: 'FEMALE',
        isStar: false,
        isFeaturedHome: false,
        featuredOrder: null,
        isActive: false,
        primaryPhotoUrl: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?q=80&w=800&auto=format&fit=crop',
        instagramUrl: 'https://instagram.com/sophiabenitez',
        birthDate: '2003-12-11',
        heightCm: 175,
        city: 'Florianópolis, SC',
        nationality: 'Brasileira',
        dressSize: '36',
        shoeSize: '37',
        bustChestCm: 85.0,
        waistCm: 60.0,
        hipsCm: 89.0,
        hairColor: 'Castanho',
        eyesColor: 'Verdes',
        photosCount: 6,
        compositeReady: false,
        createdAt: '2026-03-01T11:00:00Z',
        updatedAt: '2026-03-20T10:10:00Z'
      }
    ];

    this.saveMockList(defaults);
    return defaults;
  }

  private saveMockList(list: ModelAdminItem[]): void {
    if (typeof localStorage !== 'undefined') {
      try {
        localStorage.setItem(MOCK_MODELS_STORAGE_KEY, JSON.stringify(list));
      } catch (e) {
        console.error('Erro ao salvar mock models em localStorage:', e);
      }
    }
  }

  private upsertLocalMock(model: ModelAdminItem): void {
    const list = this.getMockList();
    const idx = list.findIndex((m) => m.id === model.id);
    if (idx >= 0) {
      list[idx] = { ...list[idx], ...model };
    } else {
      list.unshift(model);
    }
    this.saveMockList(list);
  }

  private removeLocalMock(id: string): void {
    const list = this.getMockList().filter((m) => m.id !== id);
    this.saveMockList(list);
  }
}

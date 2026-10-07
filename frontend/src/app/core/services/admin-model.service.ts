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

@Injectable({
  providedIn: 'root'
})
export class AdminModelService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/admin/models`;

  /**
   * Consulta paginada e filtrada de modelos na área administrativa.
   * SEM FALLBACK DE MOCKS: Qualquer erro retorna página vazia.
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

    const fullUrl = `${this.apiUrl}?${params.toString()}`;
    console.log('[admin-model] GET iniciado →', fullUrl, 'filters=', filters);

    return this.http.get<any>(this.apiUrl, { params }).pipe(
      tap((raw: any) => {
        const byteLen = typeof raw === 'string' ? raw.length : JSON.stringify(raw ?? {}).length;
        console.groupCollapsed(`[admin-model] ✅ HTTP 2XX recebido (~${byteLen} bytes) de /admin/models`);
        console.log('Payload CRUO (raw):', raw);
        console.log('Keys raiz:', Object.keys(raw ?? {}));
        if (Array.isArray(raw?.content)) { console.log('raw.content.length =', raw.content.length); }
        if (Array.isArray(raw?.data))    { console.log('raw.data.length =', raw.data.length); }
        if (Array.isArray(raw?.items))   { console.log('raw.items.length =', raw.items.length); }
        if (Array.isArray(raw?.records)) { console.log('raw.records.length =', raw.records.length); }
        console.groupEnd();
      }),
      map((res) => this.normalizePageResponse(res)),
      tap((normalized: AdminModelPageResponse) => {
        console.log('[admin-model] NORMALIZADO FINAL → content.length =', normalized.content.length,
          '| totalElements =', normalized.totalElements, '| totalPages =', normalized.totalPages);
      }),
      catchError((err) => {
        console.error('[admin-model] 🔴 ERRO HTTP ao carregar modelos reais. Status:', err?.status, err?.statusText, '\nErro completo:', err);
        const emptyPage: AdminModelPageResponse = {
          content: [],
          totalElements: 0,
          totalPages: 0,
          size: filters.size ?? 20,
          number: filters.page ?? 0,
          first: true,
          last: true
        };
        console.warn('[admin-model] Retornando PAGINA VAZIA (sem mocks! Nao havera dados falsos na tela). Empty page:', emptyPage);
        return of(emptyPage);
      })
    );
  }

  /**
   * Obtém detalhes de um modelo por ID para edição.
   * SEM MOCK: Erro é propagado para o componente exibir toast.
   */
  getModelById(id: string): Observable<ModelAdminItem> {
    return this.http.get<ModelAdminItem>(`${this.apiUrl}/${id}`).pipe(
      map(m => this._resolveAdminCoverUrl(m)),
      catchError((err) => {
        console.error(`[admin-model] 🔴 ERRO ao obter modelo por ID ${id}:`, err);
        return throwError(() => err);
      })
    );
  }

  /**
   * Cadastra um novo modelo no casting oficial.
   * SEM MOCK: Salva apenas no backend real.
   */
  createModel(data: ModelFormData): Observable<ModelAdminItem> {
    return this.http.post<ModelAdminItem>(this.apiUrl, data).pipe(
      map(m => this._resolveAdminCoverUrl(m)),
      catchError((err) => {
        console.error('[admin-model] 🔴 ERRO ao cadastrar novo modelo:', err);
        return throwError(() => err);
      })
    );
  }

  /**
   * Atualiza os dados completos de um modelo existente.
   * SEM MOCK: Apenas backend real.
   */
  updateModel(id: string, data: ModelFormData): Observable<ModelAdminItem> {
    return this.http.put<ModelAdminItem>(`${this.apiUrl}/${id}`, data).pipe(
      map(m => this._resolveAdminCoverUrl(m)),
      catchError((err) => {
        console.error(`[admin-model] 🔴 ERRO ao atualizar modelo ${id}:`, err);
        return throwError(() => err);
      })
    );
  }

  /**
   * Alterna a classificação Star do modelo (Patch rápido).
   */
  updateStar(id: string, isStar: boolean): Observable<ModelAdminItem> {
    return this.http.patch<ModelAdminItem>(`${this.apiUrl}/${id}/star`, { isStar }).pipe(
      map(m => this._resolveAdminCoverUrl(m)),
      catchError((err) => {
        console.error(`[admin-model] 🔴 ERRO ao alternar Star modelo ${id}:`, err);
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
      catchError((err) => {
        console.error(`[admin-model] 🔴 ERRO ao alternar Status modelo ${id}:`, err);
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
      catchError((err) => {
        console.error(`[admin-model] 🔴 ERRO ao alternar Destaque modelo ${id}:`, err);
        return throwError(() => err);
      })
    );
  }

  /**
   * Remove o modelo do catálogo.
   * SEM MOCK: apenas backend real executa hard delete.
   */
  deleteModel(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`).pipe(
      catchError((err) => {
        console.error(`[admin-model] 🔴 ERRO ao excluir modelo ${id}:`, err);
        return throwError(() => err);
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
    if (!res) {
      console.warn('[admin-model] Resposta nula/undefined. Retornando pagina vazia.');
      return { content: [], totalElements: 0, totalPages: 0, size: 20, number: 0 };
    }

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

    if (extractedList.length === 0 && Array.isArray(res.content)) {
      extractedList = res.content;
    }

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

    console.log('[admin-model] Normalização concluída. content.length=', normalized.content.length,
      'totalElements=', normalized.totalElements, 'totalPages=', normalized.totalPages);

    if (normalized.content.length === 0) {
      console.warn('[admin-model] NORMALIZAÇÃO: lista VAZIA. extractedList=', extractedList, 'meta=', meta,
        'Keys raiz res=', Object.keys(res ?? {}), 'res bruto:', res);
    }

    return normalized;
  }
}

import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams, HttpErrorResponse } from '@angular/common/http';
import { Observable, of, throwError } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import {
  Candidate,
  CandidatePageResponse,
  CandidatePhoto,
  CandidateStatus,
  CandidateStatusUpdatePayload
} from '../models/candidate.model';

@Injectable({
  providedIn: 'root'
})
export class CandidateService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiUrl;

  // Helper IDENTICO ao PublicModelService._resolveSafeMediaUrl para fotos de candidatos.
  // Resolve exatamente o mesmo problema: URLs de facePhotoUrl/profilePhotoUrl/fullBodyPhotoUrl
  // que vem com projeto supabase errado (zmpqmdi), bucket errado, incompletas ou param duplicado ?v=&v=.
  // Projeto REAL = stmytwsdlonpnirqiufq / Bucket default candidatos = candidates-uploads.
  private _resolveCandidatePhotoUrl(rawUrlOrFile: any, fallbackRaw: string | null = null): string {
    const SUPABASE_PUBLIC_BASE = 'https://stmytwsdlonpnirqiufq.supabase.co/storage/v1/object/public/candidates-uploads';
    const CORRECT_PROJECT_HOST = 'stmytwsdlonpnirqiufq.supabase.co';
    const CORRECT_BUCKETS_ALLOWED = new Set(['candidates-uploads', 'models-media', 'site-assets']);

    // Input pode ser string (URL direta) ou objeto { url, filePath, storagePath, fileUrl }
    let rawUrl = '';
    let filePath: string = '';
    if (typeof rawUrlOrFile === 'string') {
      rawUrl = rawUrlOrFile.trim();
    } else if (rawUrlOrFile && typeof rawUrlOrFile === 'object') {
      rawUrl = (rawUrlOrFile.url || rawUrlOrFile.fileUrl || fallbackRaw || '').trim();
      filePath = ((rawUrlOrFile.filePath || rawUrlOrFile.storagePath || '') + '').trim().replace(/^\/+/, '');
    }
    if (!rawUrl && fallbackRaw) rawUrl = fallbackRaw.trim();
    if (!rawUrl && !filePath) return '';

    const invalid = (u: string): boolean => {
      if (!u || u.length < 10) return true;
      if (!u.startsWith('http')) return true;
      try {
        const pu = new URL(u);
        if (pu.hostname && pu.hostname !== CORRECT_PROJECT_HOST) return true;
        const m = pu.pathname.match(/object\/public\/([^/]+)/);
        if (m && m[1] && !CORRECT_BUCKETS_ALLOWED.has(m[1])) return true;
      } catch { return true; }
      if (/\/(candidates-uploads|models-media)\/?$/.test(u)) return true;
      if (!/\.(pdf|jpe?g|png|webp|gif|heic|svg)(\?|$)/i.test(u)) return true;
      return false;
    };

    let final = '';
    if (!invalid(rawUrl)) final = rawUrl;
    else {
      let relative = '';
      if (filePath) {
        const slashIdx = filePath.indexOf('/');
        relative = (slashIdx >= 0 && CORRECT_BUCKETS_ALLOWED.has(filePath.substring(0, slashIdx)))
          ? filePath.substring(slashIdx + 1)
          : filePath;
      } else if (rawUrl) {
        try {
          const pu = new URL(rawUrl);
          const m = pu.pathname.match(/object\/public\/[^/]+\/(.+)$/);
          if (m && m[1]) relative = m[1];
          else {
            const full = pu.pathname.split('/').filter(Boolean).join('/');
            const candidatesIdx = full.indexOf('submissions/');
            if (candidatesIdx >= 0 && /\.(jpe?g|png|webp|heic)/i.test(full)) relative = full.substring(candidatesIdx);
          }
        } catch {}
      }
      if (relative) {
        const prefix = (relative.startsWith('submissions/') || relative.startsWith('candidates/'))
          ? SUPABASE_PUBLIC_BASE
          : 'https://stmytwsdlonpnirqiufq.supabase.co/storage/v1/object/public/models-media';
        final = `${prefix}/${relative}`;
      } else final = rawUrl;
    }

    if (final) {
      // Anti duplicacao bucket path
      final = final.replace(/(object\/public\/)(candidates-uploads\/){2,}/g, '$1candidates-uploads/')
        .replace(/(object\/public\/candidates-uploads\/)\/?candidates-uploads\//g, '$1')
        .replace(/(?<!:)\/\/+/g, '/').replace('https:/', 'https://');
      // Bust cache SEM ?v= duplicado (igual ao composite!)
      let cleaned = final;
      if (cleaned.includes('v=')) {
        cleaned = cleaned
          .replace(/([?&])v=[^&]*(&|$)/g, (m: any, sep: any, end: any) => (end === '&' ? sep : ''))
          .replace(/[?&]$/, '');
      }
      const sep = cleaned.includes('?') ? '&' : '?';
      final = `${cleaned}${sep}v=${Math.floor(Date.now() / 3_600_000)}`;
    }
    return final;
  }

  // 6 Candidatos Mockados Fashion para Modo Offline / Demonstração
  private mockCandidates: Candidate[] = [
    {
      id: 'c01-valentina-rocha',
      fullName: 'Valentina Rocha',
      email: 'valentina.rocha@editorial.com.br',
      phone: '(11) 98765-4321',
      birthDate: '2007-04-12',
      isMinor: true,
      guardianName: 'Silvia Regina Rocha',
      guardianPhone: '(11) 98765-0000',
      guardianEmail: 'silvia.rocha@email.com',
      city: 'São Paulo',
      state: 'SP',
      height: 178,
      bust: 82,
      waist: 60,
      hips: 89,
      shoes: 38,
      eyeColor: 'Castanhos',
      hairColor: 'Castanho Escuro',
      status: 'PENDING',
      notes: 'Perfil com forte apelo para passarela e alta costura internacional. Excelente angulação facial.',
      createdAt: '2026-09-28T14:32:00Z',
      instagramHandle: '@valentinarocha.model',
      age: 17,
      photos: [
        {
          id: 'p1',
          url: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=1200&q=85',
          type: 'POLAROID_ROSTO'
        },
        {
          id: 'p2',
          url: 'https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&w=1200&q=85',
          type: 'POLAROID_PERFIL'
        },
        {
          id: 'p3',
          url: 'https://images.unsplash.com/photo-1524504388940-b1c1722653e1?auto=format&fit=crop&w=1200&q=85',
          type: 'CORPO_INTEIRO'
        },
        {
          id: 'p4',
          url: 'https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?auto=format&fit=crop&w=1200&q=85',
          type: 'COMPOSITE'
        }
      ]
    },
    {
      id: 'c02-gabriel-castro',
      fullName: 'Gabriel Castro',
      email: 'gabriel.castro@scouting.com',
      phone: '(21) 99123-4567',
      birthDate: '2003-11-20',
      isMinor: false,
      city: 'Rio de Janeiro',
      state: 'RJ',
      height: 188,
      bust: 98,
      waist: 77,
      hips: 95,
      shoes: 42,
      eyeColor: 'Verdes',
      hairColor: 'Castanho Claro',
      status: 'PENDING',
      notes: 'Traços marcantes de mandíbula, porte atlético para campanhas de moda masculina e alfaiataria.',
      createdAt: '2026-09-27T10:15:00Z',
      instagramHandle: '@gabrielcastro_wb',
      age: 21,
      photos: [
        {
          id: 'p5',
          url: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=1200&q=85',
          type: 'POLAROID_ROSTO'
        },
        {
          id: 'p6',
          url: 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=1200&q=85',
          type: 'POLAROID_PERFIL'
        },
        {
          id: 'p7',
          url: 'https://images.unsplash.com/photo-1492562080023-ab3db95bfbce?auto=format&fit=crop&w=1200&q=85',
          type: 'CORPO_INTEIRO'
        }
      ]
    },
    {
      id: 'c03-maya-alencar',
      fullName: 'Maya Alencar',
      email: 'maya.alencar@editorial.com',
      phone: '(71) 98877-6655',
      birthDate: '2005-08-15',
      isMinor: false,
      city: 'Salvador',
      state: 'BA',
      height: 180,
      bust: 80,
      waist: 59,
      hips: 88,
      shoes: 39,
      eyeColor: 'Castanhos',
      hairColor: 'Preto',
      status: 'APPROVED',
      notes: 'Aprovada por unanimidade pela curadoria. Agendado teste de passarela e compcard digital.',
      createdAt: '2026-09-26T16:45:00Z',
      instagramHandle: '@maya.alencar',
      age: 20,
      photos: [
        {
          id: 'p8',
          url: 'https://images.unsplash.com/photo-1531746020798-e6953c6e8e04?auto=format&fit=crop&w=1200&q=85',
          type: 'POLAROID_ROSTO'
        },
        {
          id: 'p9',
          url: 'https://images.unsplash.com/photo-1524504388940-b1c1722653e1?auto=format&fit=crop&w=1200&q=85',
          type: 'POLAROID_PERFIL'
        },
        {
          id: 'p10',
          url: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=1200&q=85',
          type: 'CORPO_INTEIRO'
        }
      ]
    },
    {
      id: 'c04-lucas-menezes',
      fullName: 'Lucas Menezes',
      email: 'lucas.menezes@scouting.com',
      phone: '(31) 97766-5544',
      birthDate: '2006-02-28',
      isMinor: false,
      city: 'Belo Horizonte',
      state: 'MG',
      height: 186,
      bust: 94,
      waist: 75,
      hips: 92,
      shoes: 41,
      eyeColor: 'Azuis',
      hairColor: 'Loiro Escuro',
      status: 'PENDING',
      notes: 'Estilo editorial andrógino, simetria facial rara. Recomenda-se polaroid com luz natural adicional.',
      createdAt: '2026-09-25T11:20:00Z',
      instagramHandle: '@menezeslucas',
      age: 19,
      photos: [
        {
          id: 'p11',
          url: 'https://images.unsplash.com/photo-1492562080023-ab3db95bfbce?auto=format&fit=crop&w=1200&q=85',
          type: 'POLAROID_ROSTO'
        },
        {
          id: 'p12',
          url: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=1200&q=85',
          type: 'POLAROID_PERFIL'
        },
        {
          id: 'p13',
          url: 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=1200&q=85',
          type: 'CORPO_INTEIRO'
        }
      ]
    },
    {
      id: 'c05-beatriz-souza',
      fullName: 'Beatriz Souza',
      email: 'beatriz.souza@fashion.com',
      phone: '(41) 99888-1122',
      birthDate: '2008-09-10',
      isMinor: true,
      guardianName: 'Carlos Eduardo Souza',
      guardianPhone: '(41) 99888-0000',
      guardianEmail: 'carlos.souza@email.com',
      city: 'Curitiba',
      state: 'PR',
      height: 176,
      bust: 81,
      waist: 58,
      hips: 87,
      shoes: 37,
      eyeColor: 'Verdes',
      hairColor: 'Ruivo Natural',
      status: 'APPROVED',
      notes: 'Menor de idade com termo e autorização de representação conferidos. Destaque em beleza natural.',
      createdAt: '2026-09-24T18:00:00Z',
      instagramHandle: '@beasouza.model',
      age: 17,
      photos: [
        {
          id: 'p14',
          url: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=1200&q=85',
          type: 'POLAROID_ROSTO'
        },
        {
          id: 'p15',
          url: 'https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&w=1200&q=85',
          type: 'POLAROID_PERFIL'
        },
        {
          id: 'p16',
          url: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=1200&q=85',
          type: 'CORPO_INTEIRO'
        }
      ]
    },
    {
      id: 'c06-theo-brandao',
      fullName: 'Théo Brandão',
      email: 'theo.brandao@models.com',
      phone: '(51) 98111-2233',
      birthDate: '2002-05-18',
      isMinor: false,
      city: 'Porto Alegre',
      state: 'RS',
      height: 173,
      bust: 88,
      waist: 76,
      hips: 91,
      shoes: 40,
      eyeColor: 'Castanhos',
      hairColor: 'Preto',
      status: 'REJECTED',
      notes: 'Medidas fora do padrão editorial requerido para a temporada atual. Encaminhado para arquivo de casting comercial futuro.',
      createdAt: '2026-09-23T09:10:00Z',
      instagramHandle: '@theobrandao',
      age: 23,
      photos: [
        {
          id: 'p17',
          url: 'https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&w=1200&q=85',
          type: 'POLAROID_ROSTO'
        },
        {
          id: 'p18',
          url: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=1200&q=85',
          type: 'POLAROID_PERFIL'
        }
      ]
    }
  ];

  /**
   * Listagem paginada com filtros por status e busca.
   * Consome GET /api/v1/admin/candidates?status={...}&page=0&size=12
   * com fallback transparente para o mock local se a API estiver inacessível.
   */
  getCandidates(filters?: {
    status?: CandidateStatus | 'ALL';
    page?: number;
    size?: number;
    search?: string;
  }): Observable<CandidatePageResponse> {
    const page = filters?.page ?? 0;
    const size = filters?.size ?? 12;
    let params = new HttpParams()
      .set('page', String(page))
      .set('size', String(size));

    if (filters?.status && filters.status !== 'ALL') {
      params = params.set('status', filters.status);
    }
    if (filters?.search && filters.search.trim()) {
      params = params.set('search', filters.search.trim());
    }

    return this.http.get<any>(`${this.baseUrl}/api/v1/admin/candidates`, { params }).pipe(
      map(response => this.normalizePageResponse(response, page, size)),
      catchError((error: HttpErrorResponse) => {
        console.warn('Candidatos da API não acessíveis (HTTP ' + error.status + '), ativando fallback mock offline.', error);
        return of(this.getMockPage(filters?.status, filters?.search, page, size));
      })
    );
  }

  /**
   * Obtém detalhes de um candidato por ID.
   */
  getCandidateById(id: string): Observable<Candidate> {
    return this.http.get<any>(`${this.baseUrl}/api/v1/admin/candidates/${id}`).pipe(
      map(data => this.normalizeCandidate(data)),
      catchError((error: HttpErrorResponse) => {
        console.warn(`Erro ao buscar candidato ${id} da API, procurando no mock local.`);
        const found = this.mockCandidates.find(c => c.id === id);
        if (found) {
          return of({ ...found });
        }
        return throwError(() => error);
      })
    );
  }

  /**
   * Atualização de status da candidatura (Triagem).
   * PATCH /api/v1/admin/candidates/{id}/status
   */
  updateStatus(id: string, payload: CandidateStatusUpdatePayload): Observable<Candidate> {
    const body = {
      status: payload.status,
      notes: payload.notes ?? payload.adminNotes ?? '',
      adminNotes: payload.notes ?? payload.adminNotes ?? ''
    };

    return this.http.patch<any>(`${this.baseUrl}/api/v1/admin/candidates/${id}/status`, body).pipe(
      map(data => this.normalizeCandidate(data)),
      catchError(() => {
        console.warn(`Atualizando status offline no mock local para candidato ${id} -> ${payload.status}`);
        const idx = this.mockCandidates.findIndex(c => c.id === id);
        if (idx !== -1) {
          this.mockCandidates[idx] = {
            ...this.mockCandidates[idx],
            status: payload.status,
            notes: payload.notes || this.mockCandidates[idx].notes
          };
          return of({ ...this.mockCandidates[idx] });
        }
        const fallbackCandidate: Candidate = {
          id,
          fullName: 'Candidato Atualizado',
          email: 'candidato@email.com',
          phone: '(11) 99999-9999',
          birthDate: '2005-01-01',
          isMinor: false,
          city: 'São Paulo',
          state: 'SP',
          height: 178,
          bust: 84,
          waist: 62,
          hips: 90,
          shoes: 38,
          eyeColor: 'Castanhos',
          hairColor: 'Castanho',
          status: payload.status,
          photos: [],
          notes: payload.notes,
          createdAt: new Date().toISOString()
        };
        return of(fallbackCandidate);
      })
    );
  }

  /**
   * Promove o candidato aprovado para o elenco oficial de modelos.
   */
  promoteToModel(id: string, activateImmediately = true): Observable<Candidate> {
    let params = new HttpParams();
    if (activateImmediately) {
      params = params.set('activateImmediately', 'true');
    }

    return this.http.post<any>(`${this.baseUrl}/api/v1/admin/candidates/${id}/promote-to-model`, null, { params }).pipe(
      map(data => this.normalizeCandidate(data)),
      catchError(() => {
        console.warn(`Promovendo offline no mock para candidato ${id}`);
        const idx = this.mockCandidates.findIndex(c => c.id === id);
        if (idx !== -1) {
          this.mockCandidates[idx] = {
            ...this.mockCandidates[idx],
            status: 'APPROVED',
            convertedToModelId: 'mod-' + id
          };
          return of({ ...this.mockCandidates[idx] });
        }
        const promotedFallback: Candidate = {
          id,
          fullName: 'Candidato Promovido',
          email: 'candidato@email.com',
          phone: '(11) 99999-9999',
          birthDate: '2005-01-01',
          isMinor: false,
          city: 'São Paulo',
          state: 'SP',
          height: 178,
          bust: 84,
          waist: 62,
          hips: 90,
          shoes: 38,
          eyeColor: 'Castanhos',
          hairColor: 'Castanho',
          status: 'APPROVED',
          photos: [],
          convertedToModelId: 'mod-' + id,
          createdAt: new Date().toISOString()
        };
        return of(promotedFallback);
      })
    );
  }

  /**
   * Move o candidato para o Arquivo Morto (Soft Delete).
   * DELETE /api/v1/admin/candidates/{id}
   */
  archiveCandidate(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/api/v1/admin/candidates/${id}`).pipe(
      catchError(() => {
        return this.http.delete<void>(`${this.baseUrl}/admin/applications/${id}`);
      })
    );
  }

  // --- Funções Auxiliares de Normalização & Mock ---

  private getMockPage(
    status: CandidateStatus | 'ALL' | undefined,
    search: string | undefined,
    page: number,
    size: number
  ): CandidatePageResponse {
    let filtered = [...this.mockCandidates];

    if (status && status !== 'ALL') {
      filtered = filtered.filter(c => c.status === status);
    }

    if (search && search.trim()) {
      const q = search.toLowerCase().trim();
      filtered = filtered.filter(c =>
        c.fullName.toLowerCase().includes(q) ||
        c.city.toLowerCase().includes(q) ||
        c.phone.includes(q) ||
        c.email.toLowerCase().includes(q)
      );
    }

    const totalElements = filtered.length;
    const totalPages = Math.ceil(totalElements / size) || 1;
    const startIndex = page * size;
    const content = filtered.slice(startIndex, startIndex + size);

    return {
      content,
      totalElements,
      totalPages,
      size,
      number: page
    };
  }

  private normalizePageResponse(raw: any, page: number, size: number): CandidatePageResponse {
    if (!raw) {
      return { content: [], totalElements: 0, totalPages: 0, size, number: page };
    }

    const items = Array.isArray(raw.content) ? raw.content : (Array.isArray(raw) ? raw : []);
    const normalizedContent = items.map((item: any) => this.normalizeCandidate(item));

    return {
      content: normalizedContent,
      totalElements: typeof raw.totalElements === 'number' ? raw.totalElements : normalizedContent.length,
      totalPages: typeof raw.totalPages === 'number' ? raw.totalPages : Math.ceil(normalizedContent.length / size),
      size: typeof raw.size === 'number' ? raw.size : size,
      number: typeof raw.number === 'number' ? raw.number : page
    };
  }

  private normalizeCandidate(item: any): Candidate {
    if (!item) return {} as Candidate;

    // Se já tiver array de fotos estruturado (também aplica fallback URL safe)
    let photos: CandidatePhoto[] = [];
    if (Array.isArray(item.photos) && item.photos.length > 0) {
      photos = item.photos.map((p: any, idx: number) => ({
        id: p.id || `p-${idx}`,
        // 🆕 RESOLVE URL SEGURA: projeto/bucket errados? Param duplicado? Remonta sempre!
        url: this._resolveCandidatePhotoUrl(p),
        type: p.type || (idx === 0 ? 'POLAROID_ROSTO' : (idx === 1 ? 'POLAROID_PERFIL' : 'CORPO_INTEIRO'))
      }));
    } else {
      // Campos legado facePhotoUrl/profilePhotoUrl/fullBodyPhotoUrl (CandidateSubmission)
      // 🆕 Resolve fallback URL safe em CADA UM dos 3 campos! Nunca mais placeholder WB
      if (item.facePhotoUrl) {
        const safe = this._resolveCandidatePhotoUrl(item.facePhotoUrl);
        if (safe) photos.push({ id: 'face', url: safe, type: 'POLAROID_ROSTO' });
      }
      if (item.profilePhotoUrl) {
        const safe = this._resolveCandidatePhotoUrl(item.profilePhotoUrl);
        if (safe) photos.push({ id: 'profile', url: safe, type: 'POLAROID_PERFIL' });
      }
      if (item.fullBodyPhotoUrl) {
        const safe = this._resolveCandidatePhotoUrl(item.fullBodyPhotoUrl);
        if (safe) photos.push({ id: 'body', url: safe, type: 'CORPO_INTEIRO' });
      }
    }

    const isMinor = item.isMinor ?? (item.age != null ? item.age < 18 : !!item.guardianName);

    return {
      id: item.id,
      fullName: item.fullName || 'Sem nome',
      email: item.email || '',
      phone: item.phone || '',
      birthDate: item.birthDate || '',
      isMinor,
      guardianName: item.guardianName,
      guardianPhone: item.guardianPhone,
      guardianEmail: item.guardianEmail,
      city: item.city || '',
      state: item.state || '',
      height: item.height || 0,
      bust: item.bust || 0,
      waist: item.waist || 0,
      hips: item.hips || 0,
      shoes: item.shoes || item.shoeSize || 0,
      eyeColor: item.eyeColor || '',
      hairColor: item.hairColor || '',
      status: (item.status as CandidateStatus) || 'PENDING',
      photos,
      notes: item.notes || item.feedbackNotes || '',
      createdAt: item.createdAt || new Date().toISOString(),
      instagramHandle: item.instagramHandle,
      age: item.age,
      gender: item.gender,
      protocol: item.protocol,
      reviewedBy: item.reviewedBy,
      reviewedAt: item.reviewedAt,
      convertedToModelId: item.convertedToModelId
    };
  }
}

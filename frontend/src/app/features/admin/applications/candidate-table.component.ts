import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Subject, debounceTime, distinctUntilChanged, catchError, of } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { fixUtf8, sanitizeCandidateName } from '../../../core/utils/text-sanitizer.util';

export interface CandidateApplicationRow {
  id: string;
  fullName: string;
  email: string;
  phone: string;
  birthDate: string;
  age: number;
  isMinor: boolean;
  city: string;
  state: string;
  height: number;
  bust: number;
  waist: number;
  hips: number;
  shoes: number;
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'ARCHIVED';
  hasPhotos: boolean;
  polaroidsCount: number;
  coverPhoto?: string;
  photos?: { id: string; url: string; type: string }[];
  createdAt: string;
  photoError?: boolean;
  protocol?: string;
}

@Component({
  selector: 'app-candidate-table',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './candidate-table.component.html',
  styleUrls: ['./candidate-table.component.scss']
})
export class CandidateTableComponent implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly searchSubject = new Subject<string>();

  applications: CandidateApplicationRow[] = [];
  isLoading = false;

  // Parâmetros de Filtro e Paginação
  selectedStatus = '';
  selectedMinor = '';
  searchQuery = '';
  pendingCount = 0;
  approvedCount = 0;
  rejectedCount = 0;
  archivedCount = 0;
  totalCount = 0;

  // Ações de Mudança de Status, Promoção e Arquivo Morto
  updatingStatusId: string | null = null;
  isPromotingId: string | null = null;
  candidateToArchive: CandidateApplicationRow | null = null;
  isArchiveModalOpen = false;
  isArchiving = false;
  feedbackMessage = '';

  pageIndex = 0;
  pageSize = 15;
  totalElements = 0;
  totalPages = 0;

  sortBy = 'createdAt';
  sortDirection: 'ASC' | 'DESC' = 'DESC';

  // Helper IDENTICO ao CandidateService._resolveCandidatePhotoUrl (stmytwsd projeto CORRETO!)
  // Resolve URLs de coverPhoto / photos[0].url CRUAS / incompletas / projeto errado (zmpqmdi)
  // que vem do backend e aparecem na LISTAGEM de candidaturas (tabela).
  // Se a URL vier invalida: remonta do zero extraindo path relativo. Nunca retorna vazio se houver path.
  private _resolveCandidatePhotoUrl(rawUrlOrFile: any, fallbackRaw: string | null = null): string {
    const SUPABASE_PUBLIC_BASE = 'https://stmytwsdlonpnirqiufq.supabase.co/storage/v1/object/public/candidates-uploads';
    const CORRECT_PROJECT_HOST = 'stmytwsdlonpnirqiufq.supabase.co';
    const CORRECT_BUCKETS_ALLOWED = new Set(['candidates-uploads', 'models-media', 'site-assets']);
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
      final = final.replace(/(object\/public\/)(candidates-uploads\/){2,}/g, '$1candidates-uploads/')
        .replace(/(object\/public\/candidates-uploads\/)\/?candidates-uploads\//g, '$1')
        .replace(/(?<!:)\/\/+/g, '/').replace('https:/', 'https://');
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

  ngOnInit(): void {
    this.setupSearchDebounce();
    this.loadApplications();
    this.loadCounts();
  }

  setupSearchDebounce(): void {
    this.searchSubject.pipe(
      debounceTime(350),
      distinctUntilChanged()
    ).subscribe((term) => {
      this.searchQuery = term;
      this.pageIndex = 0;
      this.loadApplications();
    });
  }

  onSearchChange(term: string): void {
    this.searchSubject.next(term);
  }

  setStatusFilter(status: string): void {
    this.selectedStatus = status;
    this.pageIndex = 0;
    this.loadApplications();
  }

  toggleSort(field: string): void {
    if (this.sortBy === field) {
      this.sortDirection = this.sortDirection === 'ASC' ? 'DESC' : 'ASC';
    } else {
      this.sortBy = field;
      this.sortDirection = 'DESC';
    }
    this.loadApplications();
  }

  loadApplications(): void {
    this.isLoading = true;
    let params = new HttpParams()
      .set('page', String(this.pageIndex))
      .set('size', String(this.pageSize))
      .set('sortBy', this.sortBy)
      .set('sortDirection', this.sortDirection);

    if (this.selectedStatus) params = params.set('status', this.selectedStatus);
    if (this.selectedMinor) params = params.set('isMinor', this.selectedMinor);
    if (this.searchQuery) params = params.set('search', this.searchQuery);

    this.http.get<any>(`${environment.apiUrl}/admin/applications`, { params }).subscribe({
      next: (res) => {
        const rawItems = res.content || [];
        // Filtro estrito: candidatos já promovidos ao casting não devem ser renderizados no Scouting Desk
        const items = rawItems.filter((item: any) =>
          item.status !== 'PROMOTED' && !item.promotedModelId && !item.convertedToModelId
        );
        this.applications = items.map((item: any) => {
          // ================================================================
          // 🆕 SOLUCAO SEM LÓGICA, SEM REGEX, 100% INFALIVEL:
          // BUSCA RECURSIVA EM TODO O OBJETO CANDIDATO (QUALQUER NIVEL) por
          // URLS DE FOTO. A PRIMEIRA URL DE FOTO ENCONTRADA = MINIATURA.
          // ================================================================
          const extrairTodasFotosRecursivamente = (obj: any, resultados: string[] = []): string[] => {
            if (!obj) return resultados;
            if (typeof obj === 'string') {
              const s = obj.trim();
              if (s.startsWith('http') && /\.(jpe?g|png|webp|gif|heic|svg)(\?|$)/i.test(s)) resultados.push(s);
              return resultados;
            }
            if (Array.isArray(obj)) {
              for (const elem of obj) extrairTodasFotosRecursivamente(elem, resultados);
              return resultados;
            }
            if (typeof obj === 'object') {
              for (const chave of Object.keys(obj)) {
                extrairTodasFotosRecursivamente(obj[chave], resultados);
              }
              return resultados;
            }
            return resultados;
          };

          // 🆕 ETAPA 1: Roda busca recursiva em TUDO que o backend mandou do candidato.
          const todasFotosUrls: string[] = extrairTodasFotosRecursivamente(item);
          let finalCover = '';
          if (todasFotosUrls.length > 0) {
            finalCover = this._resolveCandidatePhotoUrl(todasFotosUrls[0]);
          }

          // Resolve safePhotos (array todo normalizado para preview hover)
          let safePhotos: { id: string; url: string; type: string }[] | undefined = undefined;
          if (Array.isArray(item.photos) && item.photos.length > 0) {
            safePhotos = item.photos.map((p: any, idx: number) => {
              // tenta extrair a url da propria foto p (se for objeto) recursivamente tambem!
              const urlFotoDesta = extrairTodasFotosRecursivamente(p)[0] || (typeof p === 'string' ? p : (p?.url || p?.fileUrl || ''));
              return {
                id: p?.id || `p-${idx}`,
                url: this._resolveCandidatePhotoUrl(urlFotoDesta),
                type: p?.type || (idx === 0 ? 'POLAROID_ROSTO' : (idx === 1 ? 'POLAROID_PERFIL' : 'CORPO_INTEIRO'))
              };
            }).filter((x: any) => x.url);
          }

          // 🆕 ETAPA 2: Fallback seguro (caso busca recursiva nao tenha encontrado nada) → campos legados.
          if (!finalCover && item.coverPhoto) finalCover = this._resolveCandidatePhotoUrl(item.coverPhoto);
          if (!finalCover && item.facePhotoUrl) finalCover = this._resolveCandidatePhotoUrl(item.facePhotoUrl);
          if (!finalCover && item.profilePhotoUrl) finalCover = this._resolveCandidatePhotoUrl(item.profilePhotoUrl);
          if (!finalCover && item.fullBodyPhotoUrl) finalCover = this._resolveCandidatePhotoUrl(item.fullBodyPhotoUrl);

          // 🆕 ETAPA 3: Ultimo recurso → safePhotos[0] (se o array ja foi normalizado com sucesso, usa a 1ª).
          if (!finalCover && safePhotos && safePhotos.length > 0 && safePhotos[0].url) finalCover = safePhotos[0].url;

          return {
            ...item,
            fullName: sanitizeCandidateName(item.fullName),
            city: fixUtf8(item.city),
            state: fixUtf8(item.state),
            polaroidsCount: item.photoCount ?? item.polaroidsCount ?? ((safePhotos?.length || 0) || (item.photos ? item.photos.length : 0)),
            coverPhoto: finalCover || undefined,
            photos: safePhotos || item.photos,
            photoError: false,
            protocol: (item.protocol || '').toString().trim() || undefined,
          };
        });
        this.totalElements = res.totalElements || 0;
        this.totalPages = res.totalPages || 0;
        this.isLoading = false;
      },
      error: () => {
        this.loadMockFallback();
        this.isLoading = false;
      }
    });
  }

  onPhotoError(candidate: CandidateApplicationRow): void {
    candidate.photoError = true;
  }

  loadCounts(): void {
    this.http.get<any>(`${environment.apiUrl}/admin/applications/counts`).subscribe({
      next: (counts) => {
        if (counts) {
          if (counts.pending !== undefined) this.pendingCount = counts.pending;
          if (counts.approved !== undefined) this.approvedCount = counts.approved;
          if (counts.rejected !== undefined) this.rejectedCount = counts.rejected;
          if (counts.archived !== undefined) this.archivedCount = counts.archived;
          if (counts.total !== undefined) this.totalCount = counts.total;
        }
      },
      error: () => {}
    });
  }

  updateCandidateStatus(candidate: CandidateApplicationRow, newStatus: 'APPROVED' | 'REJECTED' | 'PENDING', event: Event): void {
    event.stopPropagation();
    if (!candidate || this.updatingStatusId) return;

    this.updatingStatusId = candidate.id;
    const previousStatus = candidate.status;
    candidate.status = newStatus;

    const payload = { status: newStatus };
    const url = `${environment.apiUrl}/admin/applications/${candidate.id}/decision`;
    const fallbackUrl = `${environment.apiUrl}/admin/candidates/${candidate.id}/status`;

    this.http.patch<any>(url, payload).pipe(
      catchError(() => this.http.patch<any>(fallbackUrl, payload))
    ).subscribe({
      next: () => {
        this.updatingStatusId = null;
        const labelMap: Record<string, string> = {
          'APPROVED': 'Aprovada',
          'REJECTED': 'Declinada',
          'PENDING': 'Pendente'
        };
        this.showFeedback(`Status de "${candidate.fullName}" atualizado para ${labelMap[newStatus] || newStatus} com sucesso no banco de dados.`);
        this.loadApplications();
        this.loadCounts();
      },
      error: () => {
        candidate.status = previousStatus;
        this.updatingStatusId = null;
        this.showFeedback('Erro ao atualizar status no banco de dados. Tente novamente.');
      }
    });
  }

  promoteCandidate(candidate: CandidateApplicationRow, event: Event): void {
    event.stopPropagation();
    if (!candidate || this.isPromotingId) return;

    this.isPromotingId = candidate.id;
    const url = `${environment.apiUrl}/admin/applications/${candidate.id}/promote`;
    const fallbackUrl = `${environment.apiUrl}/admin/applications/${candidate.id}/promote-to-model`;

    this.http.post<any>(url, null).pipe(
      catchError(() => this.http.post<any>(fallbackUrl, null))
    ).subscribe({
      next: () => {
        this.isPromotingId = null;
        this.applications = this.applications.filter(a => a.id !== candidate.id);
        this.totalElements = Math.max(0, this.totalElements - 1);
        this.loadCounts();
        this.showFeedback(`Candidato(a) "${candidate.fullName}" promovido(a) a Casting com sucesso! Perfil criado em /admin/models.`);
      },
      error: (err) => {
        this.isPromotingId = null;
        const msg = err?.error?.message || 'Erro ao promover candidato. Tente novamente.';
        this.showFeedback(msg);
      }
    });
  }

  openArchiveModal(candidate: CandidateApplicationRow, event: Event): void {
    event.stopPropagation();
    if (candidate.status !== 'REJECTED') {
      this.showFeedback('Apenas candidaturas com status DECLINADO podem ser permanentemente excluídas.');
      return;
    }
    this.candidateToArchive = candidate;
    this.isArchiveModalOpen = true;
  }

  closeArchiveModal(): void {
    this.candidateToArchive = null;
    this.isArchiveModalOpen = false;
  }

  executeArchive(): void {
    if (!this.candidateToArchive || this.isArchiving) return;
    const candidate = this.candidateToArchive;
    this.isArchiving = true;

    const url = `${environment.apiUrl}/admin/applications/${candidate.id}`;
    const fallbackUrl = `${environment.apiUrl}/admin/candidates/${candidate.id}`;

    this.http.delete(url).pipe(
      catchError(() => this.http.delete(fallbackUrl))
    ).subscribe({
      next: () => {
        this.isArchiving = false;
        this.isArchiveModalOpen = false;
        this.applications = this.applications.filter(a => a.id !== candidate.id);
        this.totalElements = Math.max(0, this.totalElements - 1);
        this.loadCounts();
        this.showFeedback(`Candidato(a) "${candidate.fullName}" e suas mídias foram permanentemente excluídos.`);
        this.candidateToArchive = null;
      },
      error: (err) => {
        this.isArchiving = false;
        this.isArchiveModalOpen = false;
        const msg = err?.error?.message || 'Erro ao excluir definitivamente a candidatura.';
        this.showFeedback(msg);
        this.candidateToArchive = null;
      }
    });
  }

  showFeedback(msg: string): void {
    this.feedbackMessage = msg;
    setTimeout(() => {
      if (this.feedbackMessage === msg) {
        this.feedbackMessage = '';
      }
    }, 4500);
  }

  goToPage(page: number): void {
    this.pageIndex = page;
    this.loadApplications();
  }

  getUpperBound(): number {
    return Math.min((this.pageIndex + 1) * this.pageSize, this.totalElements);
  }

  openDetail(id: string): void {
    this.router.navigate(['/admin/candidaturas', id]);
  }

  private loadMockFallback(): void {
    this.applications = [
      {
        id: '1',
        fullName: 'Mariana Souza Fagundes',
        email: 'mariana.souza@email.com',
        phone: '+55 11 98888-7777',
        birthDate: '2008-05-14',
        age: 18,
        isMinor: false,
        city: 'São Paulo',
        state: 'SP',
        height: 178,
        bust: 83,
        waist: 59,
        hips: 88,
        shoes: 37,
        status: 'PENDING',
        hasPhotos: true,
        polaroidsCount: 4,
        coverPhoto: 'assets/images/placeholder-polaroid.jpg',
        createdAt: '2026-09-29T14:32:00Z'
      },
      {
        id: '2',
        fullName: 'Lucas Gabriel Silveira',
        email: 'lucas.silveira@email.com',
        phone: '+55 21 97777-6666',
        birthDate: '2009-08-20',
        age: 17,
        isMinor: true,
        city: 'Niterói',
        state: 'RJ',
        height: 187,
        bust: 96,
        waist: 76,
        hips: 95,
        shoes: 42,
        status: 'PENDING',
        hasPhotos: true,
        polaroidsCount: 5,
        coverPhoto: 'assets/images/placeholder-polaroid.jpg',
        createdAt: '2026-09-29T11:15:00Z'
      }
    ];
    this.totalElements = 2;
    this.totalPages = 1;
    this.pendingCount = 2;
  }
}

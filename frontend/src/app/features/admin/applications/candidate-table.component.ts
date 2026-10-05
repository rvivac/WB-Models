import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Subject, debounceTime, distinctUntilChanged } from 'rxjs';
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
        const items = res.content || [];
        this.applications = items.map((item: any) => {
          // =====================================================================
          // 🆕 IMPLEMENTACAO EXATA PEDIDA: MINIATURA = FOTO ROSTO FRONTAL NATURAL
          // (Prioridade 1 ABSOLUTA. Nao confia mais em photos[0] aleatorio!)
          // =====================================================================
          const encontrarFotoRostoFrontalNatural = (photosArr: any[]): any | null => {
            if (!Array.isArray(photosArr) || photosArr.length === 0) return null;

            // 1) PRIORIDADE 1 MAXIMA: type = POLAROID_ROSTO (campo oficial)
            let matchRosto = photosArr.find((p: any) => {
              if (!p || typeof p !== 'object') return false;
              const t = (p.type || p.kind || '').toString().trim().toUpperCase();
              return t === 'POLAROID_ROSTO' || t === 'ROSTO' || t === 'FACE' || t === 'FRONTAL' || t === 'FACE_PHOTO' || t === 'POLAROID_FACE';
            });
            if (matchRosto) return matchRosto;

            // 2) PRIORIDADE 2: Nome do arquivo / id / label contem ROSTO FRONTAL NATURAL (case insensitive)
            const regexRosto = /ROSTO|FRONTAL|FACE|NATURAL|POLAROID_ROSTO|FOTO[\s_-]*0*1|PRIMEIRA[\s_-]*FOTO|FOTO[\s_-]*ROSTO|FRENTE/i;
            matchRosto = photosArr.find((p: any) => {
              if (!p || typeof p !== 'object') return false;
              return regexRosto.test(p.id || '') || regexRosto.test(p.type || '') ||
                     regexRosto.test(p.fileName || p.name || p.label || p.title || '') ||
                     regexRosto.test(p.url || p.fileUrl || p.storagePath || p.filePath || '');
            });
            if (matchRosto) return matchRosto;

            return null;
          };

          // 🆕 ANTES de qualquer coisa: tenta ENCONTRAR A FOTO ROSTO FRONTAL NATURAL (exatamente como pediu!)
          const fotoRostoFrontalNatural = encontrarFotoRostoFrontalNatural(item.photos);

          // Helper inline: NORMALIZA qualquer formato de foto (CandidateMedia / CandidatePhoto / DTO antigo / string)
          // para o _resolveCandidatePhotoUrl sempre reconhecer e remontar a URL correta stmytwsd.
          const normalizarFoto = (foto: any): any => {
            if (!foto) return null;
            if (typeof foto === 'string') return foto.trim();
            if (typeof foto !== 'object') return null;
            return {
              id: foto.id || foto.photoId || `f-${Date.now()}`,
              url: (foto.url || foto.fileUrl || foto.publicUrl || foto.previewUrl || '') + '',
              fileUrl: (foto.fileUrl || foto.url || foto.publicUrl || '') + '',
              filePath: (foto.filePath || foto.storagePath || foto.path || foto.relativePath || '') + '',
              storagePath: (foto.storagePath || foto.filePath || foto.path || '') + '',
              fileName: (foto.fileName || foto.name || foto.originalName || foto.label || '') + '',
              type: foto.type || ''
            };
          };

          // =====================================================================
          // ORDEM FINAL PRIORIDADES DA MINIATURA (100% o que voce pediu):
          // 1. 🆕 FOTO ROSTO FRONTAL NATURAL encontrada acima
          // 2. fallback: primeira foto do array photos[0] (caso rosto nao exista)
          // 3. coverPhoto (campo dedicado backend)
          // 4. coverImageUrl
          // 5. campos legados facePhotoUrl → profilePhotoUrl → fullBodyPhotoUrl
          // 6. ultimo: safePhotos[0].url (array normalizado, se existir)
          // =====================================================================
          let fonteCapa: any = null;
          if (fotoRostoFrontalNatural) {
            fonteCapa = normalizarFoto(fotoRostoFrontalNatural);
          }
          let safeCover = fonteCapa ? this._resolveCandidatePhotoUrl(fonteCapa) : '';

          // Se nao deu certo a foto Rosto Natural (por algum motivo), cai no fallback de photos[0] normalizado
          if (!safeCover && item.photos && item.photos.length > 0) {
            const fallback0norm = normalizarFoto(item.photos[0]);
            safeCover = fallback0norm ? this._resolveCandidatePhotoUrl(fallback0norm) : '';
          }

          // Fallback: coverPhoto / coverImageUrl se existir
          if (!safeCover && item.coverPhoto) safeCover = this._resolveCandidatePhotoUrl(normalizarFoto(item.coverPhoto));
          if (!safeCover && item.coverImageUrl) safeCover = this._resolveCandidatePhotoUrl(normalizarFoto(item.coverImageUrl));

          // 3) Resolve TODO array photos → URLs seguras normalizadas (hover preview / fallback)
          let safePhotos: { id: string; url: string; type: string }[] | undefined = undefined;
          if (Array.isArray(item.photos) && item.photos.length > 0) {
            safePhotos = item.photos.map((p: any, idx: number) => {
              const norm = normalizarFoto(p);
              return {
                id: norm && norm.id ? norm.id : `p-${idx}`,
                url: norm ? this._resolveCandidatePhotoUrl(norm) : '',
                type: p.type || (idx === 0 ? 'POLAROID_ROSTO' : (idx === 1 ? 'POLAROID_PERFIL' : 'CORPO_INTEIRO'))
              };
            }).filter(x => x.url);
          }

          // 4) Fallback legado: facePhotoUrl / profile / fullBody
          let finalCover = safeCover;
          if (!finalCover && item.facePhotoUrl) finalCover = this._resolveCandidatePhotoUrl(normalizarFoto(item.facePhotoUrl));
          if (!finalCover && item.profilePhotoUrl) finalCover = this._resolveCandidatePhotoUrl(normalizarFoto(item.profilePhotoUrl));
          if (!finalCover && item.fullBodyPhotoUrl) finalCover = this._resolveCandidatePhotoUrl(normalizarFoto(item.fullBodyPhotoUrl));

          // 5) Ultimo recurso: usa PRIMEIRA FOTO do array safePhotos (100% normalizada e resolvida)
          if (!finalCover && safePhotos && safePhotos.length > 0 && safePhotos[0].url) {
            finalCover = safePhotos[0].url;
          }

          return {
            ...item,
            fullName: sanitizeCandidateName(item.fullName),
            city: fixUtf8(item.city),
            state: fixUtf8(item.state),
            polaroidsCount: item.photoCount ?? item.polaroidsCount ?? ((safePhotos?.length || 0) || (item.photos ? item.photos.length : 0)),
            coverPhoto: finalCover || undefined,
            photos: safePhotos || item.photos,
            photoError: false,
            // Protocolo Scouting (WB-YYYYMMDD-XXXX) vindo do backend. Disponivel para link no HTML do card.
            protocol: (item.protocol || '').toString().trim() || undefined,
          };
        });          };
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
        if (counts && counts.pending !== undefined) {
          this.pendingCount = counts.pending;
        }
      },
      error: () => {}
    });
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

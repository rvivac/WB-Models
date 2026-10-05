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
          // 🆕 SOLUCAO DEFINITIVA: MINIATURA SEMPRE APARECE.
          // Extrai FOTOS DE TUDO QUE EXISTIR NO OBJETO CANDIDATO.
          // NAO depende de type, fileName, posicao no array, nome de campo, nada.
          // Se o candidato TIVER QUALQUER FOTO (badge 3 Polaroids confirma que tem!),
          // a PRIMEIRA FOTO VALIDA ENCONTRADA será a miniatura do card. Ponto final.
          // =====================================================================
          const extrairTodasFontesFotoCandidato = (cand: any): any[] => {
            const fontes: any[] = [];
            if (!cand) return fontes;

            // 🆕 ================================================================
            // PRIORIDADE 0 (MAXIMA): FOTO FACE / ROSTO / FRONTAL.
            // Ex: face_Gemini_Generated_Image_kjejtvkjejtvkjej.jpeg
            // Passa POR CIMA de qualquer outra foto. Nao importa se e a 1ª ou 3ª.
            // ================================================================
            const regexRostoFace = /face[_-]|rosto|frontal|natural|_rosto|face_photo|polaroid[_-]?face|polaroid[_-]?rosto|foto[_-]?0*1/i;
            if (Array.isArray(cand.photos) && cand.photos.length > 0) {
              const idxFotoRosto = cand.photos.findIndex((p: any) => {
                if (!p) return false;
                if (typeof p === 'string') return regexRostoFace.test(p);
                if (typeof p !== 'object') return false;
                return regexRostoFace.test(p.id || '') || regexRostoFace.test(p.type || '') ||
                       regexRostoFace.test(p.fileName || p.name || p.label || p.title || '') ||
                       regexRostoFace.test(p.url || p.fileUrl || '') ||
                       regexRostoFace.test(p.filePath || p.storagePath || p.path || '');
              });
              if (idxFotoRosto >= 0) {
                // INSERE NO INICIO (posicao 0) para que o loop de finalCover PEGUE ELA PRIMEIRO.
                const fotoRosto = cand.photos[idxFotoRosto];
                fontes.push(fotoRosto);
              }
            }

            // 1) Campos top-level conhecidos (cover, facePhotoUrl etc)
            ['coverPhoto', 'coverImageUrl', 'facePhotoUrl', 'profilePhotoUrl', 'fullBodyPhotoUrl', 'thumbnailUrl', 'previewPhoto', 'polaroid0', 'polaroidFace', 'mainPhoto']
              .forEach(campo => { if (cand[campo]) fontes.push(cand[campo]); });

            // 2) ARRAY PHOTOS: TODAS as fotos do array, TODAS as posicoes, TODOS os campos de foto possiveis.
            //    Nao importa se e candidato com 3 ou 4 ou 5 polaroids: PEGA TUDO.
            if (Array.isArray(cand.photos) && cand.photos.length > 0) {
              cand.photos.forEach((p: any) => {
                if (!p) return;
                if (typeof p === 'string') { if (p) fontes.push(p); return; }
                if (typeof p !== 'object') return;
                const tentarCampos = ['url', 'fileUrl', 'publicUrl', 'src', 'href', 'imageUrl', 'previewUrl', 'link', 'photoUrl', 'storageUrl', 'cdnUrl', 'filePath', 'storagePath', 'path', 'relativePath', 'value', 'contentUrl', 'downloadUrl'];
                // Primeiro: tenta CAMPOS INDIVIDUAIS
                tentarCampos.forEach(c => { if (p[c]) fontes.push(p[c]); });
                // Depois: coloca o OBJETO p INTEIRO como ultimo recurso (helper normaliza)
                fontes.push(p);
              });
            }

            // 3) Array legado candidatePictures / polaroids / submissionsPhotos se existir com outro nome
            ['candidatePhotos', 'candidatePictures', 'polaroids', 'submissionPhotos', 'media', 'images', 'pictures', 'fotos']
              .forEach(arrCampo => {
                const arr = cand[arrCampo];
                if (Array.isArray(arr) && arr.length > 0) {
                  arr.forEach((p: any) => {
                    if (!p) return;
                    if (typeof p === 'string') { if (p) fontes.push(p); return; }
                    if (typeof p === 'object') {
                      ['url', 'fileUrl', 'publicUrl', 'src', 'filePath', 'storagePath'].forEach(c => { if (p[c]) fontes.push(p[c]); });
                      fontes.push(p);
                    }
                  });
                }
              });

            // 4) Campos dentro de item.photo ou item.polaroid (singular) se existir
            if (cand.photo && typeof cand.photo === 'object') {
              ['url', 'fileUrl', 'publicUrl', 'filePath', 'storagePath'].forEach(c => { if (cand.photo[c]) fontes.push(cand.photo[c]); });
              fontes.push(cand.photo);
            }

            return fontes.filter(x => !!x);
          };

          // Helper inline: NORMALIZA qualquer coisa para o _resolveCandidatePhotoUrl reconhecer
          const normalizarFotoUniversal = (foto: any): any => {
            if (!foto) return null;
            if (typeof foto === 'string') {
              const s = foto.trim();
              // Se ja for URL completa: retorna direto (helper identifica e ajusta projeto/bucket se precisar)
              if (s.startsWith('http') || s.startsWith('/')) return s;
              // Senao: passa como filePath (relativo)
              return { filePath: s, url: s, fileUrl: s, storagePath: s };
            }
            if (typeof foto !== 'object') return null;
            // Nao importa os nomes de campos: junta TUDO o que pode ser foto no objeto normalizado
            return {
              id: foto.id || foto.photoId || foto.uuid || `f-${Date.now()}`,
              url: (foto.url || foto.fileUrl || foto.publicUrl || foto.previewUrl || foto.src || foto.imageUrl || foto.href || foto.downloadUrl || '') + '',
              fileUrl: (foto.fileUrl || foto.url || foto.publicUrl || '') + '',
              filePath: (foto.filePath || foto.storagePath || foto.path || foto.relativePath || foto.objectPath || foto.localPath || foto.fullPath || '') + '',
              storagePath: (foto.storagePath || foto.filePath || foto.path || foto.key || foto.objectKey || '') + '',
              fileName: (foto.fileName || foto.name || foto.originalName || foto.label || foto.title || '') + '',
              type: foto.type || foto.kind || foto.mediaType || ''
            };
          };

          // 🆕 PEGA A PRIMEIRA FOTO VÁLIDA DE TODAS AS FONTES EXTRAÍDAS.
          //    Nao precisa ser a foto[0], nao precisa ser type ROSTO: QUALQUER foto válida já resolve o placeholder.
          const fontesFotos = extrairTodasFontesFotoCandidato(item).map(normalizarFotoUniversal).filter(x => !!x);
          let finalCover = '';
          for (const fonte of fontesFotos) {
            const resolvida = this._resolveCandidatePhotoUrl(fonte);
            if (resolvida && resolvida.length > 10 && resolvida.startsWith('http')) {
              finalCover = resolvida;
              break;
            }
          }

          // Resolve safePhotos (array todo normalizado para preview hover)
          let safePhotos: { id: string; url: string; type: string }[] | undefined = undefined;
          if (Array.isArray(item.photos) && item.photos.length > 0) {
            safePhotos = item.photos.map((p: any, idx: number) => {
              const norm = normalizarFotoUniversal(p);
              return {
                id: norm && norm.id ? norm.id : `p-${idx}`,
                url: norm ? this._resolveCandidatePhotoUrl(norm) : '',
                type: p?.type || (idx === 0 ? 'POLAROID_ROSTO' : (idx === 1 ? 'POLAROID_PERFIL' : 'CORPO_INTEIRO'))
              };
            }).filter((x: any) => x.url);
          }

          // ULTIMO RECURSO: se ainda esta vazio, tenta safePhotos[0].url
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

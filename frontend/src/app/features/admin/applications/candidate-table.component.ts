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
        this.applications = items.map((item: any) => ({
          ...item,
          fullName: sanitizeCandidateName(item.fullName),
          city: fixUtf8(item.city),
          state: fixUtf8(item.state),
          polaroidsCount: item.photoCount ?? item.polaroidsCount ?? (item.photos ? item.photos.length : 0),
          coverPhoto: item.coverPhoto || (item.photos && item.photos.length > 0 ? item.photos[0].url : undefined),
          photoError: false
        }));
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

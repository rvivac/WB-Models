import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  CandidateStatusUpdate,
  CandidateSubmissionFilters,
  CandidateSubmissionResponse,
  PageResponse
} from '../../shared/models/candidate-submission.interface';

@Injectable({
  providedIn: 'root'
})
export class AdminCandidateService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiUrl;

  /**
   * Consulta paginada com filtros dinâmicos no endpoint administrativo de candidaturas.
   */
  getSubmissions(filters: CandidateSubmissionFilters): Observable<PageResponse<CandidateSubmissionResponse>> {
    let params = new HttpParams()
      .set('page', String(filters.page ?? 0))
      .set('size', String(filters.size ?? 20));

    if (filters.sort) {
      params = params.set('sort', filters.sort);
    }
    if (filters.search && filters.search.trim()) {
      params = params.set('search', filters.search.trim());
    }
    if (filters.status && filters.status !== 'ALL') {
      params = params.set('status', filters.status);
    }
    if (filters.gender) {
      params = params.set('gender', filters.gender);
    }
    if (filters.minHeight != null) {
      params = params.set('minHeight', String(filters.minHeight));
    }
    if (filters.maxHeight != null) {
      params = params.set('maxHeight', String(filters.maxHeight));
    }
    if (filters.startDate) {
      params = params.set('startDate', filters.startDate);
    }
    if (filters.endDate) {
      params = params.set('endDate', filters.endDate);
    }

    return this.http.get<PageResponse<CandidateSubmissionResponse>>(`${this.baseUrl}/admin/submissions`, { params });
  }

  /**
   * Obtém o dossiê completo de uma candidatura pelo UUID.
   */
  getSubmissionById(id: string): Observable<CandidateSubmissionResponse> {
    return this.http.get<CandidateSubmissionResponse>(`${this.baseUrl}/admin/submissions/${id}`);
  }

  /**
   * Atualiza o status operacional (APPROVED, REJECTED, ARCHIVED) com notas de feedback do Booker.
   */
  updateStatus(id: string, payload: CandidateStatusUpdate): Observable<CandidateSubmissionResponse> {
    return this.http.patch<CandidateSubmissionResponse>(`${this.baseUrl}/admin/submissions/${id}/status`, payload);
  }
}

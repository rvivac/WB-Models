import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface AdminAuditLogItem {
  id: string;
  adminId?: string;
  adminEmail: string;
  action: string;
  resourceType: string;
  resourceId?: string;
  description?: string;
  detailsJson?: Record<string, any>;
  ipAddress?: string;
  userAgent?: string;
  createdAt: string;
}

export interface AuditLogFilterParams {
  adminEmail?: string;
  resourceType?: string;
  action?: string;
  startDate?: string;
  endDate?: string;
  search?: string;
  page?: number;
  size?: number;
  sort?: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class AuditLogService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/admin/audit-logs`;

  getAuditLogs(filters: AuditLogFilterParams = {}): Observable<PageResponse<AdminAuditLogItem>> {
    let params = new HttpParams();

    if (filters.adminEmail && filters.adminEmail.trim()) {
      params = params.set('adminEmail', filters.adminEmail.trim());
    }
    if (filters.resourceType && filters.resourceType.trim()) {
      params = params.set('resourceType', filters.resourceType.trim());
    }
    if (filters.action && filters.action.trim()) {
      params = params.set('action', filters.action.trim());
    }
    if (filters.startDate) {
      params = params.set('startDate', filters.startDate);
    }
    if (filters.endDate) {
      params = params.set('endDate', filters.endDate);
    }
    if (filters.search && filters.search.trim()) {
      params = params.set('search', filters.search.trim());
    }
    if (filters.page !== undefined) {
      params = params.set('page', filters.page.toString());
    }
    if (filters.size !== undefined) {
      params = params.set('size', filters.size.toString());
    }
    if (filters.sort && filters.sort.trim()) {
      params = params.set('sort', filters.sort.trim());
    }

    return this.http.get<PageResponse<AdminAuditLogItem>>(this.baseUrl, { params });
  }

  getAuditLogById(id: string): Observable<AdminAuditLogItem> {
    return this.http.get<AdminAuditLogItem>(`${this.baseUrl}/${id}`);
  }
}

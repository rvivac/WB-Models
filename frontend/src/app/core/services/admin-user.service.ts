import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface AdminUserItem {
  id: string;
  name: string;
  email: string;
  role: 'SUPER_ADMIN' | 'WEBMASTER' | 'ADMIN' | 'SCOUT' | string;
  isActive: boolean;
  is2faEnabled: boolean;
  mustChangePassword?: boolean;
  lastLoginAt?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface CreateAdminUserPayload {
  name: string;
  email: string;
  role: 'SUPER_ADMIN' | 'WEBMASTER' | 'ADMIN' | 'SCOUT' | string;
}

export interface CreateAdminUserResponse {
  user: AdminUserItem;
  temporaryPassword: string;
}

export interface PaginatedAdminUsers {
  content: AdminUserItem[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

@Injectable({
  providedIn: 'root'
})
export class AdminUserService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/admin/users`;

  listUsers(page = 0, size = 20): Observable<PaginatedAdminUsers> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', 'name,asc');

    return this.http.get<PaginatedAdminUsers>(this.baseUrl, { params });
  }

  createUser(payload: CreateAdminUserPayload): Observable<CreateAdminUserResponse> {
    return this.http.post<CreateAdminUserResponse>(this.baseUrl, payload);
  }

  toggleStatus(id: string): Observable<AdminUserItem> {
    return this.http.patch<AdminUserItem>(`${this.baseUrl}/${id}/toggle-status`, {});
  }

  updateRole(id: string, role: string): Observable<AdminUserItem> {
    return this.http.patch<AdminUserItem>(`${this.baseUrl}/${id}/role`, { role });
  }
}

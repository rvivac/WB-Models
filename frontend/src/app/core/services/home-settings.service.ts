import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface HomeSettings {
  id?: string;
  heroTitle: string;
  heroSubtitle: string;
  heroDescription?: string;
  bannerImageUrl?: string;
  videoUrl?: string;
  aboutPreview?: string;
  metaTitle?: string;
  metaDescription?: string;
  scrollLabel?: string;
}

@Injectable({
  providedIn: 'root'
})
export class HomeSettingsService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiUrl;

  /**
   * Obtém as configurações públicas da página principal salvas no banco de dados.
   */
  getPublicSettings(): Observable<HomeSettings> {
    return this.http.get<HomeSettings>(`${this.baseUrl}/home-settings`);
  }

  /**
   * Obtém as configurações da Home para a área administrativa.
   */
  getAdminSettings(): Observable<HomeSettings> {
    return this.http.get<HomeSettings>(`${this.baseUrl}/admin/home-settings`);
  }

  /**
   * Atualiza as configurações da página principal no banco de dados.
   */
  updateSettings(settings: Partial<HomeSettings>): Observable<HomeSettings> {
    return this.http.put<HomeSettings>(`${this.baseUrl}/admin/home-settings`, settings);
  }
}

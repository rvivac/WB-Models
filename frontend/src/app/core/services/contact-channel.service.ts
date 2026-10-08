import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface ContactChannel {
  id?: string;
  type: string; // 'WHATSAPP' | 'EMAIL' | 'PHONE' | 'INSTAGRAM' | 'ADDRESS' | 'OFFICE_HOURS' | 'LINK'
  value: string;
  label: string;
  active: boolean;
  displayOrder: number;
}

@Injectable({
  providedIn: 'root'
})
export class ContactChannelService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiUrl;

  /**
   * Obtém os canais de contato públicos e ativos ordenados por displayOrder.
   */
  getPublicChannels(): Observable<ContactChannel[]> {
    return this.http.get<ContactChannel[]>(`${this.baseUrl}/contact-channels`);
  }

  /**
   * Obtém todos os canais de contato (ativos e inativos) para gestão administrativa.
   */
  getAdminChannels(): Observable<ContactChannel[]> {
    return this.http.get<ContactChannel[]>(`${this.baseUrl}/admin/contact-channels`);
  }

  /**
   * Cria um novo canal de contato no banco de dados.
   */
  createChannel(channel: Partial<ContactChannel>): Observable<ContactChannel> {
    return this.http.post<ContactChannel>(`${this.baseUrl}/admin/contact-channels`, channel);
  }

  /**
   * Atualiza um canal de contato existente no banco de dados.
   */
  updateChannel(id: string, channel: Partial<ContactChannel>): Observable<ContactChannel> {
    return this.http.put<ContactChannel>(`${this.baseUrl}/admin/contact-channels/${id}`, channel);
  }

  /**
   * Remove um canal de contato do banco de dados.
   */
  deleteChannel(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/admin/contact-channels/${id}`);
  }
}

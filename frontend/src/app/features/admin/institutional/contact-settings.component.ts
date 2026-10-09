import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';

export interface AddressData {
  street?: string;
  complement?: string;
  neighborhood?: string;
  city?: string;
  state?: string;
  zipCode?: string;
  country?: string;
}

export interface SocialMediaData {
  instagram?: string;
  facebook?: string;
}

export interface SocialMediaItem {
  id?: string;
  name: string;
  url: string;
  isEditing?: boolean;
}

export interface ContactSettingsData {
  primaryEmail: string;
  scoutingEmail?: string;
  pressEmail?: string;
  phone: string;
  whatsapp: string;
  whatsappDefaultMessage?: string;
  businessHours?: string;
  address?: AddressData;
  socialMedia?: SocialMediaData;
  socialMediaList?: SocialMediaItem[];
  [key: string]: any;
}

@Component({
  selector: 'app-contact-settings',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterModule],
  templateUrl: './contact-settings.component.html',
  styleUrls: ['./contact-settings.component.scss']
})
export class ContactSettingsComponent implements OnInit {
  private http = inject(HttpClient);

  contactData: ContactSettingsData = {
    primaryEmail: 'info@wbagency.com.br',
    scoutingEmail: 'scouting@wbagency.com.br',
    pressEmail: 'press@wbagency.com.br',
    phone: '+55 11 97065-6003',
    whatsapp: '+55 11 97065-6003',
    whatsappDefaultMessage: 'Olá! Gostaria de falar com a equipe de atendimento da WB Agency.',
    businessHours: 'Segunda a Sexta: 09h às 18h (GMT-3)',
    address: {
      street: 'Avenida Paulista, 1000',
      complement: 'Conjunto 1402',
      neighborhood: 'Bela Vista',
      city: 'São Paulo',
      state: 'SP',
      zipCode: '01310-100',
      country: 'Brasil'
    },
    socialMedia: {
      instagram: 'https://instagram.com/wbagency',
      facebook: ''
    }
  };

  socialMediaList: SocialMediaItem[] = [];

  editingField: string | null = null;
  tempValue: any = '';
  tempAddress: AddressData = {};
  isSaving = false;
  successField: string | null = null;
  errorMessage: string | null = null;

  ngOnInit(): void {
    this.loadContactData();
  }

  loadContactData(): void {
    this.http.get<ContactSettingsData>(`${environment.apiUrl}/admin/institutional/contact`).subscribe({
      next: (data) => {
        if (data) {
          this.contactData = {
            ...this.contactData,
            ...data,
            address: { ...this.contactData.address, ...data.address },
            socialMedia: { ...this.contactData.socialMedia, ...data.socialMedia }
          };
          if (data.socialMediaList && Array.isArray(data.socialMediaList)) {
            this.socialMediaList = data.socialMediaList.map(item => ({
              ...item,
              isEditing: false
            }));
          }
        }
      },
      error: (err) => {
        this.errorMessage = 'Erro ao carregar canais de contato do servidor. Verifique a conexão com o banco de dados.';
        console.error('Falha ao carregar canais de contato:', err);
      }
    });
  }

  startEdit(field: string, currentValue: any): void {
    this.errorMessage = null;
    this.editingField = field;
    if (field === 'address') {
      this.tempAddress = { ...(this.contactData.address || {}) };
    } else {
      this.tempValue = currentValue != null ? currentValue : '';
    }
    setTimeout(() => {
      const el = document.getElementById('field-' + field);
      if (el) el.focus();
    }, 50);
  }

  cancelEdit(): void {
    this.editingField = null;
    this.tempValue = '';
    this.tempAddress = {};
    this.errorMessage = null;
  }

  isFieldEmpty(field: string): boolean {
    if (field === 'address') {
      const a = this.contactData.address;
      return !a || (!a.street && !a.city);
    }
    if (field.startsWith('socialMedia.')) {
      const networkKey = field.split('.')[1];
      const val = (this.contactData.socialMedia as any)?.[networkKey];
      return !val || (typeof val === 'string' && val.trim() === '');
    }
    const val = this.contactData[field];
    return !val || (typeof val === 'string' && val.trim() === '');
  }

  getFieldStatusClass(field: string): string {
    if (this.editingField === field) {
      return 'status-editing';
    }
    if (this.isFieldEmpty(field)) {
      return 'status-empty';
    }
    return 'status-ready';
  }

  getFieldStatusLabel(field: string): string {
    if (this.editingField === field) {
      return 'Editando';
    }
    if (this.isFieldEmpty(field)) {
      return 'Vazio';
    }
    return 'Salvo';
  }

  saveField(field: string): void {
    this.isSaving = true;
    this.errorMessage = null;

    if (this.editingField !== field) {
      if (field === 'address') {
        this.tempAddress = { ...(this.contactData.address || {}) };
      } else if (field.startsWith('socialMedia.')) {
        const networkKey = field.split('.')[1];
        this.tempValue = (this.contactData.socialMedia as any)?.[networkKey] || '';
      } else {
        this.tempValue = this.contactData[field] || '';
      }
    }

    let payload: Record<string, any> = {};

    if (field === 'address') {
      payload['address'] = { ...this.tempAddress };
    } else if (field.startsWith('socialMedia.')) {
      const networkKey = field.split('.')[1];
      payload['socialMedia'] = {
        ...(this.contactData.socialMedia || {}),
        [networkKey]: this.tempValue
      };
    } else {
      payload[field] = this.tempValue;
    }

    this.http.patch<ContactSettingsData>(`${environment.apiUrl}/admin/institutional/contact`, payload).subscribe({
      next: (res) => {
        this.isSaving = false;
        if (field === 'address') {
          this.contactData.address = { ...this.tempAddress };
        } else if (field.startsWith('socialMedia.')) {
          const networkKey = field.split('.')[1];
          if (!this.contactData.socialMedia) {
            this.contactData.socialMedia = {};
          }
          (this.contactData.socialMedia as any)[networkKey] = this.tempValue;
        } else {
          this.contactData[field] = this.tempValue;
        }

        if (res) {
          this.contactData = {
            ...this.contactData,
            ...res,
            address: { ...this.contactData.address, ...res.address },
            socialMedia: { ...this.contactData.socialMedia, ...res.socialMedia }
          };
        }

        this.showSuccessFeedback(field);
        this.cancelEdit();
      },
      error: (err) => {
        this.isSaving = false;
        const msg = err?.error?.message || err?.message || 'Falha na comunicação com o servidor.';
        this.errorMessage = `Erro ao salvar alteração no banco de dados: ${msg}`;
        console.error(`Erro ao salvar campo ${field}:`, err);
      }
    });
  }

  cleanNumber(num?: string): string {
    return num ? num.replace(/\D/g, '') : '';
  }

  encodeText(txt?: string): string {
    return txt ? encodeURIComponent(txt) : '';
  }

  private showSuccessFeedback(field: string): void {
    this.successField = field;
    setTimeout(() => {
      if (this.successField === field) {
        this.successField = null;
      }
    }, 2500);
  }

  addSocialMedia(): void {
    this.socialMediaList.push({
      name: '',
      url: '',
      isEditing: true
    });
  }

  removeSocialMedia(index: number): void {
    this.socialMediaList.splice(index, 1);
    this.saveSocialMediaList();
  }

  saveSocialMediaItem(item: SocialMediaItem): void {
    if (!item.name.trim() || !item.url.trim()) return;
    item.isEditing = false;
    this.saveSocialMediaList();
  }

  cancelSocialMediaEdit(item: SocialMediaItem, index: number): void {
    if (!item.name && !item.url) {
      this.socialMediaList.splice(index, 1);
    } else {
      item.isEditing = false;
    }
  }

  saveSocialMediaList(): void {
    const payload = {
      socialMediaList: this.socialMediaList.map(item => ({
        id: item.id || undefined,
        name: item.name.trim(),
        url: item.url.trim()
      }))
    };

    this.http.patch<ContactSettingsData>(`${environment.apiUrl}/admin/institutional/contact`, payload).subscribe({
      next: (res) => {
        if (res && res.socialMediaList) {
          this.socialMediaList = res.socialMediaList.map(item => ({
            ...item,
            isEditing: false
          }));
        }
        this.showSuccessFeedback('socialMediaList');
      },
      error: (err) => {
        const msg = err?.error?.message || err?.message || 'Falha ao salvar redes sociais.';
        this.errorMessage = `Erro ao salvar redes sociais: ${msg}`;
        console.error('Erro ao salvar redes sociais:', err);
      }
    });
  }
}

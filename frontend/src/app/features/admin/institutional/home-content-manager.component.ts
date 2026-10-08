import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { HttpClient, HttpEventType } from '@angular/common/http';
import { environment } from '../../../../environments/environment';
import { HomeSettingsService, HomeSettings } from '../../../core/services/home-settings.service';

export interface HomeContentData {
  heroTitle: string;
  heroSubtitle: string;
  heroDescription: string;
  scrollLabel: string;
  metaTitle: string;
  metaDescription: string;
  videoUrl?: string;
  posterUrl?: string;
}

@Component({
  selector: 'app-home-content-manager',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './home-content-manager.component.html',
  styleUrls: ['./home-content-manager.component.scss']
})
export class HomeContentManagerComponent implements OnInit {
  private fb = inject(FormBuilder);
  private http = inject(HttpClient);
  private homeSettingsService = inject(HomeSettingsService);

  form: FormGroup = this.fb.group({
    heroTitle: ['', [Validators.maxLength(50)]],
    heroSubtitle: ['', [Validators.maxLength(80)]],
    heroDescription: ['', [Validators.maxLength(250)]],
    scrollLabel: ['SCROLL'],
    metaTitle: [''],
    metaDescription: ['']
  });

  currentVideoUrl = '';
  currentPosterUrl = '';

  editingField: string | null = null;
  tempFieldValues: { [key: string]: string } = {};

  isSaving = false;
  isVideoUploading = false;
  videoUploadProgress = 0;
  feedbackMessage = '';

  startEdit(field: string): void {
    this.editingField = field;
    this.tempFieldValues[field] = this.form.get(field)?.value || '';
    setTimeout(() => {
      const el = document.getElementById(field);
      if (el) {
        el.focus();
      }
    }, 50);
  }

  cancelEdit(): void {
    if (this.editingField && this.tempFieldValues[this.editingField] !== undefined) {
      this.form.get(this.editingField)?.setValue(this.tempFieldValues[this.editingField]);
    }
    this.editingField = null;
  }

  saveField(field: string): void {
    if (this.form.get(field)?.invalid) return;
    this.saveTextContent();
    this.editingField = null;
  }

  isFieldEmpty(field: string): boolean {
    const val = this.form.get(field)?.value;
    return val === null || val === undefined || (typeof val === 'string' && val.trim() === '');
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
      return 'Em Edição';
    }
    if (this.isFieldEmpty(field)) {
      return 'Vazio';
    }
    return 'Salvo';
  }

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.homeSettingsService.getAdminSettings().subscribe({
      next: (data) => {
        if (data) {
          this.form.patchValue(data);
          this.currentVideoUrl = data.videoUrl || '';
          this.currentPosterUrl = data.bannerImageUrl || '';
        }
      },
      error: (err) => {
        this.showFeedback('Erro ao carregar configurações da Home do servidor.');
        console.error('Falha ao carregar configurações da Home:', err);
      }
    });
  }

  saveTextContent(): void {
    if (this.form.invalid) return;

    this.isSaving = true;
    const payload = {
      ...this.form.value,
      videoUrl: this.currentVideoUrl,
      bannerImageUrl: this.currentPosterUrl
    };

    this.homeSettingsService.updateSettings(payload).subscribe({
      next: (res) => {
        this.isSaving = false;
        if (res) {
          this.form.patchValue(res);
        }
        this.showFeedback('Configurações da Home salvas com sucesso no banco de dados!');
      },
      error: (err) => {
        this.isSaving = false;
        const msg = err?.error?.message || err?.message || 'Falha na comunicação com o servidor.';
        this.showFeedback(`Erro ao salvar configurações da Home: ${msg}`);
        console.error('Falha ao salvar configurações da Home:', err);
      }
    });
  }

  onVideoSelected(event: Event): void {
    const target = event.target as HTMLInputElement;
    if (target.files && target.files.length > 0) {
      const file = target.files[0];
      if (file.size > 50 * 1024 * 1024) {
        this.showFeedback('O arquivo de vídeo excede o teto permitido de 50MB.');
        return;
      }

      const formData = new FormData();
      formData.append('file', file);

      this.isVideoUploading = true;
      this.videoUploadProgress = 0;

      this.http.put<any>(
        `${environment.apiUrl}/admin/institutional/home/video`,
        formData,
        { reportProgress: true, observe: 'events' }
      ).subscribe({
        next: (ev) => {
          if (ev.type === HttpEventType.UploadProgress && ev.total) {
            this.videoUploadProgress = Math.round((100 * ev.loaded) / ev.total);
          } else if (ev.type === HttpEventType.Response) {
            if (ev.body && ev.body.videoUrl) {
              this.currentVideoUrl = ev.body.videoUrl;
            }
            this.isVideoUploading = false;
            this.showFeedback('Vídeo Hero atualizado com sucesso!');
          }
        },
        error: () => {
          this.isVideoUploading = false;
          this.showFeedback('Falha no upload do vídeo.');
        }
      });
    }
  }

  onPosterSelected(event: Event): void {
    const target = event.target as HTMLInputElement;
    if (target.files && target.files.length > 0) {
      const file = target.files[0];
      if (file.size > 10 * 1024 * 1024) {
        this.showFeedback('A imagem de poster excede o teto permitido de 10MB.');
        return;
      }

      const formData = new FormData();
      formData.append('file', file);

      this.http.put<any>(`${environment.apiUrl}/admin/institutional/home/poster`, formData).subscribe({
        next: (res) => {
          if (res && res.posterUrl) {
            this.currentPosterUrl = res.posterUrl;
          }
          this.showFeedback('Poster de contingência atualizado com sucesso!');
        },
        error: () => this.showFeedback('Falha no upload do poster.')
      });
    }
  }

  private showFeedback(msg: string): void {
    this.feedbackMessage = msg;
    setTimeout(() => {
      if (this.feedbackMessage === msg) {
        this.feedbackMessage = '';
      }
    }, 4000);
  }
}

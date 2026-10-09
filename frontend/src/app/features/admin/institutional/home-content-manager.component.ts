import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { HttpClient, HttpEventType } from '@angular/common/http';
import { environment } from '../../../../environments/environment';
import { HomeSettingsService } from '../../../core/services/home-settings.service';

@Component({
  selector: 'app-home-content-manager',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './home-content-manager.component.html',
  styleUrls: ['./home-content-manager.component.scss']
})
export class HomeContentManagerComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly http = inject(HttpClient);
  private readonly homeSettingsService = inject(HomeSettingsService);

  readonly form: FormGroup = this.fb.group({
    heroTitle: ['', [Validators.maxLength(50)]],
    heroSubtitle: ['', [Validators.maxLength(80)]],
    scrollLabel: ['SCROLL'],
    metaTitle: [''],
    metaDescription: ['']
  });

  currentVideoUrl = '';
  currentPosterUrl = '';

  isSaving = false;
  isVideoUploading = false;
  videoUploadProgress = 0;
  feedbackMessage = '';

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.homeSettingsService.getAdminSettings().subscribe({
      next: (data) => {
        if (data) {
          this.form.patchValue({
            heroTitle: data.heroTitle || '',
            heroSubtitle: data.heroSubtitle || '',
            scrollLabel: data.scrollLabel || 'SCROLL',
            metaTitle: data.metaTitle || '',
            metaDescription: data.metaDescription || ''
          });
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
        this.showFeedback('Configurações salvas com sucesso!');
      },
      error: (err) => {
        this.isSaving = false;
        const msg = err?.error?.message || err?.message || 'Falha na comunicação com o servidor.';
        this.showFeedback(`Erro ao salvar: ${msg}`);
        console.error('Falha ao salvar configurações:', err);
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

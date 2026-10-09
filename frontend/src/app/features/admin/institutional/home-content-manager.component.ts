import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { HttpClient, HttpEventType } from '@angular/common/http';
import { environment } from '../../../../environments/environment';
import { HomeSettingsService, HomeSettings } from '../../../core/services/home-settings.service';

@Component({
  selector: 'app-home-content-manager',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, RouterLink],
  templateUrl: './home-content-manager.component.html',
  styleUrls: ['./home-content-manager.component.scss']
})
export class HomeContentManagerComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly http = inject(HttpClient);
  private readonly homeSettingsService = inject(HomeSettingsService);

  readonly DEFAULT_VIDEO_URL = '/assets/videos/wb-presentation.mp4';
  readonly DEFAULT_POSTER_URL = '/assets/images/hero-poster.jpg';
  readonly LOGO_POSTER_URL = '/assets/images/logo-wb-agency.jpeg';

  readonly form: FormGroup = this.fb.group({
    heroTitle: ['', [Validators.maxLength(80)]],
    heroSubtitle: ['', [Validators.maxLength(120)]],
    scrollLabel: ['SCROLL'],
    metaTitle: [''],
    metaDescription: [''],

    // Disclaimer
    disclaimerActive: [false],
    disclaimerTitle: [''],
    disclaimerText: [''],
    disclaimerLinkUrl: [''],
    disclaimerLinkLabel: [''],

    // Duração do splash em milissegundos
    splashDurationMs: [1000, [Validators.min(300), Validators.max(10000)]]
  });

  // Controle de origem das mídias
  videoSourceType: 'default' | 'external' | 'upload' = 'default';
  posterSourceType: 'default' | 'external' | 'upload' = 'default';

  currentVideoUrl = this.DEFAULT_VIDEO_URL;
  currentPosterUrl = this.DEFAULT_POSTER_URL;
  externalVideoInput = '';
  externalPosterInput = '';

  videoHasError = false;
  posterHasError = false;

  isSaving = false;
  isVideoUploading = false;
  isPosterUploading = false;
  videoUploadProgress = 0;
  posterUploadProgress = 0;
  feedbackMessage = '';

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.homeSettingsService.getAdminSettings().subscribe({
      next: (data) => {
        if (data) {
          this.form.patchValue({
            heroTitle: data.heroTitle || 'WB AGENCY',
            heroSubtitle: data.heroSubtitle || 'EDITORIAL & HIGH FASHION SCOUTING',
            scrollLabel: data.scrollLabel || 'SCROLL',
            metaTitle: data.metaTitle || '',
            metaDescription: data.metaDescription || '',
            disclaimerActive: !!data.disclaimerActive,
            disclaimerTitle: data.disclaimerTitle || '',
            disclaimerText: data.disclaimerText || '',
            disclaimerLinkUrl: data.disclaimerLinkUrl || '',
            disclaimerLinkLabel: data.disclaimerLinkLabel || '',
            splashDurationMs: data.splashDurationMs || 1000
          });

          // Define vídeo vigente
          const video = data.videoUrl?.trim();
          if (!video || video === this.DEFAULT_VIDEO_URL || video.endsWith('wb-presentation.mp4')) {
            this.videoSourceType = 'default';
            this.currentVideoUrl = this.DEFAULT_VIDEO_URL;
          } else {
            this.videoSourceType = 'external';
            this.currentVideoUrl = video;
            this.externalVideoInput = video;
          }

          // Define poster vigente
          const poster = data.posterUrl?.trim() || data.bannerImageUrl?.trim();
          if (!poster || poster === this.DEFAULT_POSTER_URL || poster.endsWith('hero-poster.jpg')) {
            this.posterSourceType = 'default';
            this.currentPosterUrl = this.DEFAULT_POSTER_URL;
          } else if (poster === this.LOGO_POSTER_URL || poster.endsWith('logo-wb-agency.jpeg')) {
            this.posterSourceType = 'default';
            this.currentPosterUrl = this.LOGO_POSTER_URL;
          } else {
            this.posterSourceType = 'external';
            this.currentPosterUrl = poster;
            this.externalPosterInput = poster;
          }
        }
      },
      error: (err) => {
        this.showFeedback('Erro ao carregar configurações da Home do servidor.');
        console.error('Falha ao carregar configurações da Home:', err);
      }
    });
  }

  selectVideoType(type: 'default' | 'external' | 'upload'): void {
    this.videoSourceType = type;
    this.videoHasError = false;
    if (type === 'default') {
      this.currentVideoUrl = this.DEFAULT_VIDEO_URL;
    } else if (type === 'external' && this.externalVideoInput.trim()) {
      this.currentVideoUrl = this.externalVideoInput.trim();
    }
  }

  onExternalVideoChange(): void {
    this.videoHasError = false;
    const url = this.externalVideoInput.trim();
    if (url) {
      this.currentVideoUrl = url;
    } else {
      this.currentVideoUrl = this.DEFAULT_VIDEO_URL;
    }
  }

  onVideoError(): void {
    this.videoHasError = true;
    console.warn('[HOME-HERO] Falha ao carregar vídeo configurado. Aplicando fallback local.');
  }

  selectPosterType(type: 'default' | 'external' | 'upload'): void {
    this.posterSourceType = type;
    this.posterHasError = false;
    if (type === 'default') {
      this.currentPosterUrl = this.DEFAULT_POSTER_URL;
    } else if (type === 'external' && this.externalPosterInput.trim()) {
      this.currentPosterUrl = this.externalPosterInput.trim();
    }
  }

  selectDefaultPoster(option: 'poster' | 'logo'): void {
    this.posterSourceType = 'default';
    this.posterHasError = false;
    this.currentPosterUrl = option === 'poster' ? this.DEFAULT_POSTER_URL : this.LOGO_POSTER_URL;
  }

  onExternalPosterChange(): void {
    this.posterHasError = false;
    const url = this.externalPosterInput.trim();
    if (url) {
      this.currentPosterUrl = url;
    } else {
      this.currentPosterUrl = this.DEFAULT_POSTER_URL;
    }
  }

  onPosterError(): void {
    this.posterHasError = true;
    console.warn('[HOME-HERO] Falha ao carregar poster configurado. Aplicando fallback local.');
  }

  setSplashDuration(ms: number): void {
    this.form.patchValue({ splashDurationMs: ms });
  }

  saveTextContent(): void {
    if (this.form.invalid) return;

    this.isSaving = true;
    const finalVideoUrl = this.videoHasError ? this.DEFAULT_VIDEO_URL : (this.currentVideoUrl || this.DEFAULT_VIDEO_URL);
    const finalPosterUrl = this.posterHasError ? this.DEFAULT_POSTER_URL : (this.currentPosterUrl || this.DEFAULT_POSTER_URL);

    const payload: Partial<HomeSettings> = {
      ...this.form.value,
      splashDurationMs: Number(this.form.value.splashDurationMs) || 1000,
      videoUrl: finalVideoUrl,
      posterUrl: finalPosterUrl,
      bannerImageUrl: finalPosterUrl
    };

    this.homeSettingsService.updateSettings(payload).subscribe({
      next: (res) => {
        this.isSaving = false;
        if (res) {
          this.form.patchValue(res);
        }
        this.showFeedback('Configurações da Home salvas com sucesso!');
        try { alert('Configurações e Aviso/Disclaimer da Home salvos com sucesso!'); } catch {}
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
              this.videoSourceType = 'upload';
              this.videoHasError = false;
            }
            this.isVideoUploading = false;
            this.showFeedback('Vídeo Hero enviado com sucesso!');
          }
        },
        error: () => {
          this.isVideoUploading = false;
          this.showFeedback('Falha no upload do vídeo para o storage.');
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

      this.isPosterUploading = true;
      this.posterUploadProgress = 0;

      this.http.put<any>(`${environment.apiUrl}/admin/institutional/home/poster`, formData, {
        reportProgress: true,
        observe: 'events'
      }).subscribe({
        next: (ev) => {
          if (ev.type === HttpEventType.UploadProgress && ev.total) {
            this.posterUploadProgress = Math.round((100 * ev.loaded) / ev.total);
          } else if (ev.type === HttpEventType.Response) {
            if (ev.body && ev.body.posterUrl) {
              this.currentPosterUrl = ev.body.posterUrl;
              this.posterSourceType = 'upload';
              this.posterHasError = false;
            }
            this.isPosterUploading = false;
            this.showFeedback('Poster de contingência atualizado com sucesso!');
          }
        },
        error: () => {
          this.isPosterUploading = false;
          this.showFeedback('Falha no upload do poster para o storage.');
        }
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

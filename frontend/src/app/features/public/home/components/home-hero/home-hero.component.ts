import { Component, OnInit, OnDestroy, ViewChild, ElementRef, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HomeSettingsService, HomeSettings } from '../../../../../core/services/home-settings.service';

@Component({
  selector: 'app-home-hero',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './home-hero.component.html',
  styleUrls: ['./home-hero.component.scss']
})
export class HomeHeroComponent implements OnInit, OnDestroy {
  @ViewChild('heroVideo') heroVideoRef?: ElementRef<HTMLVideoElement>;

  private readonly homeSettingsService = inject(HomeSettingsService);

  content: HomeSettings | null = null;
  videoUrl: string = 'assets/videos/wb-presentation.mp4';
  posterUrl: string = 'assets/images/hero-poster.jpg';
  showDisclaimerModal: boolean = true;

  showSplashLogo = signal<boolean>(true);
  isMuted = signal<boolean>(true);

  // Limite máximo interno de volume do player (5% do ganho)
  readonly TARGET_VOLUME: number = 0.05;
  private timerId?: any;
  private fadeInterval?: any;
  private splashStartTime: number = 0;
  private splashDurationMs: number = 1000;

  ngOnInit(): void {
    // Recupera duração configurada prévia do localStorage para evitar delay de rede
    const cachedDuration = typeof localStorage !== 'undefined' ? localStorage.getItem('wb_splash_duration_ms') : null;
    this.splashDurationMs = cachedDuration ? (Number(cachedDuration) || 1000) : 1000;
    this.splashStartTime = Date.now();

    // Inicia o splash com a duração prevista
    this.timerId = setTimeout(() => {
      this.showSplashLogo.set(false);
    }, this.splashDurationMs);

    this.loadHomeData();
  }

  loadHomeData(): void {
    this.homeSettingsService.getPublicSettings().subscribe({
      next: (data) => {
        if (data) {
          this.content = data;
          if (data.videoUrl?.trim()) {
            this.videoUrl = data.videoUrl.trim();
          }
          if (data.posterUrl?.trim() || data.bannerImageUrl?.trim()) {
            this.posterUrl = (data.posterUrl || data.bannerImageUrl)!.trim();
          }

          // Ajusta a duração do splash caso configurada no servidor
          if (data.splashDurationMs && data.splashDurationMs > 0) {
            this.splashDurationMs = data.splashDurationMs;
            try {
              if (typeof localStorage !== 'undefined') {
                localStorage.setItem('wb_splash_duration_ms', String(data.splashDurationMs));
              }
            } catch {}

            // Se o splash ainda estiver ativo, recalcula o tempo restante exato
            if (this.showSplashLogo()) {
              if (this.timerId) {
                clearTimeout(this.timerId);
              }
              const elapsed = Date.now() - this.splashStartTime;
              const remaining = Math.max(0, this.splashDurationMs - elapsed);
              this.timerId = setTimeout(() => {
                this.showSplashLogo.set(false);
              }, remaining);
            }
          }
        }
      },
      error: (err) => {
        console.warn('Não foi possível carregar configurações personalizadas da Home, usando padrões.', err);
      }
    });
  }

  ngOnDestroy(): void {
    if (this.timerId) {
      clearTimeout(this.timerId);
    }
    if (this.fadeInterval) {
      clearInterval(this.fadeInterval);
      this.fadeInterval = null;
    }
  }

  toggleAudio(): void {
    const video = this.heroVideoRef?.nativeElement;
    if (!video) return;

    if (this.fadeInterval) {
      clearInterval(this.fadeInterval);
      this.fadeInterval = null;
    }

    if (this.isMuted()) {
      video.muted = false;
      this.isMuted.set(false);
      let current = 0;
      video.volume = current;

      if (video.paused) {
        video.play().catch(() => {});
      }

      this.fadeInterval = setInterval(() => {
        if (current < this.TARGET_VOLUME) {
          current = Math.min(this.TARGET_VOLUME, current + 0.02);
          video.volume = Number(current.toFixed(2));
        } else {
          if (this.fadeInterval) {
            clearInterval(this.fadeInterval);
            this.fadeInterval = null;
          }
        }
      }, 50);
    } else {
      video.muted = true;
      video.volume = 0;
      this.isMuted.set(true);
    }
  }

  scrollToContent(): void {
    if (typeof window !== 'undefined') {
      window.scrollTo({
        top: window.innerHeight,
        behavior: 'smooth'
      });
    }
  }
}

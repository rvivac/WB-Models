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

  ngOnInit(): void {
    // Exibe o splash por exatamente 2 segundos e encerra
    this.timerId = setTimeout(() => {
      this.showSplashLogo.set(false);
    }, 2000);

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

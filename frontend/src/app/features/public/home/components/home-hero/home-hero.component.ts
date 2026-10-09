import { Component, OnInit, OnDestroy, ViewChild, ElementRef, signal } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-home-hero',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './home-hero.component.html',
  styleUrls: ['./home-hero.component.scss']
})
export class HomeHeroComponent implements OnInit, OnDestroy {
  @ViewChild('heroVideo') heroVideoRef?: ElementRef<HTMLVideoElement>;

  showSplashLogo = signal<boolean>(true);
  isMuted = signal<boolean>(true);

  // Limite máximo interno de volume do player (5% do ganho)
  readonly TARGET_VOLUME: number = 0.05;
  private timerId?: any;
  private fadeInterval?: any;

  ngOnInit(): void {
    // Exibe o splash por exatamente 1 segundo e encerra
    this.timerId = setTimeout(() => {
      this.showSplashLogo.set(false);
    }, 1000);
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

import { Component, OnInit, inject, signal, effect, ElementRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { PublicContentService, HomeHeroPayload } from '../../../../../core/services/public-content.service';
import { TranslationService } from '../../../../../core/services/translation.service';
import { TranslatePipe } from '../../../../../shared/pipes/translate.pipe';

@Component({
  selector: 'app-home-hero',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslatePipe],
  templateUrl: './home-hero.component.html',
  styleUrls: ['./home-hero.component.scss']
})
export class HomeHeroComponent implements OnInit {
  private readonly publicContentService = inject(PublicContentService);
  private readonly translationService = inject(TranslationService);

  @ViewChild('heroVideo') videoRef?: ElementRef<HTMLVideoElement>;

  readonly heroData = signal<HomeHeroPayload>({
    videoUrl: 'https://assets.mixkit.co/videos/preview/mixkit-fashion-model-in-neon-light-40156-large.mp4',
    posterImageUrl: 'https://images.unsplash.com/photo-1509631179647-0177331693ae?q=80&w=1920&auto=format&fit=crop',
    title: 'WB SCOUTING',
    subtitle: 'Conectando os rostos mais autênticos e promissores às principais passarelas, campanhas globais e produções editoriais de alto padrão.',
    ctaText: 'Ver Casting',
    ctaLink: '/models/female'
  });

  readonly isVideoError = signal<boolean>(false);
  readonly isVideoLoaded = signal<boolean>(false);

  constructor() {
    // Reage dinamicamente a mudanças de idioma
    effect(() => {
      const currentLang = this.translationService.currentLang();
      this.loadHero(currentLang);
    });
  }

  ngOnInit(): void {
    // Carregamento inicial garantido
    this.loadHero(this.translationService.currentLang());
  }

  loadHero(lang: string): void {
    this.publicContentService.getHeroContent(lang).subscribe({
      next: (data) => {
        if (data) {
          this.heroData.set(data);
          // Tentar reproduzir vídeo se o elemento estiver disponível
          setTimeout(() => this.attemptAutoplay(), 100);
        }
      },
      error: () => {
        this.isVideoError.set(true);
      }
    });
  }

  onVideoLoaded(): void {
    this.isVideoLoaded.set(true);
  }

  onVideoError(): void {
    console.warn('Erro ao carregar vídeo hero institucional, ativando fallback de poster.');
    this.isVideoError.set(true);
  }

  attemptAutoplay(): void {
    if (this.videoRef?.nativeElement) {
      const video = this.videoRef.nativeElement;
      video.muted = true;
      video.playsInline = true;
      const playPromise = video.play();
      if (playPromise !== undefined) {
        playPromise.catch(() => {
          // Autoplay prevenido pelo navegador, poster permanece visível
          console.info('Autoplay do vídeo prevenido pelo navegador.');
        });
      }
    }
  }

  scrollToContent(): void {
    if (typeof window !== 'undefined') {
      window.scrollTo({
        top: window.innerHeight - 80,
        behavior: 'smooth'
      });
    }
  }
}

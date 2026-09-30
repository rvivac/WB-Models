import { Injectable, inject } from '@angular/core';
import { Observable, catchError, map, of } from 'rxjs';
import { ApiService } from './api.service';

export interface SiteContentPublicDto {
  sectionKey: string;
  payload: Record<string, any>;
  mediaUrls?: Record<string, string>;
  lang: string;
}

export interface HomeHeroPayload {
  videoUrl?: string;
  posterImageUrl?: string;
  title?: string;
  subtitle?: string;
  ctaText?: string;
  ctaLink?: string;
}

export interface AboutManifestoPayload {
  headline: string;
  quote: string;
  body: string;
}

export interface ContactChannelsPublicDto {
  email: string;
  whatsappNumber: string;
  whatsappUrl: string;
  instagramHandle: string;
  address: string;
  officeHours: string;
}

@Injectable({
  providedIn: 'root'
})
export class PublicContentService {
  private readonly api = inject(ApiService);

  private readonly defaultHero: HomeHeroPayload = {
    videoUrl: 'assets/videos/wb-presentation.mp4',
    posterImageUrl: 'assets/images/hero-poster.jpg',
    title: 'High Fashion & Scouting',
    subtitle: 'Gestão de Carreiras • Scouting Internacional',
    ctaText: 'Ver Elenco',
    ctaLink: '/models/female'
  };

  private readonly defaultContactChannels: ContactChannelsPublicDto = {
    email: 'contato@wbscouting.com',
    whatsappNumber: '+55 (11) 99999-9999',
    whatsappUrl: 'https://wa.me/5511999999999',
    instagramHandle: '@wbscouting',
    address: 'São Paulo - SP, Brasil',
    officeHours: 'Segunda a Sexta, das 09h às 18h'
  };

  getHeroContent(lang: string = 'pt'): Observable<HomeHeroPayload> {
    return this.api.get<SiteContentPublicDto>('/public/content/HOME_HERO', { lang }).pipe(
      map(response => {
        const payload = response?.payload || {};
        const rawVideo = payload['videoUrl'] || this.defaultHero.videoUrl;
        const rawPoster = payload['posterImageUrl'] || this.defaultHero.posterImageUrl;
        return {
          videoUrl: rawVideo ? rawVideo.replace(/^\/assets\//, 'assets/') : this.defaultHero.videoUrl,
          posterImageUrl: rawPoster ? rawPoster.replace(/^\/assets\//, 'assets/') : this.defaultHero.posterImageUrl,
          title: payload['title'] || this.defaultHero.title,
          subtitle: payload['subtitle'] || this.defaultHero.subtitle,
          ctaText: payload['ctaText'] || this.defaultHero.ctaText,
          ctaLink: payload['ctaLink'] || this.defaultHero.ctaLink
        };
      }),
      catchError(err => {
        console.warn('Falha ao carregar HOME_HERO da API, utilizando defaults editoriais:', err);
        return of(this.defaultHero);
      })
    );
  }

  getContactChannels(lang: string = 'pt'): Observable<ContactChannelsPublicDto> {
    return this.api.get<ContactChannelsPublicDto>('/public/contact-channels', { lang }).pipe(
      catchError(err => {
        console.warn('Falha ao carregar canais de contato da API, utilizando defaults:', err);
        return of(this.defaultContactChannels);
      })
    );
  }

  getContent(sectionKey: string, lang: string = 'pt'): Observable<SiteContentPublicDto | null> {
    return this.api.get<SiteContentPublicDto>(`/public/content/${sectionKey}`, { lang }).pipe(
      catchError(err => {
        console.warn(`Falha ao carregar seção ${sectionKey}:`, err);
        return of(null);
      })
    );
  }

  private readonly defaultAboutManifestoPt: AboutManifestoPayload = {
    headline: 'A Nova Estética do Scouting Global',
    quote: 'A beleza contemporânea nasce da singularidade e precisão.',
    body: 'A WB Agency consolidou-se como um núcleo editorial focado no desenvolvimento integral de modelos para os principais mercados da moda internacional. Nossa metodologia rejeita a padronização e prioriza a identidade visual autêntica, conectando talentos a marcas com relevância estética global.'
  };

  private readonly defaultAboutManifestoEn: AboutManifestoPayload = {
    headline: 'The New Aesthetic of Global Scouting',
    quote: 'Contemporary beauty stems from uniqueness and precision.',
    body: 'WB Agency has established itself as an editorial powerhouse dedicated to the comprehensive development of models for premier global fashion markets. Our scouting methodology moves beyond mass standards to foster authentic personal identity, positioning talents at the intersection of high fashion and international relevance.'
  };

  getAboutManifestoContent(lang: string = 'pt'): Observable<AboutManifestoPayload> {
    const isEn = lang?.toLowerCase().startsWith('en');
    const fallback = isEn ? this.defaultAboutManifestoEn : this.defaultAboutManifestoPt;

    return this.getContent('ABOUT_MANIFESTO', lang).pipe(
      map(res => {
        const payload = res?.payload;
        if (!payload) return fallback;
        return {
          headline: payload['headline'] || fallback.headline,
          quote: payload['quote'] || fallback.quote,
          body: payload['body'] || fallback.body
        };
      }),
      catchError(() => of(fallback))
    );
  }
}

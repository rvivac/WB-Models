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
}

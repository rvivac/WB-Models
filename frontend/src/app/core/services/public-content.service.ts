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
    videoUrl: 'https://assets.mixkit.co/videos/preview/mixkit-fashion-model-in-neon-light-40156-large.mp4',
    posterImageUrl: 'https://images.unsplash.com/photo-1509631179647-0177331693ae?q=80&w=1920&auto=format&fit=crop',
    title: 'WB SCOUTING',
    subtitle: 'Conectando os rostos mais autênticos e promissores às principais passarelas, campanhas globais e produções editoriais de alto padrão.',
    ctaText: 'Ver Casting',
    ctaLink: '/models/female'
  };

  private readonly defaultContactChannels: ContactChannelsPublicDto = {
    email: 'contato@wbscouting.com',
    whatsappNumber: '5511999999999',
    whatsappUrl: 'https://wa.me/5511999999999?text=Ol%C3%A1%21%20Gostaria%20de%20mais%20informa%C3%A7%C3%B5es%20sobre%20a%20ag%C3%AAncia%20WB%20Scouting.',
    instagramHandle: '@wbscouting',
    address: 'São Paulo - SP, Brasil',
    officeHours: 'Segunda a Sexta, das 09h às 18h'
  };

  getHeroContent(lang: string = 'pt'): Observable<HomeHeroPayload> {
    return this.api.get<SiteContentPublicDto>('/public/content/HOME_HERO', { lang }).pipe(
      map(response => {
        const payload = response?.payload || {};
        return {
          videoUrl: payload['videoUrl'] || this.defaultHero.videoUrl,
          posterImageUrl: payload['posterImageUrl'] || this.defaultHero.posterImageUrl,
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

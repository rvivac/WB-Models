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
  description?: string;
  ctaText?: string;
  ctaLink?: string;
}

export interface AboutManifestoPayload {
  headline: string;
  quote: string;
  body: string;
}

export interface ApplyHowItWorksPayload {
  headline: string;
  quote: string;
  /** Passos (parse do body.split('|||')) do campo body do backend */
  steps: string[];
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
    email: 'info@wbagency.com.br',
    whatsappNumber: '5511970656003',
    whatsappUrl: 'https://wa.me/5511970656003',
    instagramHandle: '@wbagency',
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
    return this.api.get<any>('/contact-channels').pipe(
      map(res => {
        if (Array.isArray(res) && res.length > 0) {
          let email = this.defaultContactChannels.email;
          let whatsappNumber = this.defaultContactChannels.whatsappNumber;
          let whatsappUrl = this.defaultContactChannels.whatsappUrl;
          let instagramHandle = this.defaultContactChannels.instagramHandle;
          let address = this.defaultContactChannels.address;
          let officeHours = this.defaultContactChannels.officeHours;

          for (const ch of res) {
            const type = (ch.type || '').toUpperCase();
            const val = ch.value || '';
            if (type === 'EMAIL' && val) email = val;
            if (type === 'WHATSAPP' && val) {
              whatsappNumber = val.replace(/\D/g, '');
              whatsappUrl = `https://wa.me/${whatsappNumber}`;
            }
            if (type === 'INSTAGRAM' && val) instagramHandle = val;
            if (type === 'ADDRESS' && val) address = val;
            if (type === 'OFFICE_HOURS' && val) officeHours = val;
          }
          return { email, whatsappNumber, whatsappUrl, instagramHandle, address, officeHours };
        }
        if (res && res.email) {
          return res as ContactChannelsPublicDto;
        }
        return this.defaultContactChannels;
      }),
      catchError(() => {
        return this.api.get<ContactChannelsPublicDto>('/public/contact-channels', { lang }).pipe(
          catchError(() => of(this.defaultContactChannels))
        );
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

  // 🆕 Padrões do texto Próximos Passos Apply (Como Funciona) — fallback quando API não retorna
  private readonly defaultApplyHowItWorksPt: ApplyHowItWorksPayload = {
    headline: 'Próximos Passos • Como Funciona',
    quote: 'Transparência total no processo de avaliação de novos talentos.',
    steps: [
      'Nossa diretoria de casting analisa todas as candidaturas em até 5 dias úteis.',
      'Em caso de compatibilidade de perfil com nosso casting comercial ou fashion, nossa equipe entrará em contato via telefone ou e-mail cadastrado.',
      'A WB Agency nunca cobra taxas para avaliação de perfil ou agenciamento inicial.'
    ]
  };
  private readonly defaultApplyHowItWorksEn: ApplyHowItWorksPayload = {
    headline: 'Next Steps • How It Works',
    quote: 'Full transparency throughout our new talent evaluation workflow.',
    steps: [
      'Our casting board reviews every submission within 5 business days.',
      'When your profile matches our commercial or high fashion rosters, our scouting team contacts you via the phone or email you registered.',
      'WB Agency never charges assessment fees or upfront agency deposits of any kind.'
    ]
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

  // 🆕 Helper para o texto Proximos Passos / Como Funciona do Apply (Bilingual CMS editavel)
  // Retorna headline + quote + steps[] (array pronto para *ngFor <ul><li>).
  getApplyHowItWorksContent(lang: string = 'pt'): Observable<ApplyHowItWorksPayload> {
    const isEn = lang?.toLowerCase().startsWith('en');
    const fallback = isEn ? this.defaultApplyHowItWorksEn : this.defaultApplyHowItWorksPt;

    return this.getContent('APPLY_HOW_IT_WORKS', lang).pipe(
      map(res => {
        const payload = res?.payload;
        if (!payload) return fallback;
        const headline = payload['headline'] || fallback.headline;
        const quote = payload['quote'] || fallback.quote;
        const rawBody = (payload['body'] || '') as string;
        const steps = rawBody.length > 0 && rawBody.includes('|||')
          ? rawBody.split('|||').map(s => s.trim()).filter(Boolean)
          : (rawBody.length > 0 ? [rawBody] : fallback.steps);
        return { headline, quote, steps };
      }),
      catchError(() => of(fallback))
    );
  }
}

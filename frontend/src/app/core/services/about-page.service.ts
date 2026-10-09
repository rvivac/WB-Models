import { Injectable, inject } from '@angular/core';
import { Observable, catchError, of } from 'rxjs';
import { ApiService } from './api.service';
import { AboutPage } from '../../shared/models/about-page.interface';

@Injectable({
  providedIn: 'root'
})
export class AboutPageService {
  private readonly api = inject(ApiService);

  public readonly defaultAboutPage: AboutPage = {
    title: 'A Nova Estética do Scouting Global',
    subtitle: 'MANIFESTO INSTITUCIONAL',
    description: 'A WB Agency consolidou-se como um núcleo editorial focado no desenvolvimento integral de modelos para os principais mercados da moda internacional. Nossa metodologia rejeita a padronização e prioriza a identidade visual autêntica, conectando talentos a marcas com relevância estética global.',
    heroQuote: 'Acreditamos na autenticidade, na força da personalidade e na beleza singular de cada indivíduo.',
    manifestoTitle: 'Nossa Filosofia',
    manifestoText: 'Conectamos talentos às principais marcas com curadoria estratégica, visão de vanguarda e compromisso com o desenvolvimento humano e profissional em escala global.',
    pillarsTitle: 'Nossos Pilares & Valores',
    pillars: [
      {
        order: 1,
        titulo: 'Curadoria & Autenticidade',
        descricao: 'Descoberta e representação de perfis singulares com identidade própria e alto potencial editorial.'
      },
      {
        order: 2,
        titulo: 'Transparência & Ética',
        descricao: 'Relações comerciais claras e respeito irrestrito aos contratos, imagem e bem-estar de cada modelo.'
      },
      {
        order: 3,
        titulo: 'Desenvolvimento de Carreira',
        descricao: 'Orientação contínua, construção de portfólio de alto nível e preparação para passarelas e campanhas.'
      },
      {
        order: 4,
        titulo: 'Alcance & Conexões',
        descricao: 'Pontes estratégicas com as principais agências parceiras, diretores de casting e marcas mundiais.'
      }
    ],
    seo: {
      metaTitle: 'Sobre Nós | WB Agency - Scouting & Model Management',
      metaDescription: 'Conheça a WB Agency, agência de modelos e scouting internacional focada na autenticidade, excelência editorial e gestão de carreiras globais.'
    }
  };

  getPublicAboutPage(): Observable<AboutPage> {
    return this.api.get<AboutPage>('/public/institutional/about');
  }

  getAboutContent(lang?: string): Observable<any> {
    const isEn = lang?.toLowerCase().startsWith('en');
    const path = isEn ? '/public/institutional/about?lang=en' : '/public/institutional/about';
    return this.api.get<any>(path);
  }

  getAdminAboutPage(): Observable<AboutPage> {
    return this.api.get<AboutPage>('/admin/institutional/about');
  }

  updateAboutPage(dto: AboutPage): Observable<AboutPage> {
    return this.api.put<AboutPage>('/admin/institutional/about', dto);
  }
}

import { Component, OnInit, inject, signal, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { TranslatePipe } from '../../../shared/pipes/translate.pipe';
import { PublicContentService, AboutManifestoPayload } from '../../../core/services/public-content.service';
import { TranslationService } from '../../../core/services/translation.service';
import { AboutPageService } from '../../../core/services/about-page.service';
import { AboutPillar } from '../../../shared/models/about-page.interface';

@Component({
  selector: 'app-about',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslatePipe],
  templateUrl: './about.component.html',
  styleUrls: ['./about.component.scss']
})
export class AboutComponent implements OnInit {
  private readonly publicContentService = inject(PublicContentService);
  private readonly translationService = inject(TranslationService);
  private readonly aboutPageService = inject(AboutPageService, { optional: true });

  readonly aboutContent = signal<AboutManifestoPayload>({
    headline: 'A Nova Estética do Scouting Global',
    quote: 'A beleza contemporânea nasce da singularidade e precisão.',
    body: 'A WB Agency consolidou-se como um núcleo editorial focado no desenvolvimento integral de modelos para os principais mercados da moda internacional. Nossa metodologia rejeita a padronização e prioriza a identidade visual autêntica, conectando talentos a marcas com relevância estética global.'
  });

  readonly pillars = signal<AboutPillar[]>([
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
  ]);

  constructor() {
    effect(() => {
      const lang = this.translationService.currentLang();
      this.loadAboutContent(lang);
    });
  }

  ngOnInit(): void {
    this.loadAboutContent(this.translationService.currentLang());
  }

  loadAboutContent(lang: string): void {
    if (this.aboutPageService) {
      this.aboutPageService.getPublicAboutPage().subscribe({
        next: (page) => {
          if (page) {
            this.aboutContent.set({
              headline: page.title || this.aboutContent().headline,
              quote: page.heroQuote || this.aboutContent().quote,
              body: page.manifestoText || this.aboutContent().body
            });
            if (page.pillars && page.pillars.length > 0) {
              this.pillars.set(page.pillars);
            }
          }
        },
        error: (err) => console.warn('Falha ao carregar Sobre Nós institucional:', err)
      });
    }

    this.publicContentService.getAboutManifestoContent(lang).subscribe({
      next: (content) => {
        if (content) {
          this.aboutContent.set(content);
        }
      },
      error: (err) => console.warn('Falha ao carregar manifesto CMS:', err)
    });
  }
}

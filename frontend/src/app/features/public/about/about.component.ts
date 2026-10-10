import { Component, OnInit, inject, signal, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Title, Meta } from '@angular/platform-browser';
import { TranslatePipe } from '../../../shared/pipes/translate.pipe';
import { TranslationService } from '../../../core/services/translation.service';
import { AboutPageService } from '../../../core/services/about-page.service';
import { AboutPillar } from '../../../shared/models/about-page.interface';

export interface AboutContent {
  headline: string;
  quote: string;
  sectionTitle: string;
  body: string;
}

@Component({
  selector: 'app-about',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslatePipe],
  templateUrl: './about.component.html',
  styleUrls: ['./about.component.scss']
})
export class AboutComponent implements OnInit {
  private readonly aboutService = inject(AboutPageService);
  private readonly titleService = inject(Title);
  private readonly metaService = inject(Meta);
  private readonly translationService = inject(TranslationService);

  // Variável vinculada diretamente aos 4 campos da Seção Editorial
  content: AboutContent | null = null;

  // Alias retrocompatível com aboutData
  get aboutData(): any {
    return this.content;
  }

  // Sinais preservados para reatividade complementar e testes existentes
  readonly title = signal<string>('');
  readonly subtitle = signal<string>('');
  readonly description = signal<string>('');
  readonly heroQuote = signal<string>('');
  readonly sectionTitle = signal<string>('');
  readonly manifestoTitle = signal<string>('');
  readonly manifestoText = signal<string>('');
  readonly pillarsTitle = signal<string>('');
  readonly pillars = signal<AboutPillar[]>([]);

  readonly isLoading = signal<boolean>(true);
  readonly hasError = signal<boolean>(false);

  constructor() {
    effect(() => {
      const currentLang = this.translationService.currentLang();
      this.loadAboutData(currentLang);
    }, { allowSignalWrites: true });
  }

  ngOnInit(): void {
    // Carregamento reativo orquestrado pelo effect()
  }

  loadAboutData(lang: string = 'pt'): void {
    this.isLoading.set(true);
    this.hasError.set(false);

    const isEn = lang?.toLowerCase().startsWith('en');

    this.aboutService.getAboutContent(lang).subscribe({
      next: (data: any) => {
        if (data) {
          this.content = {
            headline: data.headline ?? data.pageTitle ?? data.title ?? '',
            quote: data.quote ?? data.heroQuote ?? '',
            sectionTitle: data.sectionTitle ?? data.manifestoTitle ?? '',
            body: data.body ?? data.manifestoText ?? data.bodyText ?? data.description ?? ''
          };

          this.title.set(this.content.headline);
          this.subtitle.set('');
          this.description.set(this.content.body);
          this.heroQuote.set(this.content.quote);
          this.sectionTitle.set(this.content.sectionTitle);
          this.manifestoTitle.set(this.content.sectionTitle);
          this.manifestoText.set(this.content.body);
          this.pillarsTitle.set(data.pillarsTitle || '');
          this.pillars.set(data.pillars || []);

          if (data.seo?.metaTitle) {
            this.titleService.setTitle(data.seo.metaTitle);
          }
          if (data.seo?.metaDescription) {
            this.metaService.updateTag({ name: 'description', content: data.seo.metaDescription });
          }
        }
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('[ABOUT] Erro ao carregar dados:', err);
        this.isLoading.set(false);
        this.hasError.set(true);
      }
    });
  }
}

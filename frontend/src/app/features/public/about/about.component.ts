import { Component, OnInit, inject, signal, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { TranslatePipe } from '../../../shared/pipes/translate.pipe';
import { PublicContentService, AboutManifestoPayload } from '../../../core/services/public-content.service';
import { TranslationService } from '../../../core/services/translation.service';

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

  readonly aboutContent = signal<AboutManifestoPayload>({
    headline: 'A Nova Estética do Scouting Global',
    quote: 'A beleza contemporânea nasce da singularidade e precisão.',
    body: 'A WB Agency consolidou-se como um núcleo editorial focado no desenvolvimento integral de modelos para os principais mercados da moda internacional. Nossa metodologia rejeita a padronização e prioriza a identidade visual autêntica, conectando talentos a marcas com relevância estética global.'
  });

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
    this.publicContentService.getAboutManifestoContent(lang).subscribe(content => {
      if (content) {
        this.aboutContent.set(content);
      }
    });
  }
}

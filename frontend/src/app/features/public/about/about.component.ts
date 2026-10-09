import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { Title, Meta } from '@angular/platform-browser';
import { TranslatePipe } from '../../../shared/pipes/translate.pipe';
import { environment } from '../../../../environments/environment';
import { AboutPage, AboutPillar } from '../../../shared/models/about-page.interface';

@Component({
  selector: 'app-about',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslatePipe],
  templateUrl: './about.component.html',
  styleUrls: ['./about.component.scss']
})
export class AboutComponent implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly titleService = inject(Title);
  private readonly metaService = inject(Meta);

  // Estados reativos (Signals) populados exclusivamente via backend
  readonly title = signal<string>('');
  readonly subtitle = signal<string>('');
  readonly description = signal<string>('');
  readonly heroQuote = signal<string>('');
  readonly manifestoTitle = signal<string>('');
  readonly manifestoText = signal<string>('');
  readonly pillarsTitle = signal<string>('');
  readonly pillars = signal<AboutPillar[]>([]);

  readonly isLoading = signal<boolean>(true);
  readonly hasError = signal<boolean>(false);

  ngOnInit(): void {
    this.loadAboutData();
  }

  loadAboutData(): void {
    this.isLoading.set(true);
    this.hasError.set(false);

    const endpoint = `${environment.apiUrl}/public/institutional/about`;

    this.http.get<AboutPage>(endpoint).subscribe({
      next: (page) => {
        if (page) {
          this.title.set(page.title || '');
          this.subtitle.set(page.subtitle || '');
          this.description.set(page.description || '');
          this.heroQuote.set(page.heroQuote || '');
          this.manifestoTitle.set(page.manifestoTitle || '');
          this.manifestoText.set(page.manifestoText || '');
          this.pillarsTitle.set(page.pillarsTitle || '');
          this.pillars.set(page.pillars || []);

          if (page.seo?.metaTitle) {
            this.titleService.setTitle(page.seo.metaTitle);
          }
          if (page.seo?.metaDescription) {
            this.metaService.updateTag({ name: 'description', content: page.seo.metaDescription });
          }
        }
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error(`Erro detalhado ao carregar dados da página Sobre Nós do backend (${endpoint}):`, err);
        this.isLoading.set(false);
        this.hasError.set(true);
      }
    });
  }
}

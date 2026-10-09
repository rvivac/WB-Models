import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { HomeSettingsService } from '../../../core/services/home-settings.service';

@Component({
  selector: 'app-footer',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './footer.component.html',
  styleUrls: ['./footer.component.scss']
})
export class FooterComponent implements OnInit {
  private readonly homeSettingsService = inject(HomeSettingsService);

  readonly currentYear: number = new Date().getFullYear();

  readonly footerDescription = signal<string>(
    'Agência de modelos e gestão internacional de talentos. Representação exclusiva, editorial e comercial com inteligência e inovação.'
  );
  readonly footerHubs = signal<string>('PARIS • MILAN • NEW YORK • SÃO PAULO');
  readonly footerPressBookingUrl = signal<string>('/contato');
  readonly footerApplyUrl = signal<string>('/apply');

  ngOnInit(): void {
    this.homeSettingsService.getPublicSettings().subscribe({
      next: (settings) => {
        if (settings) {
          if (settings.footerDescription) {
            this.footerDescription.set(settings.footerDescription);
          }
          if (settings.footerHubs) {
            this.footerHubs.set(settings.footerHubs);
          }
          if (settings.footerPressBookingUrl) {
            this.footerPressBookingUrl.set(settings.footerPressBookingUrl);
          }
          if (settings.footerApplyUrl) {
            this.footerApplyUrl.set(settings.footerApplyUrl);
          }
        }
      },
      error: () => {
        // Fallback estático já inicializado nos signals
      }
    });
  }
}

import { Component, input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-loading-spinner',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="spinner-container" [class.fullscreen]="fullscreen()">
      <div class="luxury-spinner"></div>
      @if (message()) {
        <p class="spinner-message tracking-editorial">{{ message() }}</p>
      }
    </div>
  `,
  styles: [`
    .spinner-container {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 2.5rem;
      gap: 1.25rem;

      &.fullscreen {
        position: fixed;
        inset: 0;
        background: rgba(7, 8, 11, 0.85);
        backdrop-filter: blur(12px);
        z-index: 9999;
      }
    }

    .luxury-spinner {
      width: 44px;
      height: 44px;
      border: 2px solid var(--border-subtle);
      border-top-color: var(--gold-light);
      border-radius: 50%;
      animation: spin 0.8s cubic-bezier(0.68, -0.55, 0.27, 1.55) infinite;
    }

    .spinner-message {
      font-size: 0.75rem;
      color: var(--gold-light);
      letter-spacing: 0.2em;
    }

    @keyframes spin {
      to { transform: rotate(360deg); }
    }
  `]
})
export class LoadingSpinnerComponent {
  readonly fullscreen = input<boolean>(false);
  readonly message = input<string>('Carregando...');
}

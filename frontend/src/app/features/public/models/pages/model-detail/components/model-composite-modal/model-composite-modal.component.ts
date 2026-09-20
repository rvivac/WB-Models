import { Component, HostListener, input, output, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { TranslatePipe } from '../../../../../../../shared/pipes/translate.pipe';

@Component({
  selector: 'app-model-composite-modal',
  standalone: true,
  imports: [CommonModule, TranslatePipe],
  templateUrl: './model-composite-modal.component.html',
  styleUrls: ['./model-composite-modal.component.scss']
})
export class ModelCompositeModalComponent {
  private readonly http = inject(HttpClient);

  readonly compositeUrl = input.required<string>();
  readonly modelName = input.required<string>();

  readonly close = output<void>();

  // Controles de Visualização e Zoom
  readonly zoomLevel = signal<number>(1);
  readonly panX = signal<number>(0);
  readonly panY = signal<number>(0);
  readonly isDragging = signal<boolean>(false);
  readonly isDownloading = signal<boolean>(false);

  private dragStartX = 0;
  private dragStartY = 0;

  // Detecção de tipo de arquivo (PDF vs Imagem)
  readonly isPdf = computed(() => {
    const url = this.compositeUrl()?.toLowerCase() || '';
    return url.endsWith('.pdf') || url.includes('.pdf?') || url.includes('/pdf/');
  });

  // Estilo dinâmico para zoom e pan
  readonly transformStyle = computed(() => {
    const z = this.zoomLevel();
    const x = this.panX();
    const y = this.panY();
    return `scale(${z}) translate(${x / z}px, ${y / z}px)`;
  });

  @HostListener('window:keydown', ['$event'])
  handleKeyDown(event: KeyboardEvent): void {
    if (event.key === 'Escape') {
      this.closeModal();
    } else if (event.key === '+' || event.key === '=') {
      this.zoomIn();
    } else if (event.key === '-') {
      this.zoomOut();
    } else if (event.key === '0') {
      this.resetZoom();
    }
  }

  closeModal(): void {
    this.close.emit();
  }

  onBackdropClick(event: MouseEvent): void {
    const target = event.target as HTMLElement;
    if (target.classList.contains('composite-modal-backdrop') || target.classList.contains('composite-viewport')) {
      this.closeModal();
    }
  }

  zoomIn(): void {
    const current = this.zoomLevel();
    if (current < 3) {
      this.zoomLevel.set(Math.min(3, Math.round((current + 0.25) * 100) / 100));
    }
  }

  zoomOut(): void {
    const current = this.zoomLevel();
    if (current > 0.75) {
      const next = Math.max(0.75, Math.round((current - 0.25) * 100) / 100);
      this.zoomLevel.set(next);
      if (next <= 1) {
        this.panX.set(0);
        this.panY.set(0);
      }
    }
  }

  resetZoom(): void {
    this.zoomLevel.set(1);
    this.panX.set(0);
    this.panY.set(0);
  }

  startDrag(event: MouseEvent): void {
    if (this.zoomLevel() <= 1) return;
    this.isDragging.set(true);
    this.dragStartX = event.clientX - this.panX();
    this.dragStartY = event.clientY - this.panY();
    event.preventDefault();
  }

  onDrag(event: MouseEvent): void {
    if (!this.isDragging()) return;
    this.panX.set(event.clientX - this.dragStartX);
    this.panY.set(event.clientY - this.dragStartY);
  }

  endDrag(): void {
    this.isDragging.set(false);
  }

  downloadComposite(): void {
    const url = this.compositeUrl();
    if (!url || this.isDownloading()) return;

    this.isDownloading.set(true);

    const safeName = (this.modelName() || 'Model').trim().replace(/[^a-zA-Z0-9_-]/g, '_');
    const extMatch = url.match(/\.(pdf|webp|jpe?g|png)(?:\?|$)/i);
    const ext = extMatch ? extMatch[1].toLowerCase() : (this.isPdf() ? 'pdf' : 'jpg');
    const filename = `Composite_${safeName}.${ext}`;

    // Tenta download direto via Blob para forçar o download sem abrir nova aba
    this.http.get(url, { responseType: 'blob' }).subscribe({
      next: (blob) => {
        const objectUrl = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = objectUrl;
        link.download = filename;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        window.URL.revokeObjectURL(objectUrl);
        this.isDownloading.set(false);
      },
      error: () => {
        // Fallback: se CORS no bucket impedir download direto via blob, abre a URL com atributo download
        const link = document.createElement('a');
        link.href = url;
        link.target = '_blank';
        link.download = filename;
        link.rel = 'noopener noreferrer';
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        this.isDownloading.set(false);
      }
    });
  }
}

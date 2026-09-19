import { Component, HostListener, input, output, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslatePipe } from '../../../../../../../shared/pipes/translate.pipe';

@Component({
  selector: 'app-model-lightbox',
  standalone: true,
  imports: [CommonModule, TranslatePipe],
  templateUrl: './model-lightbox.component.html',
  styleUrls: ['./model-lightbox.component.scss']
})
export class ModelLightboxComponent {
  readonly images = input.required<string[]>();
  readonly currentIndex = input.required<number>();

  readonly close = output<void>();
  readonly indexChange = output<number>();

  readonly currentImageUrl = computed(() => {
    const list = this.images();
    const idx = this.currentIndex();
    return list[idx] || '';
  });

  @HostListener('window:keydown', ['$event'])
  handleKeyDown(event: KeyboardEvent): void {
    if (event.key === 'Escape') {
      this.closeModal();
    } else if (event.key === 'ArrowLeft') {
      this.prevImage();
    } else if (event.key === 'ArrowRight') {
      this.nextImage();
    }
  }

  prevImage(): void {
    const total = this.images().length;
    if (total <= 1) return;
    const current = this.currentIndex();
    const newIndex = current > 0 ? current - 1 : total - 1;
    this.indexChange.emit(newIndex);
  }

  nextImage(): void {
    const total = this.images().length;
    if (total <= 1) return;
    const current = this.currentIndex();
    const newIndex = current < total - 1 ? current + 1 : 0;
    this.indexChange.emit(newIndex);
  }

  closeModal(): void {
    this.close.emit();
  }

  onBackdropClick(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('lightbox-backdrop')) {
      this.closeModal();
    }
  }
}

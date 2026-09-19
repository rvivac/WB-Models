import { Component, input, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ModelCardPublicDto } from '../../../../../core/services/public-model.service';
import { TranslatePipe } from '../../../../../shared/pipes/translate.pipe';

@Component({
  selector: 'app-model-card',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslatePipe],
  templateUrl: './model-card.component.html',
  styleUrls: ['./model-card.component.scss']
})
export class ModelCardComponent {
  readonly model = input.required<ModelCardPublicDto>();

  // Fallback de alta moda caso a imagem primária falhe ou esteja em branco
  readonly defaultCoverImage = 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?q=80&w=800&auto=format&fit=crop';
  readonly isImageError = signal<boolean>(false);

  getImageSrc(): string {
    if (this.isImageError()) {
      return this.defaultCoverImage;
    }
    const cover = this.model().coverImageUrl;
    return cover && cover.trim() !== '' ? cover : this.defaultCoverImage;
  }

  onImageError(): void {
    this.isImageError.set(true);
  }
}

import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CdkDragDrop, DragDropModule, moveItemInArray } from '@angular/cdk/drag-drop';
import { GalleryPhoto, PhotoCategory } from '../../../../../shared/models/gallery.model';

@Component({
  selector: 'app-photo-uploader-grid',
  standalone: true,
  imports: [CommonModule, DragDropModule],
  templateUrl: './photo-uploader-grid.component.html',
  styleUrls: ['./photo-uploader-grid.component.scss']
})
export class PhotoUploaderGridComponent {
  @Input() photos: GalleryPhoto[] = [];
  @Output() photosChange = new EventEmitter<GalleryPhoto[]>();
  @Output() fileUploaded = new EventEmitter<File[]>();
  @Output() validationError = new EventEmitter<string>();

  isDraggingOverZone = false;
  selectedCategoryFilter: 'ALL' | 'BOOK' | 'POLAROID' = 'ALL';

  readonly maxPhotos = 15;
  readonly maxSizeBytes = 10 * 1024 * 1024; // 10MB
  readonly allowedTypes = ['image/jpeg', 'image/png', 'image/webp'];

  onDropFiles(event: DragEvent): void {
    event.preventDefault();
    this.isDraggingOverZone = false;
    if (event.dataTransfer?.files) {
      this.processSelectedFiles(event.dataTransfer.files);
    }
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.isDraggingOverZone = true;
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    this.isDraggingOverZone = false;
  }

  onFileInputChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files) {
      this.processSelectedFiles(input.files);
      input.value = ''; // Reseta input para permitir mesmo arquivo se necessário
    }
  }

  processSelectedFiles(fileList: FileList): void {
    const validFiles: File[] = [];
    let hasInvalidType = false;
    let hasOversized = false;

    Array.from(fileList).forEach((file) => {
      if (!this.allowedTypes.includes(file.type)) {
        hasInvalidType = true;
        return;
      }
      if (file.size > this.maxSizeBytes) {
        hasOversized = true;
        return;
      }
      if (this.photos.length + validFiles.length >= this.maxPhotos) {
        this.validationError.emit(`Limite máximo de ${this.maxPhotos} imagens atingido.`);
        return;
      }
      validFiles.push(file);
    });

    if (hasInvalidType) {
      this.validationError.emit('Formato de arquivo não suportado. Apenas JPG, PNG ou WebP são permitidos.');
    }
    if (hasOversized) {
      this.validationError.emit('O tamanho do arquivo excede o limite máximo permitido de 10 MB.');
    }

    if (validFiles.length > 0) {
      this.fileUploaded.emit(validFiles);

      // Gera pré-visualização local imediata para feedback instantâneo
      validFiles.forEach((file) => {
        const objectUrl = URL.createObjectURL(file);
        const newPhoto: GalleryPhoto = {
          id: 'temp-' + Math.random().toString(36).substring(2, 9),
          url: objectUrl,
          category: 'BOOK',
          orderIndex: this.photos.length,
          isCover: this.photos.length === 0,
          file,
          uploadProgress: 100,
          isUploading: false
        };
        this.photos.push(newPhoto);
      });

      this.updateOrderIndices();
    }
  }

  onReorder(event: CdkDragDrop<GalleryPhoto[]>): void {
    moveItemInArray(this.photos, event.previousIndex, event.currentIndex);
    this.updateOrderIndices();
  }

  setAsCover(index: number): void {
    if (index >= 0 && index < this.photos.length) {
      const [target] = this.photos.splice(index, 1);
      this.photos.unshift(target);
      this.updateOrderIndices();
    }
  }

  removePhoto(index: number): void {
    if (index >= 0 && index < this.photos.length) {
      this.photos.splice(index, 1);
      this.updateOrderIndices();
    }
  }

  toggleCategory(photo: GalleryPhoto): void {
    photo.category = photo.category === 'BOOK' ? 'POLAROID' : 'BOOK';
    this.photosChange.emit(this.photos);
  }

  updateOrderIndices(): void {
    this.photos.forEach((photo, idx) => {
      photo.orderIndex = idx;
      photo.isCover = idx === 0;
    });
    this.photosChange.emit([...this.photos]);
  }
}

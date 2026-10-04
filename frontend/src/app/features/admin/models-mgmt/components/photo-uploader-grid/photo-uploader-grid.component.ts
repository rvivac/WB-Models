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

      // ============================================================
      // IMUTABILIDADE: Cria NOVO array com spread operator (NÃO muta input)
      // ============================================================
      const novas: GalleryPhoto[] = validFiles.map((file, idx) => ({
        id: 'temp-' + Math.random().toString(36).substring(2, 9),
        url: URL.createObjectURL(file),
        category: 'BOOK',
        orderIndex: this.photos.length + idx,
        isCover: this.photos.length === 0 && idx === 0,
        file,
        uploadProgress: 1,
        isUploading: true
      }));

      // Novo array (imutavel) + atualiza indices + emite para o pai:
      let novaLista = [...this.photos, ...novas];
      novaLista = this._applyOrderAndCover(novaLista);
      this.photosChange.emit(novaLista);
    }
  }

  onReorder(event: CdkDragDrop<GalleryPhoto[]>): void {
    let lista = [...this.photos];
    moveItemInArray(lista, event.previousIndex, event.currentIndex);
    lista = this._applyOrderAndCover(lista);
    this.photosChange.emit(lista);
  }

  setAsCover(index: number): void {
    if (index < 0 || index >= this.photos.length) return;
    let lista = [...this.photos];
    const [target] = lista.splice(index, 1);
    lista.unshift(target);
    lista = this._applyOrderAndCover(lista);
    this.photosChange.emit(lista);
  }

  removePhoto(index: number): void {
    if (index < 0 || index >= this.photos.length) return;
    let lista = this.photos.filter((_, i) => i !== index);
    lista = this._applyOrderAndCover(lista);
    this.photosChange.emit(lista);
  }

  toggleCategory(photo: GalleryPhoto): void {
    const idx = this.photos.findIndex(p => p.id === photo.id);
    if (idx < 0) return;
    let lista = [...this.photos];
    lista[idx] = { ...lista[idx], category: lista[idx].category === 'BOOK' ? 'POLAROID' : 'BOOK' };
    this.photosChange.emit(lista);
  }

  updateOrderIndices(): void {
    // Mantido apenas para compatibilidade com código antigo.
    // Use _applyOrderAndCover (imutavel) agora.
    this.photos.forEach((photo, idx) => {
      photo.orderIndex = idx;
      photo.isCover = idx === 0;
    });
    this.photosChange.emit([...this.photos]);
  }

  /**
   * Helper IMUTAVEL. Recebe lista, retorna NOVA lista com:
   * - orderIndex sequencial 0..N
   * - Exatamente UMA foto como isCover (sempre a de posicao 0)
   */
  private _applyOrderAndCover(lista: GalleryPhoto[]): GalleryPhoto[] {
    if (lista.length === 0) return [];
    const nova = lista.map((p, idx) => ({
      ...p,
      orderIndex: idx,
      isCover: idx === 0 ? true : false
    }));
    return nova;
  }

  /**
   * trackBy para *ngFor de fotos: previne que Angular recrie
   * TODO o DOM quando a galeria sofre pequenas alteracoes.
   * (Performance + evita "flicker" / desaparecimento de previews temporarios)
   */
  trackByPhotoId(index: number, item: GalleryPhoto): string {
    return item.id + '-' + (item.orderIndex ?? index);
  }
}

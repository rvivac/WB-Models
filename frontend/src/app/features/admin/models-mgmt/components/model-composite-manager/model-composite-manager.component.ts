import { Component, Input, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient, HttpEventType } from '@angular/common/http';
import { environment } from '../../../../../../environments/environment';

export interface ModelComposite {
  id: string;
  fileUrl: string;
  fileName: string;
  fileType: 'PDF' | 'IMAGE';
  fileSizeBytes: number;
  updatedAt: string;
}

@Component({
  selector: 'app-model-composite-manager',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './model-composite-manager.component.html',
  styleUrls: ['./model-composite-manager.component.scss']
})
export class ModelCompositeManagerComponent implements OnInit {
  @Input({ required: true }) modelId!: string;

  private readonly http = inject(HttpClient);
  composite: ModelComposite | null = null;
  isUploading = false;
  uploadProgress = 0;
  isDragging = false;
  errorMessage: string | null = null;

  private readonly allowedMimeTypes = ['application/pdf', 'image/jpeg', 'image/png', 'image/webp'];
  private readonly maxSizeBytes = 25 * 1024 * 1024; // 25 MB

  ngOnInit(): void {
    if (this.modelId) {
      this.loadComposite();
    }
  }

  loadComposite(): void {
    this.http.get<ModelComposite>(`${environment.apiUrl}/admin/models/${this.modelId}/composite`).subscribe({
      next: (data) => {
        this.composite = data;
        this.errorMessage = null;
      },
      error: () => {
        this.composite = null;
      }
    });
  }

  onFileSelected(event: Event): void {
    const target = event.target as HTMLInputElement;
    if (target.files && target.files.length > 0) {
      this.uploadComposite(target.files[0]);
      target.value = '';
    }
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.isDragging = false;
    if (event.dataTransfer?.files && event.dataTransfer.files.length > 0) {
      this.uploadComposite(event.dataTransfer.files[0]);
    }
  }

  onDragOver(e: DragEvent): void {
    e.preventDefault();
    this.isDragging = true;
  }

  onDragLeave(e: DragEvent): void {
    e.preventDefault();
    this.isDragging = false;
  }

  uploadComposite(file: File): void {
    this.errorMessage = null;

    if (!this.isValidFileType(file)) {
      alert('Formato de arquivo inválido. Formatos permitidos: PDF, JPG, PNG e WEBP.');
      return;
    }

    if (file.size > this.maxSizeBytes) {
      alert('O arquivo selecionado excede o limite máximo permitido de 25MB.');
      return;
    }

    const formData = new FormData();
    formData.append('file', file);

    this.isUploading = true;
    this.uploadProgress = 0;

    this.http.put<ModelComposite>(
      `${environment.apiUrl}/admin/models/${this.modelId}/composite`,
      formData,
      { reportProgress: true, observe: 'events' }
    ).subscribe({
      next: (event) => {
        if (event.type === HttpEventType.UploadProgress && event.total) {
          this.uploadProgress = Math.round((100 * event.loaded) / event.total);
        } else if (event.type === HttpEventType.Response) {
          this.composite = event.body;
          this.isUploading = false;
          this.uploadProgress = 0;
        }
      },
      error: () => {
        alert('Falha ao processar o upload do Composite. Tente novamente.');
        this.isUploading = false;
      }
    });
  }

  removeComposite(): void {
    if (confirm('Tem certeza de que deseja remover o Composite oficial deste modelo?')) {
      this.http.delete(`${environment.apiUrl}/admin/models/${this.modelId}/composite`).subscribe({
        next: () => {
          this.composite = null;
        },
        error: () => alert('Erro ao excluir composite.')
      });
    }
  }

  formatBytes(bytes: number): string {
    if (!bytes || bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
  }

  private isValidFileType(file: File): boolean {
    if (this.allowedMimeTypes.includes(file.type.toLowerCase())) {
      return true;
    }
    const name = file.name.toLowerCase();
    return name.endsWith('.pdf') || name.endsWith('.jpg') || name.endsWith('.jpeg') || name.endsWith('.png') || name.endsWith('.webp');
  }
}

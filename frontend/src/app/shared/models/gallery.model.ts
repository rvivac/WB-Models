export type PhotoCategory = 'BOOK' | 'POLAROID';

export interface GalleryPhoto {
  id?: string;
  url: string;
  storagePath?: string;
  category: PhotoCategory;
  orderIndex: number;
  isCover: boolean;
  file?: File;
  uploadProgress?: number;
  isUploading?: boolean;
}

export interface ModelComposite {
  id: string;
  fileUrl: string;
  fileName: string;
  fileType: 'PDF' | 'IMAGE';
  fileSizeBytes: number;
  updatedAt: string;
}


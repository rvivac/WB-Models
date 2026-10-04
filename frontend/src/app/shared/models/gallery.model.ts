export type PhotoCategory = 'BOOK' | 'POLAROID';

export interface GalleryPhoto {
  id?: string;
  url: string;
  /** @deprecated Use filePath ao invés de storagePath (mantido para compatibilidade) */
  storagePath?: string;
  /** Caminho relativo dentro do bucket do Supabase (usado para fallback de URL) */
  filePath?: string;
  category: PhotoCategory;
  orderIndex: number;
  isCover: boolean;
  /** Upload ainda em progresso? (pre-visualização temporária temp-) */
  isUploading?: boolean;
  /** Upload 100% confirmado pela API e persistido no banco? */
  isUploaded?: boolean;
  /** Esta mídia está ATIVA no site? (inativas são ocultadas do público) */
  isActive?: boolean;
  /** Arquivo File (apenas no cliente, antes do upload) */
  file?: File;
  /** Progresso do upload 0 a 100 */
  uploadProgress?: number;
}

export interface ModelComposite {
  id: string;
  fileUrl: string;
  fileName: string;
  fileType: 'PDF' | 'IMAGE';
  fileSizeBytes: number;
  updatedAt: string;
}


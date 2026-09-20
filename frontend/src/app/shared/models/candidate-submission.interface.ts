export type SubmissionGender = 'FEMALE' | 'MALE' | 'NON_BINARY' | 'OTHER';
export type SubmissionStatus = 'PENDING' | 'REVIEWING' | 'APPROVED' | 'REJECTED' | 'CONTACTED' | 'ARCHIVED';

export interface CandidateSubmissionModel {
  fullName: string;
  email: string;
  phone: string;
  birthDate: string; // YYYY-MM-DD
  gender: SubmissionGender;
  city: string;
  state: string; // 2 chars (UF)
  height: number; // e.g. 1.78
  bust?: number | null;
  waist?: number | null;
  hips?: number | null;
  shoeSize?: number | null;
  eyeColor?: string | null;
  hairColor?: string | null;
  instagramHandle?: string | null;
  guardianName?: string | null;
  guardianPhone?: string | null;
  guardianEmail?: string | null;
  lgpdConsent: boolean;
}

export interface CandidateSubmissionPhotos {
  facePhoto: File;
  profilePhoto: File;
  fullBodyPhoto: File;
}

export interface CandidateSubmissionResponse {
  id: string;
  protocol: string;
  message?: string;
  status: SubmissionStatus;
  convertedToModelId?: string | null;
  createdAt: string;
  updatedAt?: string;

  // Dados Cadastrais & Contato
  fullName?: string;
  email?: string;
  phone?: string;
  birthDate?: string;
  age?: number;
  gender?: SubmissionGender;
  city?: string;
  state?: string;

  // Medidas e Características Físicas
  height?: number;
  bust?: number;
  waist?: number;
  hips?: number;
  shoeSize?: number;
  eyeColor?: string;
  hairColor?: string;
  instagramHandle?: string;

  // Responsável Legal
  guardianName?: string;
  guardianPhone?: string;
  guardianEmail?: string;

  // Mídias Fotográficas
  facePhotoUrl?: string;
  profilePhotoUrl?: string;
  fullBodyPhotoUrl?: string;

  // Auditoria de Revisão
  reviewedBy?: string;
  reviewedAt?: string;
  feedbackNotes?: string;
}

export interface CandidateStatusUpdate {
  status: SubmissionStatus;
  feedbackNotes?: string;
  adminNotes?: string;
}

export interface CandidateSubmissionFilters {
  search?: string;
  status?: SubmissionStatus | 'ALL';
  gender?: SubmissionGender;
  minHeight?: number;
  maxHeight?: number;
  startDate?: string;
  endDate?: string;
  page?: number;
  size?: number;
  sort?: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

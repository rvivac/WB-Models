export type CandidateStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export interface CandidatePhoto {
  id: string;
  url: string;
  type: 'POLAROID_ROSTO' | 'POLAROID_PERFIL' | 'CORPO_INTEIRO' | 'COMPOSITE';
}

export interface Candidate {
  id: string;
  fullName: string;
  email: string;
  phone: string;
  birthDate: string;
  isMinor: boolean;
  guardianName?: string;
  guardianPhone?: string;
  guardianEmail?: string;
  city: string;
  state: string;
  height: number;
  bust: number;
  waist: number;
  hips: number;
  shoes: number;
  eyeColor: string;
  hairColor: string;
  status: CandidateStatus;
  photos: CandidatePhoto[];
  notes?: string;
  createdAt: string;
  instagramHandle?: string;
  age?: number;
  gender?: string;
  protocol?: string;
  reviewedBy?: string;
  reviewedAt?: string;
  convertedToModelId?: string;
}

export interface CandidateStatusUpdatePayload {
  status: CandidateStatus;
  notes?: string;
  adminNotes?: string;
}

export interface CandidatePageResponse {
  content: Candidate[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

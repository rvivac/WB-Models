export interface CandidateMedia {
  id: string;
  url: string;
  type: 'POLAROID_ROSTO' | 'POLAROID_PERFIL' | 'CORPO_INTEIRO' | 'COMPOSITE';
  fileName: string;
  fileSizeBytes: number;
}

export interface CandidateDetail {
  id: string;
  fullName: string;
  email: string;
  phone: string;
  instagram?: string;
  birthDate: string;
  age: number;
  isMinor: boolean;
  guardianName?: string;
  guardianPhone?: string;
  guardianEmail?: string;
  city: string;
  state: string;
  biometrics: {
    height: number;
    bust: number;
    waist: number;
    hips: number;
    shoes: number;
    eyes: string;
    hair: string;
  };
  photos: CandidateMedia[];
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  internalNotes?: string;
  lgpdConsent: boolean;
  lgpdConsentAt: string;
  submittedAt: string;
}

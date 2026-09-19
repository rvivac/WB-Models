export interface CandidatePhotoUpload {
  photoPosition: number;
  fileUrl: string;
  filePath: string;
}

export interface CandidateApplicationRequest {
  fullName: string;
  email: string;
  phone: string;
  age: number;
  guardianName?: string;
  gender: string;
  heightCm: number;
  weightKg?: number;
  bustChestCm?: number;
  waistCm?: number;
  hipsCm?: number;
  instagramHandle?: string;
  tiktokHandle?: string;
  lgpdAccepted: boolean;
  photos: CandidatePhotoUpload[];
}

export interface CandidateResponse {
  id: string;
  fullName: string;
  email: string;
  phone: string;
  age: number;
  gender: string;
  heightCm: number;
  createdAt: string;
  photoCount: number;
}

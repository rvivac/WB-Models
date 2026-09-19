export type ModelGender = 'FEMALE' | 'MALE' | 'NON_BINARY';
export type ModelCategory = 'FASHION' | 'COMMERCIAL' | 'PLUS_SIZE' | 'NEW_FACE' | 'SPECIAL';

export interface ModelMediaItem {
  id: string;
  mediaType: 'BOOK' | 'POLAROID' | 'COMPOSITE' | 'VIDEO';
  fileUrl: string;
  displayOrder: number;
  isCover: boolean;
}

export interface ModelItem {
  id: string;
  name: string;
  slug: string;
  gender: ModelGender;
  category: ModelCategory;
  heightCm: number;
  weightKg?: number;
  bustChestCm?: number;
  waistCm?: number;
  hipsCm?: number;
  shoesSize?: number;
  eyesColor?: string;
  hairColor?: string;
  instagramHandle?: string;
  bio?: string;
  isActive: boolean;
  isFeatured: boolean;
  coverImageUrl?: string;
  compositeUrl?: string;
  media: ModelMediaItem[];
  createdAt: string;
}

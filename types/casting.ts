import { Database, ModelGender, EyeColor, HairColor, MediaCategory } from './database.types';

export type Model = Database['public']['Tables']['models']['Row'];
export type ModelMedia = Database['public']['Tables']['model_media']['Row'];
export type Candidature = Database['public']['Tables']['candidatures']['Row'];

export interface ModelWithMedia extends Model {
  media: ModelMedia[];
}

export interface CastingFiltersState {
  gender: ModelGender | 'all';
  isStarOnly: boolean;
  minHeight: number;
  maxHeight: number;
  dressSize: string | 'all';
  eyeColor: EyeColor | 'all';
  searchQuery: string;
}

export interface ModelMeasurementGroup {
  height: string;
  bust: string;
  waist: string;
  hips: string;
  shoes: string;
  dress: string;
  eyes: string;
  hair: string;
}

export interface ScoutingFormPayload {
  fullName: string;
  email: string;
  phoneWhatsapp: string;
  birthDate: string;
  instagram?: string;
  city: string;
  state: string;
  gender: ModelGender;
  heightCm: number;
  bustChestCm?: number;
  waistCm?: number;
  hipsCm?: number;
  shoeSize?: number;
  dressSize?: string;
  eyeColor?: EyeColor;
  hairColor?: HairColor;
  photos: {
    file: File;
    previewUrl: string;
    type: 'rosto_frontal' | 'perfil' | 'corpo_inteiro' | 'sorrindo' | 'polaroid_extra';
  }[];
  lgpdConsent: boolean;
}

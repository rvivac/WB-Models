import { Database, ModelGender, EyeColor, HairColor, MediaCategory } from './database.types';

export type Model = Database['public']['Tables']['models']['Row'];
export type ModelMedia = Database['public']['Tables']['model_media']['Row'];
export type CastingBoard = Database['public']['Tables']['casting_boards']['Row'];
export type BoardItem = Database['public']['Tables']['board_items']['Row'];
export type ScoutingApplication = Database['public']['Tables']['scouting_applications']['Row'];

export interface ModelWithMedia extends Model {
  media: ModelMedia[];
}

export interface BoardItemWithModel extends BoardItem {
  model: ModelWithMedia;
}

export interface CastingBoardDetailed extends CastingBoard {
  items: BoardItemWithModel[];
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

export interface CreateBoardPayload {
  title: string;
  clientName?: string;
  clientEmail?: string;
  notes?: string;
  password?: string;
  expirationDays: 7 | 15 | 30;
  modelIds: string[];
}

export interface ImageValidationResult {
  isValid: boolean;
  error?: string;
  width?: number;
  height?: number;
  aspectRatio?: number;
}

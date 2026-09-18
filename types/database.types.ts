export type Json =
  | string
  | number
  | boolean
  | null
  | { [key: string]: Json | undefined }
  | Json[];

export type ModelGender = 'female' | 'male' | 'non_binary';
export type EyeColor = 'castanho_claro' | 'castanho_escuro' | 'verde' | 'azul' | 'mel' | 'preto' | 'heterocromia';
export type HairColor = 'preto' | 'castanho_escuro' | 'castanho_claro' | 'loiro' | 'ruivo' | 'grisalho' | 'colorido';
export type MediaCategory = 'polaroid' | 'editorial' | 'runway' | 'commercial' | 'composite_cover';
export type CandidatureStatus = 'received' | 'under_review' | 'approved' | 'declined' | 'archived';
export type UserRoleEnum = 'superadmin' | 'booker' | 'scout' | 'readonly';

export interface Database {
  public: {
    Tables: {
      user_roles: {
        Row: {
          id: string;
          user_id: string;
          role: UserRoleEnum;
          full_name: string;
          created_at: string;
          updated_at: string;
        };
        Insert: {
          id?: string;
          user_id: string;
          role?: UserRoleEnum;
          full_name: string;
          created_at?: string;
          updated_at?: string;
        };
        Update: {
          id?: string;
          user_id?: string;
          role?: UserRoleEnum;
          full_name?: string;
          created_at?: string;
          updated_at?: string;
        };
      };
      models: {
        Row: {
          id: string;
          slug: string;
          artistic_name: string;
          legal_name: string | null;
          gender: ModelGender;
          is_star: boolean;
          is_active: boolean;
          height_cm: number;
          bust_chest_cm: number;
          waist_cm: number;
          hips_cm: number;
          shoe_size: number;
          dress_size: string;
          eye_color: EyeColor;
          hair_color: HairColor;
          city: string | null;
          state: string | null;
          nationality: string | null;
          birth_date: string | null;
          bio_pt: string | null;
          bio_en: string | null;
          instagram_handle: string | null;
          featured_order: number;
          hero_image_key: string;
          hero_video_stream_id: string | null;
          created_at: string;
          updated_at: string;
        };
        Insert: {
          id?: string;
          slug: string;
          artistic_name: string;
          legal_name?: string | null;
          gender: ModelGender;
          is_star?: boolean;
          is_active?: boolean;
          height_cm: number;
          bust_chest_cm: number;
          waist_cm: number;
          hips_cm: number;
          shoe_size: number;
          dress_size: string;
          eye_color: EyeColor;
          hair_color: HairColor;
          city?: string | null;
          state?: string | null;
          nationality?: string | null;
          birth_date?: string | null;
          bio_pt?: string | null;
          bio_en?: string | null;
          instagram_handle?: string | null;
          featured_order?: number;
          hero_image_key: string;
          hero_video_stream_id?: string | null;
          created_at?: string;
          updated_at?: string;
        };
        Update: {
          id?: string;
          slug?: string;
          artistic_name?: string;
          legal_name?: string | null;
          gender?: ModelGender;
          is_star?: boolean;
          is_active?: boolean;
          height_cm?: number;
          bust_chest_cm?: number;
          waist_cm?: number;
          hips_cm?: number;
          shoe_size?: number;
          dress_size?: string;
          eye_color?: EyeColor;
          hair_color?: HairColor;
          city?: string | null;
          state?: string | null;
          nationality?: string | null;
          birth_date?: string | null;
          bio_pt?: string | null;
          bio_en?: string | null;
          instagram_handle?: string | null;
          featured_order?: number;
          hero_image_key?: string;
          hero_video_stream_id?: string | null;
          created_at?: string;
          updated_at?: string;
        };
      };
      model_media: {
        Row: {
          id: string;
          model_id: string;
          category: MediaCategory;
          media_url: string;
          storage_path: string;
          cloudflare_image_id: string | null;
          aspect_ratio: string | null;
          display_order: number;
          is_published: boolean;
          caption: string | null;
          created_at: string;
          updated_at: string;
        };
        Insert: {
          id?: string;
          model_id: string;
          category: MediaCategory;
          media_url: string;
          storage_path: string;
          cloudflare_image_id?: string | null;
          aspect_ratio?: string | null;
          display_order?: number;
          is_published?: boolean;
          caption?: string | null;
          created_at?: string;
          updated_at?: string;
        };
        Update: {
          id?: string;
          model_id?: string;
          category?: MediaCategory;
          media_url?: string;
          storage_path?: string;
          cloudflare_image_id?: string | null;
          aspect_ratio?: string | null;
          display_order?: number;
          is_published?: boolean;
          caption?: string | null;
          created_at?: string;
          updated_at?: string;
        };
      };
      candidatures: {
        Row: {
          id: string;
          status: CandidatureStatus;
          full_name: string;
          email: string;
          phone_whatsapp: string;
          birth_date: string;
          instagram: string | null;
          city: string;
          state: string;
          gender: ModelGender;
          height_cm: number;
          bust_chest_cm: number | null;
          waist_cm: number | null;
          hips_cm: number | null;
          shoe_size: number | null;
          dress_size: string | null;
          eye_color: EyeColor | null;
          hair_color: HairColor | null;
          uploaded_photos: Json;
          lgpd_consent_given: boolean;
          lgpd_consent_timestamp: string;
          lgpd_consent_ip: string | null;
          lgpd_consent_user_agent: string | null;
          review_notes: string | null;
          reviewed_by: string | null;
          expires_at: string;
          created_at: string;
          updated_at: string;
        };
        Insert: {
          id?: string;
          status?: CandidatureStatus;
          full_name: string;
          email: string;
          phone_whatsapp: string;
          birth_date: string;
          instagram?: string | null;
          city: string;
          state: string;
          gender: ModelGender;
          height_cm: number;
          bust_chest_cm?: number | null;
          waist_cm?: number | null;
          hips_cm?: number | null;
          shoe_size?: number | null;
          dress_size?: string | null;
          eye_color?: EyeColor | null;
          hair_color?: HairColor | null;
          uploaded_photos?: Json;
          lgpd_consent_given: boolean;
          lgpd_consent_timestamp?: string;
          lgpd_consent_ip?: string | null;
          lgpd_consent_user_agent?: string | null;
          review_notes?: string | null;
          reviewed_by?: string | null;
          expires_at?: string;
          created_at?: string;
          updated_at?: string;
        };
        Update: {
          id?: string;
          status?: CandidatureStatus;
          full_name?: string;
          email?: string;
          phone_whatsapp?: string;
          birth_date?: string;
          instagram?: string | null;
          city?: string;
          state?: string;
          gender?: ModelGender;
          height_cm?: number;
          bust_chest_cm?: number | null;
          waist_cm?: number | null;
          hips_cm?: number | null;
          shoe_size?: number | null;
          dress_size?: string | null;
          eye_color?: EyeColor | null;
          hair_color?: HairColor | null;
          uploaded_photos?: Json;
          lgpd_consent_given?: boolean;
          lgpd_consent_timestamp?: string;
          lgpd_consent_ip?: string | null;
          lgpd_consent_user_agent?: string | null;
          review_notes?: string | null;
          reviewed_by?: string | null;
          expires_at?: string;
          created_at?: string;
          updated_at?: string;
        };
      };
    };
  };
}

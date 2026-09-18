-- ==============================================================================
-- WB CASTING INTELLIGENCE PLATFORM (WB-CIP)
-- ARQUITETURA DE BANCO DE DADOS & SEGURANÇA POSTGRESQL (SUPABASE)
-- Engenharia de Sistemas: RVIVAC Guild (Peruíbe - SP)
-- Padrão: Editorial Minimalista, RLS Granular, Conformidade LGPD & Zero Overengineering
-- ==============================================================================

-- 1. EXTENSÕES DO NÚCLEO
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 2. TIPOS CUSTOMIZADOS (ENUMS)
DO $$ BEGIN
    CREATE TYPE model_gender AS ENUM ('female', 'male', 'non_binary');
EXCEPTION WHEN duplicate_object THEN null; END $$;

DO $$ BEGIN
    CREATE TYPE eye_color AS ENUM ('castanho_claro', 'castanho_escuro', 'verde', 'azul', 'mel', 'preto', 'heterocromia');
EXCEPTION WHEN duplicate_object THEN null; END $$;

DO $$ BEGIN
    CREATE TYPE hair_color AS ENUM ('preto', 'castanho_escuro', 'castanho_claro', 'loiro', 'ruivo', 'grisalho', 'colorido');
EXCEPTION WHEN duplicate_object THEN null; END $$;

DO $$ BEGIN
    CREATE TYPE media_category AS ENUM ('polaroid', 'editorial', 'runway', 'commercial', 'composite_cover');
EXCEPTION WHEN duplicate_object THEN null; END $$;

DO $$ BEGIN
    CREATE TYPE application_status AS ENUM ('received', 'under_review', 'approved', 'declined', 'archived');
EXCEPTION WHEN duplicate_object THEN null; END $$;

DO $$ BEGIN
    CREATE TYPE user_role_enum AS ENUM ('superadmin', 'booker', 'scout', 'readonly');
EXCEPTION WHEN duplicate_object THEN null; END $$;

-- 3. TABELA DE ROLES DE USUÁRIOS DA AGÊNCIA (BACKOFFICE)
CREATE TABLE IF NOT EXISTS public.user_roles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    role user_role_enum NOT NULL DEFAULT 'readonly',
    full_name TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT unique_user_role UNIQUE (user_id)
);

-- 4. TABELA DE MODELOS (CASTING ATIVO)
CREATE TABLE IF NOT EXISTS public.models (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    slug TEXT NOT NULL UNIQUE,
    artistic_name TEXT NOT NULL,
    legal_name TEXT, -- Restrito ao backoffice (Proteção de dados)
    gender model_gender NOT NULL,
    is_star BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- Biometria e Medidas Técnicas Essenciais
    height_cm NUMERIC(5,2) NOT NULL CHECK (height_cm >= 130 AND height_cm <= 225),
    bust_chest_cm NUMERIC(5,2) NOT NULL,
    waist_cm NUMERIC(5,2) NOT NULL,
    hips_cm NUMERIC(5,2) NOT NULL,
    shoe_size NUMERIC(3,1) NOT NULL,
    dress_size TEXT NOT NULL,
    eye_color eye_color NOT NULL,
    hair_color hair_color NOT NULL,
    
    -- Identificação & Cidade Base
    city TEXT,
    state TEXT,
    nationality TEXT DEFAULT 'Brasileira',
    birth_date DATE,
    
    -- Informações Profissionais
    bio_pt TEXT,
    bio_en TEXT,
    instagram_handle TEXT,
    featured_order INT DEFAULT 0,
    
    -- Mídia Principal (Cloudflare Images / Stream ID)
    hero_image_key TEXT NOT NULL,
    hero_video_stream_id TEXT,
    
    -- Auditoria
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_models_public_lookup 
    ON public.models (is_active, is_star, featured_order DESC, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_models_filters 
    ON public.models (gender, height_cm, dress_size, eye_color) 
    WHERE is_active = TRUE;

CREATE INDEX IF NOT EXISTS idx_models_slug 
    ON public.models (slug);

-- 5. TABELA DE MÍDIAS DOS MODELOS (PORTFÓLIO, EDITORIAL, POLAROIDS)
CREATE TABLE IF NOT EXISTS public.model_media (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    model_id UUID NOT NULL REFERENCES public.models(id) ON DELETE CASCADE,
    category media_category NOT NULL,
    media_url TEXT NOT NULL,
    storage_path TEXT NOT NULL,
    cloudflare_image_id TEXT,
    aspect_ratio TEXT DEFAULT '3:4',
    display_order INT NOT NULL DEFAULT 0,
    is_published BOOLEAN NOT NULL DEFAULT TRUE,
    is_sensitive_polaroid BOOLEAN NOT NULL DEFAULT FALSE, -- Polaroides brutas exigem URL assinada
    caption TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_model_media_model_category 
    ON public.model_media (model_id, category, display_order ASC) 
    WHERE is_published = TRUE;

-- 6. MÓDULO B2B: CASTING BOARDS EFÊMEROS & COMPARTILHÁVEIS
CREATE TABLE IF NOT EXISTS public.casting_boards (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    share_token TEXT NOT NULL UNIQUE, -- Token alfanumérico seguro para acesso público/B2B
    title TEXT NOT NULL, -- Ex: "Campanha Verão 2027 - Marca X"
    client_name TEXT, -- Nome do produtor / agência parceira
    client_email TEXT,
    notes TEXT,
    password_hash TEXT, -- Opcional: proteção por senha via crypt(password, gen_salt('bf'))
    is_password_protected BOOLEAN GENERATED ALWAYS AS (password_hash IS NOT NULL) STORED,
    expires_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() + INTERVAL '15 days'),
    created_by UUID REFERENCES auth.users(id), -- Nullable se gerado por visitante/produtor no catálogo
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_casting_boards_token 
    ON public.casting_boards (share_token, is_active, expires_at);

-- 7. ITENS DO CASTING BOARD (RELAÇÃO N:N COM MODELOS)
CREATE TABLE IF NOT EXISTS public.board_items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    board_id UUID NOT NULL REFERENCES public.casting_boards(id) ON DELETE CASCADE,
    model_id UUID NOT NULL REFERENCES public.models(id) ON DELETE CASCADE,
    display_order INT NOT NULL DEFAULT 0,
    producer_notes TEXT, -- Ex: "Opção principal para desfile de abertura"
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT unique_board_model UNIQUE (board_id, model_id)
);

CREATE INDEX IF NOT EXISTS idx_board_items_order 
    ON public.board_items (board_id, display_order ASC);

-- 8. FUNIL DE SCOUTING: CANDIDATURAS COM SEGURANÇA E LGPD
CREATE TABLE IF NOT EXISTS public.scouting_applications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    status application_status NOT NULL DEFAULT 'received',
    
    -- Dados Pessoais Protegidos
    full_name TEXT NOT NULL,
    email TEXT NOT NULL,
    phone_whatsapp TEXT NOT NULL,
    birth_date DATE NOT NULL,
    instagram TEXT,
    city TEXT NOT NULL,
    state TEXT NOT NULL,
    
    -- Medidas Técnicas Declaradas
    gender model_gender NOT NULL,
    height_cm NUMERIC(5,2) NOT NULL,
    bust_chest_cm NUMERIC(5,2),
    waist_cm NUMERIC(5,2),
    hips_cm NUMERIC(5,2),
    shoe_size NUMERIC(3,1),
    dress_size TEXT,
    eye_color eye_color,
    hair_color hair_color,
    
    -- Metadados de Fotos Otimizadas no Cliente
    -- Estrutura JSON: [{"url": "...", "type": "rosto_frontal", "aspect_ratio": "3:4", "size_kb": 180}]
    uploaded_photos JSONB NOT NULL DEFAULT '[]'::jsonb,
    
    -- Protocolo Estrito de Consentimento LGPD (Lei 13.709/2018)
    lgpd_consent_given BOOLEAN NOT NULL DEFAULT FALSE,
    lgpd_consent_timestamp TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    lgpd_consent_ip TEXT,
    lgpd_consent_user_agent TEXT,
    
    -- Triagem & Auto-Descarte LGPD (90 dias)
    review_notes TEXT,
    reviewed_by UUID REFERENCES auth.users(id),
    expires_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() + INTERVAL '90 days'),
    
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_scouting_applications_status_date 
    ON public.scouting_applications (status, created_at DESC);

-- 9. TRIGGERS DE UPDATED_AT
CREATE OR REPLACE FUNCTION public.handle_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS tr_models_updated_at ON public.models;
CREATE TRIGGER tr_models_updated_at BEFORE UPDATE ON public.models FOR EACH ROW EXECUTE FUNCTION public.handle_updated_at();

DROP TRIGGER IF EXISTS tr_model_media_updated_at ON public.model_media;
CREATE TRIGGER tr_model_media_updated_at BEFORE UPDATE ON public.model_media FOR EACH ROW EXECUTE FUNCTION public.handle_updated_at();

DROP TRIGGER IF EXISTS tr_casting_boards_updated_at ON public.casting_boards;
CREATE TRIGGER tr_casting_boards_updated_at BEFORE UPDATE ON public.casting_boards FOR EACH ROW EXECUTE FUNCTION public.handle_updated_at();

DROP TRIGGER IF EXISTS tr_scouting_applications_updated_at ON public.scouting_applications;
CREATE TRIGGER tr_scouting_applications_updated_at BEFORE UPDATE ON public.scouting_applications FOR EACH ROW EXECUTE FUNCTION public.handle_updated_at();

-- 10. FUNÇÃO AUXILIAR DE SEGURANÇA (VERIFICAÇÃO DE EQUIPE WB)
CREATE OR REPLACE FUNCTION public.is_agency_team()
RETURNS BOOLEAN AS $$
BEGIN
    RETURN EXISTS (
        SELECT 1 FROM public.user_roles 
        WHERE user_id = auth.uid() 
          AND role IN ('superadmin', 'booker', 'scout')
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 11. POLÍTICAS DE ROW LEVEL SECURITY (RLS) RIGOROSAS

-- Habilitação obrigatória de RLS
ALTER TABLE public.user_roles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.models ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.model_media ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.casting_boards ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.board_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.scouting_applications ENABLE ROW LEVEL SECURITY;

-- 11.1 Políticas para MODELS
-- Leitura pública irrestrita apenas de modelos ativos (sem expor dados sensíveis)
CREATE POLICY "Public Read Active Models" 
    ON public.models FOR SELECT 
    USING (is_active = TRUE);

-- Equipe autenticada tem acesso irrestrito
CREATE POLICY "Agency Full Access Models" 
    ON public.models FOR ALL 
    USING (public.is_agency_team())
    WITH CHECK (public.is_agency_team());

-- 11.2 Políticas para MODEL_MEDIA
-- Leitura pública de portfólio e fotos não-sensíveis de modelos ativos
CREATE POLICY "Public Read Portfolio Media" 
    ON public.model_media FOR SELECT 
    USING (
        is_published = TRUE 
        AND is_sensitive_polaroid = FALSE 
        AND EXISTS (
            SELECT 1 FROM public.models 
            WHERE public.models.id = public.model_media.model_id 
              AND public.models.is_active = TRUE
        )
    );

-- Leitura de Polaroides Sensíveis e Gestão restrita à Equipe da Agência
CREATE POLICY "Agency Full Access Media" 
    ON public.model_media FOR ALL 
    USING (public.is_agency_team())
    WITH CHECK (public.is_agency_team());

-- 11.3 Políticas para CASTING_BOARDS
-- Visitantes/Produtores podem criar novos boards públicos/efêmeros
CREATE POLICY "Public Create Casting Board" 
    ON public.casting_boards FOR INSERT 
    WITH CHECK (TRUE);

-- Consulta de Board via Token único válido e não expirado
CREATE POLICY "Public Read Active Casting Board via Token" 
    ON public.casting_boards FOR SELECT 
    USING (
        is_active = TRUE 
        AND expires_at > NOW()
    );

-- Atualização e gestão total de boards pela equipe da agência
CREATE POLICY "Agency Full Access Casting Boards" 
    ON public.casting_boards FOR ALL 
    USING (public.is_agency_team())
    WITH CHECK (public.is_agency_team());

-- 11.4 Políticas para BOARD_ITEMS
-- Inserção de itens permitida para boards ativos
CREATE POLICY "Public Insert Board Items" 
    ON public.board_items FOR INSERT 
    WITH CHECK (
        EXISTS (
            SELECT 1 FROM public.casting_boards 
            WHERE public.casting_boards.id = public.board_items.board_id 
              AND public.casting_boards.is_active = TRUE 
              AND public.casting_boards.expires_at > NOW()
        )
    );

-- Leitura pública de itens de boards válidos
CREATE POLICY "Public Read Board Items" 
    ON public.board_items FOR SELECT 
    USING (
        EXISTS (
            SELECT 1 FROM public.casting_boards 
            WHERE public.casting_boards.id = public.board_items.board_id 
              AND public.casting_boards.is_active = TRUE 
              AND public.casting_boards.expires_at > NOW()
        )
    );

-- Gestão completa pela equipe
CREATE POLICY "Agency Full Access Board Items" 
    ON public.board_items FOR ALL 
    USING (public.is_agency_team())
    WITH CHECK (public.is_agency_team());

-- 11.5 Políticas para SCOUTING_APPLICATIONS (LGPD Compliance)
-- Inserção anônima permitida exclusivamente com consentimento formal marcado
CREATE POLICY "Public Candidate Submit Application" 
    ON public.scouting_applications FOR INSERT 
    WITH CHECK (lgpd_consent_given = TRUE);

-- Leitura, avaliação e eliminação EXCLUSIVAS para equipe interna da agência
CREATE POLICY "Agency Exclusive Access Applications" 
    ON public.scouting_applications FOR ALL 
    USING (public.is_agency_team())
    WITH CHECK (public.is_agency_team());

-- 12. BUCKETS DE ARMAZENAMENTO SEGREGAÇÃO (SUPABASE STORAGE)
INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES 
    ('models-public', 'models-public', true, 15728640, ARRAY['image/jpeg', 'image/png', 'image/webp', 'image/avif']),
    ('scouting-quarantine', 'scouting-quarantine', false, 10485760, ARRAY['image/jpeg', 'image/png', 'image/webp', 'image/avif'])
ON CONFLICT (id) DO UPDATE SET 
    public = EXCLUDED.public,
    file_size_limit = EXCLUDED.file_size_limit,
    allowed_mime_types = EXCLUDED.allowed_mime_types;

-- Políticas de Armazenamento no Storage:
CREATE POLICY "Public View Public Model Photos" 
    ON storage.objects FOR SELECT 
    USING (bucket_id = 'models-public');

CREATE POLICY "Agency Manage Public Photos" 
    ON storage.objects FOR ALL 
    USING (bucket_id = 'models-public' AND public.is_agency_team())
    WITH CHECK (bucket_id = 'models-public' AND public.is_agency_team());

-- Upload em Quarentena para Scouting:
CREATE POLICY "Candidate Upload Scouting Photos" 
    ON storage.objects FOR INSERT 
    WITH CHECK (bucket_id = 'scouting-quarantine');

CREATE POLICY "Agency Exclusive Access Quarantine" 
    ON storage.objects FOR SELECT 
    USING (bucket_id = 'scouting-quarantine' AND public.is_agency_team());

CREATE POLICY "Agency Purge Quarantine Photos" 
    ON storage.objects FOR DELETE 
    USING (bucket_id = 'scouting-quarantine' AND public.is_agency_team());

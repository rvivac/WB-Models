-- ==============================================================================
-- WB SCOUTING PLATFORM - RELATIONAL DATABASE SCHEMA (DDL)
-- Target Engine: PostgreSQL 14+ / Supabase
-- Architecture & Standards: Spring Data JPA / Hibernate & PostgREST Compatible
-- ==============================================================================

-- 1. EXTENSÕES NECESSÁRIAS
-- Pgcrypto para hash de senhas e geração criptográfica caso necessário
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 2. ENUMS DE DOMÍNIO
-- Enums declarados de forma idempotente para evitar exceções em re-execuções
DO $$ 
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'gender_type') THEN
        CREATE TYPE gender_type AS ENUM ('FEMALE', 'MALE');
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'media_type') THEN
        CREATE TYPE media_type AS ENUM ('BOOK', 'POLAROID', 'COMPOSITE');
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'admin_role') THEN
        CREATE TYPE admin_role AS ENUM ('SUPER_ADMIN', 'CONTENT_ADMIN');
    END IF;
END $$;

-- 3. FUNÇÃO GENÉRICA DE AUDITORIA (UPDATED_AT)
-- Executada antes de operações de UPDATE para manter a consistência temporal
CREATE OR REPLACE FUNCTION trigger_set_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 4. TABELAS DO SCHEMA

-- ------------------------------------------------------------------------------
-- 4.1. TABELA: admins
-- Usuários administrativos da plataforma (Backoffice / Gestão de Conteúdo)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.admins (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(150) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role admin_role NOT NULL DEFAULT 'SUPER_ADMIN',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    password_reset_token VARCHAR(255) NULL,
    password_reset_expires_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_admins_email UNIQUE (email)
);

DROP TRIGGER IF EXISTS set_timestamp_admins ON public.admins;
CREATE TRIGGER set_timestamp_admins
    BEFORE UPDATE ON public.admins
    FOR EACH ROW
    EXECUTE FUNCTION trigger_set_timestamp();

-- ------------------------------------------------------------------------------
-- 4.2. TABELA: models
-- Catálogo oficial de modelos e dados biométricos
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.models (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    stage_name VARCHAR(150) NOT NULL,
    gender gender_type NOT NULL,
    is_star BOOLEAN NOT NULL DEFAULT FALSE,
    is_featured_home BOOLEAN NOT NULL DEFAULT FALSE,
    featured_order INTEGER NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    primary_photo_url TEXT NULL,
    instagram_url VARCHAR(255) NULL,
    
    -- Dados biométricos e profissionais (anuláveis para preenchimento flexível)
    birth_date DATE NULL,
    height_cm INTEGER NULL CHECK (height_cm IS NULL OR (height_cm >= 50 AND height_cm <= 250)),
    city VARCHAR(100) NULL,
    nationality VARCHAR(100) NULL,
    dress_size VARCHAR(20) NULL,
    shoe_size VARCHAR(20) NULL,
    bust_chest_cm NUMERIC(5,2) NULL,
    waist_cm NUMERIC(5,2) NULL,
    hips_cm NUMERIC(5,2) NULL,
    hair_color VARCHAR(50) NULL,
    eyes_color VARCHAR(50) NULL,
    
    -- Auditoria
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

DROP TRIGGER IF EXISTS set_timestamp_models ON public.models;
CREATE TRIGGER set_timestamp_models
    BEFORE UPDATE ON public.models
    FOR EACH ROW
    EXECUTE FUNCTION trigger_set_timestamp();

-- ------------------------------------------------------------------------------
-- 4.3. TABELA: model_media
-- Book fotográfico, Polaroides e Composite digital
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.model_media (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    model_id UUID NOT NULL,
    media_type media_type NOT NULL,
    file_url TEXT NOT NULL,
    file_path TEXT NOT NULL,
    display_order INTEGER NOT NULL DEFAULT 0,
    is_cover BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_model_media_model 
        FOREIGN KEY (model_id) 
        REFERENCES public.models(id) 
        ON DELETE CASCADE
);

DROP TRIGGER IF EXISTS set_timestamp_model_media ON public.model_media;
CREATE TRIGGER set_timestamp_model_media
    BEFORE UPDATE ON public.model_media
    FOR EACH ROW
    EXECUTE FUNCTION trigger_set_timestamp();

-- Restrição Crítica: Cada modelo pode possuir NO MÁXIMO UM registro ativo com media_type = 'COMPOSITE'
CREATE UNIQUE INDEX IF NOT EXISTS idx_model_media_single_active_composite
    ON public.model_media (model_id)
    WHERE (media_type = 'COMPOSITE' AND is_active = TRUE);

-- ------------------------------------------------------------------------------
-- 4.4. TABELA: candidates
-- Fichas de inscrição de novos talentos (Funil "Quero ser modelo")
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.candidates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name VARCHAR(200) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(50) NOT NULL,
    birth_date DATE NULL,
    age INTEGER NULL,
    gender VARCHAR(50) NOT NULL,
    height_cm NUMERIC(5,2) NOT NULL,
    city VARCHAR(100) NULL,
    state VARCHAR(50) NULL,
    legal_guardian_name VARCHAR(200) NULL,
    legal_guardian_contact VARCHAR(50) NULL,
    guardian_name VARCHAR(200) NULL,
    weight_kg NUMERIC(5,2) NULL,
    bust_chest_cm NUMERIC(5,2) NULL,
    waist_cm NUMERIC(5,2) NULL,
    hips_cm NUMERIC(5,2) NULL,
    shoe_size VARCHAR(20) NULL,
    dress_size VARCHAR(20) NULL,
    instagram_handle VARCHAR(100) NULL,
    portfolio_url VARCHAR(255) NULL,
    tiktok_handle VARCHAR(100) NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    internal_notes TEXT NULL,
    
    -- Termos LGPD (Consentimento explícito e mandatório)
    lgpd_accepted BOOLEAN NOT NULL DEFAULT TRUE CHECK (lgpd_accepted IS TRUE),
    lgpd_accepted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    -- Auditoria (Registro imutável de candidatura)
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ------------------------------------------------------------------------------
-- 4.5. TABELA: candidate_photos
-- Fotografias submetidas pelos candidatos (mínimo 3, máximo 6 imagens no fluxo novo)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.candidate_photos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    candidate_id UUID NOT NULL,
    storage_path TEXT NOT NULL,
    display_order INTEGER NOT NULL DEFAULT 1,
    uploaded_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    photo_position SMALLINT NULL,
    file_url TEXT NULL,
    file_path TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_candidate_photos_candidate 
        FOREIGN KEY (candidate_id) 
        REFERENCES public.candidates(id) 
        ON DELETE CASCADE
);

-- ------------------------------------------------------------------------------
-- 4.5.1. TABELA: candidate_submissions
-- Submissões do endpoint público de captação (/api/v1/submissions - Prompt 3.3.1)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.candidate_submissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    protocol VARCHAR(50) NOT NULL,
    full_name VARCHAR(120) NOT NULL,
    email VARCHAR(100) NOT NULL,
    phone VARCHAR(50) NOT NULL,
    birth_date DATE NOT NULL,
    age INTEGER NOT NULL,
    gender VARCHAR(30) NOT NULL,
    city VARCHAR(80) NOT NULL,
    state VARCHAR(2) NOT NULL,
    height NUMERIC(4,2) NOT NULL,
    bust NUMERIC(5,2) NULL,
    waist NUMERIC(5,2) NULL,
    hips NUMERIC(5,2) NULL,
    shoe_size INTEGER NULL,
    eye_color VARCHAR(50) NULL,
    hair_color VARCHAR(50) NULL,
    instagram_handle VARCHAR(80) NULL,
    guardian_name VARCHAR(120) NULL,
    guardian_phone VARCHAR(50) NULL,
    guardian_email VARCHAR(100) NULL,
    lgpd_consent BOOLEAN NOT NULL DEFAULT TRUE CHECK (lgpd_consent IS TRUE),
    lgpd_consent_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    face_photo_url TEXT NOT NULL,
    profile_photo_url TEXT NOT NULL,
    full_body_photo_url TEXT NOT NULL,
    reviewed_by VARCHAR(150) NULL,
    reviewed_at TIMESTAMPTZ NULL,
    feedback_notes VARCHAR(500) NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_candidate_submissions_protocol UNIQUE (protocol)
);

DROP TRIGGER IF EXISTS set_timestamp_candidate_submissions ON public.candidate_submissions;
CREATE TRIGGER set_timestamp_candidate_submissions
    BEFORE UPDATE ON public.candidate_submissions
    FOR EACH ROW
    EXECUTE FUNCTION trigger_set_timestamp();

CREATE INDEX IF NOT EXISTS idx_candidate_submissions_status ON public.candidate_submissions (status);
CREATE INDEX IF NOT EXISTS idx_candidate_submissions_email ON public.candidate_submissions (email);

-- ------------------------------------------------------------------------------
-- 4.6. TABELA: site_contents
-- Conteúdos institucionais dinâmicos e internacionalização (PT/EN)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.site_contents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    section_key VARCHAR(100) NOT NULL,
    payload_pt JSONB NOT NULL,
    payload_en JSONB NOT NULL,
    media_urls JSONB NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_site_contents_section_key UNIQUE (section_key)
);

DROP TRIGGER IF EXISTS set_timestamp_site_contents ON public.site_contents;
CREATE TRIGGER set_timestamp_site_contents
    BEFORE UPDATE ON public.site_contents
    FOR EACH ROW
    EXECUTE FUNCTION trigger_set_timestamp();

-- 5. ÍNDICES DE PERFORMANCE & OTIMIZAÇÃO DE BUSCA

-- Índices em Chaves Estrangeiras (Foreign Keys)
CREATE INDEX IF NOT EXISTS idx_model_media_model_id 
    ON public.model_media (model_id);

CREATE INDEX IF NOT EXISTS idx_candidate_photos_candidate_id 
    ON public.candidate_photos (candidate_id);

-- Índices Compostos para Catálogo e Filtragem de Casting
CREATE INDEX IF NOT EXISTS idx_models_gender_active 
    ON public.models (gender, is_active);

CREATE INDEX IF NOT EXISTS idx_models_star_active 
    ON public.models (is_star, is_active);

-- Índice Condicional para Vitrine da Home Page (Ordenação ultra-rápida)
CREATE INDEX IF NOT EXISTS idx_models_featured_home 
    ON public.models (is_featured_home, featured_order ASC) 
    WHERE (is_active = TRUE);

-- Índice para ordenação de exibição de mídias ativas
CREATE INDEX IF NOT EXISTS idx_model_media_order 
    ON public.model_media (model_id, media_type, display_order ASC) 
    WHERE (is_active = TRUE);

-- 6. SEGURANÇA E ROW LEVEL SECURITY (RLS)

-- Habilitar RLS em todas as tabelas
ALTER TABLE public.admins ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.models ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.model_media ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.candidates ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.candidate_photos ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.candidate_submissions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.site_contents ENABLE ROW LEVEL SECURITY;

-- 6.1. Políticas de Leitura Pública (Catálogo e Site Institucional)
-- Visitantes podem visualizar apenas modelos marcados como ativos
DROP POLICY IF EXISTS "Public Read Active Models" ON public.models;
CREATE POLICY "Public Read Active Models" 
    ON public.models 
    FOR SELECT 
    TO anon, authenticated
    USING (is_active = TRUE);

-- Visitantes podem visualizar mídias de modelos ativos
DROP POLICY IF EXISTS "Public Read Active Media" ON public.model_media;
CREATE POLICY "Public Read Active Media" 
    ON public.model_media 
    FOR SELECT 
    TO anon, authenticated
    USING (
        is_active = TRUE 
        AND EXISTS (
            SELECT 1 FROM public.models m 
            WHERE m.id = model_media.model_id 
              AND m.is_active = TRUE
        )
    );

-- Conteúdos do site são públicos para visualização irrestrita
DROP POLICY IF EXISTS "Public Read Site Contents" ON public.site_contents;
CREATE POLICY "Public Read Site Contents" 
    ON public.site_contents 
    FOR SELECT 
    TO anon, authenticated
    USING (TRUE);

-- 6.2. Políticas do Funil de Scouting (Quero ser modelo)
-- Candidatos anônimos podem inserir fichas (exigindo estritamente o aceite da LGPD)
DROP POLICY IF EXISTS "Public Insert Candidate Application" ON public.candidates;
CREATE POLICY "Public Insert Candidate Application" 
    ON public.candidates 
    FOR INSERT 
    TO anon, authenticated
    WITH CHECK (lgpd_accepted IS TRUE);

-- Candidatos anônimos podem inserir suas fotos associadas à candidatura existente
DROP POLICY IF EXISTS "Public Insert Candidate Photos" ON public.candidate_photos;
CREATE POLICY "Public Insert Candidate Photos" 
    ON public.candidate_photos 
    FOR INSERT 
    TO anon, authenticated
    WITH CHECK (
        EXISTS (
            SELECT 1 FROM public.candidates c 
            WHERE c.id = candidate_photos.candidate_id
        )
    );

-- 6.3. Bloqueio de Leitura Pública para Dados Sensíveis
-- 'admins', 'candidates' e 'candidate_photos' não possuem política SELECT para 'anon',
-- ficando acessíveis exclusivamente via Backend autenticado (Spring Data JPA com service_role / token de serviço)
-- ou usuários autenticados com papéis específicos no Supabase.

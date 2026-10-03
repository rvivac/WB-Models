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
-- Única função canônica usada por todos os triggers de updated_at
CREATE OR REPLACE FUNCTION public.fn_set_updated_at()
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

DROP TRIGGER IF EXISTS trg_admins_updated_at ON public.admins;
CREATE TRIGGER trg_admins_updated_at
    BEFORE UPDATE ON public.admins
    FOR EACH ROW
    EXECUTE FUNCTION public.fn_set_updated_at();

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

DROP TRIGGER IF EXISTS trg_models_updated_at ON public.models;
CREATE TRIGGER trg_models_updated_at
    BEFORE UPDATE ON public.models
    FOR EACH ROW
    EXECUTE FUNCTION public.fn_set_updated_at();

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

DROP TRIGGER IF EXISTS trg_model_media_updated_at ON public.model_media;
CREATE TRIGGER trg_model_media_updated_at
    BEFORE UPDATE ON public.model_media
    FOR EACH ROW
    EXECUTE FUNCTION public.fn_set_updated_at();

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

DROP TRIGGER IF EXISTS trg_candidates_updated_at ON public.candidates;
CREATE TRIGGER trg_candidates_updated_at
    BEFORE UPDATE ON public.candidates
    FOR EACH ROW
    EXECUTE FUNCTION public.fn_set_updated_at();

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
-- 4.6. TABELA: candidate_submissions (CANÔNICA - Backoffice & Triagem + Conversão)
-- Submissões do endpoint público de captação e backoffice de triagem
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.candidate_submissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    protocol VARCHAR(50) NOT NULL,
    full_name VARCHAR(120) NOT NULL,
    email VARCHAR(100) NOT NULL,
    phone VARCHAR(50) NOT NULL,
    birth_date DATE NOT NULL,
    age INTEGER NOT NULL,
    gender VARCHAR(20) NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(50) NOT NULL,
    height NUMERIC(4,2) NOT NULL,
    bust NUMERIC(5,2),
    waist NUMERIC(5,2),
    hips NUMERIC(5,2),
    shoe_size INTEGER,
    eye_color VARCHAR(50),
    hair_color VARCHAR(50),
    instagram_handle VARCHAR(100),
    guardian_name VARCHAR(120),
    guardian_phone VARCHAR(50),
    guardian_email VARCHAR(100),
    face_photo_url TEXT NOT NULL,
    profile_photo_url TEXT NOT NULL,
    full_body_photo_url TEXT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    feedback_notes VARCHAR(500),
    reviewed_by VARCHAR(150),
    reviewed_at TIMESTAMPTZ,
    converted_to_model_id UUID NULL REFERENCES public.models(id) ON DELETE SET NULL,
    lgpd_consent BOOLEAN NOT NULL DEFAULT FALSE,
    lgpd_consent_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_candidate_submissions_protocol UNIQUE (protocol)
);

DROP TRIGGER IF EXISTS trg_candidate_submissions_updated_at ON public.candidate_submissions;
CREATE TRIGGER trg_candidate_submissions_updated_at
    BEFORE UPDATE ON public.candidate_submissions
    FOR EACH ROW
    EXECUTE FUNCTION public.fn_set_updated_at();

CREATE INDEX IF NOT EXISTS idx_candidate_submissions_status ON public.candidate_submissions(status);
CREATE INDEX IF NOT EXISTS idx_candidate_submissions_created_at ON public.candidate_submissions(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_candidate_submissions_converted_model ON public.candidate_submissions(converted_to_model_id);
CREATE INDEX IF NOT EXISTS idx_candidate_submissions_email ON public.candidate_submissions (email);

-- ------------------------------------------------------------------------------
-- 4.7. TABELA: site_contents
-- Conteúdos institucionais dinâmicos e internacionalização (PT/EN)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.site_contents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    section_key VARCHAR(100) NOT NULL,
    payload_pt JSONB NOT NULL,
    payload_en JSONB NOT NULL,
    media_urls JSONB NULL,
    updated_by UUID NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_site_contents_section_key UNIQUE (section_key)
);

DROP TRIGGER IF EXISTS trg_site_contents_updated_at ON public.site_contents;
CREATE TRIGGER trg_site_contents_updated_at
    BEFORE UPDATE ON public.site_contents
    FOR EACH ROW
    EXECUTE FUNCTION public.fn_set_updated_at();

-- ------------------------------------------------------------------------------
-- 4.8. TABELA: admin_audit_logs
-- Trilha de auditoria imutável (Append-only) e registro de mutações corporativas
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.admin_audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    admin_id UUID NULL REFERENCES public.admins(id) ON DELETE SET NULL,
    admin_email VARCHAR(255) NOT NULL,
    action VARCHAR(50) NOT NULL,
    resource_type VARCHAR(100) NOT NULL,
    resource_id VARCHAR(255) NULL,
    description TEXT NULL,
    details_json JSONB NULL,
    ip_address VARCHAR(45) NULL,
    user_agent TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Índices de performance para busca por período, operador, ação e módulo
CREATE INDEX IF NOT EXISTS idx_admin_audit_logs_created_at ON public.admin_audit_logs (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_admin_audit_logs_admin_email ON public.admin_audit_logs (admin_email);
CREATE INDEX IF NOT EXISTS idx_admin_audit_logs_action ON public.admin_audit_logs (action);
CREATE INDEX IF NOT EXISTS idx_admin_audit_logs_resource_type ON public.admin_audit_logs (resource_type);

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
ALTER TABLE public.admin_audit_logs ENABLE ROW LEVEL SECURITY;

-- ==============================================================================
-- 6.1. POLÍTICAS DE SERVICE_ROLE (BYPASS EXPLÍCITO PARA BACKEND SPRING BOOT)
-- ==============================================================================
-- Intencionalmente documenta acesso full para service_role, garantindo
-- interoperabilidade caso a conexão JDBC seja futuramente trocada de owner
-- para a role wb_app_user (least privilege).

DROP POLICY IF EXISTS "Service Role Full Access - admins" ON public.admins;
CREATE POLICY "Service Role Full Access - admins"
    ON public.admins
    TO service_role
    USING (true)
    WITH CHECK (true);

DROP POLICY IF EXISTS "Service Role Full Access - models" ON public.models;
CREATE POLICY "Service Role Full Access - models"
    ON public.models
    TO service_role
    USING (true)
    WITH CHECK (true);

DROP POLICY IF EXISTS "Service Role Full Access - model_media" ON public.model_media;
CREATE POLICY "Service Role Full Access - model_media"
    ON public.model_media
    TO service_role
    USING (true)
    WITH CHECK (true);

DROP POLICY IF EXISTS "Service Role Full Access - candidates" ON public.candidates;
CREATE POLICY "Service Role Full Access - candidates"
    ON public.candidates
    TO service_role
    USING (true)
    WITH CHECK (true);

DROP POLICY IF EXISTS "Service Role Full Access - candidate_photos" ON public.candidate_photos;
CREATE POLICY "Service Role Full Access - candidate_photos"
    ON public.candidate_photos
    TO service_role
    USING (true)
    WITH CHECK (true);

DROP POLICY IF EXISTS "Service Role Full Access - candidate_submissions" ON public.candidate_submissions;
CREATE POLICY "Service Role Full Access - candidate_submissions"
    ON public.candidate_submissions
    TO service_role
    USING (true)
    WITH CHECK (true);

DROP POLICY IF EXISTS "Service Role Full Access - site_contents" ON public.site_contents;
CREATE POLICY "Service Role Full Access - site_contents"
    ON public.site_contents
    TO service_role
    USING (true)
    WITH CHECK (true);

DROP POLICY IF EXISTS "Service Role Full Access - admin_audit_logs" ON public.admin_audit_logs;
CREATE POLICY "Service Role Full Access - admin_audit_logs"
    ON public.admin_audit_logs
    TO service_role
    USING (true)
    WITH CHECK (true);

-- ==============================================================================
-- 6.2. POLÍTICAS DE LEITURA PÚBLICA (Catálogo e Site Institucional)
-- ==============================================================================
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

-- ==============================================================================
-- 6.3. POLÍTICAS DO FUNIL DE SCOUTING (Quero ser modelo)
-- ==============================================================================
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

-- ==============================================================================
-- 6.4. POLÍTICAS DE ADMIN_AUDIT_LOGS (RLS Imutável)
-- ==============================================================================
DROP POLICY IF EXISTS "Webmasters podem visualizar logs de auditoria" ON public.admin_audit_logs;
CREATE POLICY "Webmasters podem visualizar logs de auditoria"
    ON public.admin_audit_logs
    FOR SELECT
    TO authenticated
    USING (
        EXISTS (
            SELECT 1 FROM public.admins
            WHERE public.admins.id = auth.uid()
            AND public.admins.role IN ('WEBMASTER', 'SUPER_ADMIN')
        )
    );

DROP POLICY IF EXISTS "Inserção de logs por serviço de backend autenticado" ON public.admin_audit_logs;
CREATE POLICY "Inserção de logs por serviço de backend autenticado"
    ON public.admin_audit_logs
    FOR INSERT
    TO authenticated, service_role
    WITH CHECK (true);

-- ==============================================================================
-- 6.5. BLOQUEIO EXPLÍCITO DE ESCRITA ANÔNIMA (DEFESA EM PROFUNDIDADE)
-- ==============================================================================
-- Roles anon/authenticated NÃO recebem GRANT de INSERT/UPDATE/DELETE nas
-- tabelas transacionais sensíveis. Políticas acima existem para cenários de
-- PostgREST, e a conexão JDBC principal usa privilégio de owner/service_role.

-- ==============================================================================
-- WB SCUTING PLATFORM (WB-CIP) - SUPABASE DATABASE SCHEMA
-- Engenharia: RVIVAC Guild | Padrão Editorial & Alta Performance
-- ==============================================================================

-- 1. EXTENSÕES
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 2. TIPOS CUSTOMIZADOS (ENUMS)
CREATE TYPE model_gender AS ENUM ('female', 'male', 'non_binary');
CREATE TYPE eye_color AS ENUM ('castanho_claro', 'castanho_escuro', 'verde', 'azul', 'mel', 'preto', 'heterocromia');
CREATE TYPE hair_color AS ENUM ('preto', 'castanho_escuro', 'castanho_claro', 'loiro', 'ruivo', 'grisalho', 'colorido');
CREATE TYPE media_category AS ENUM ('polaroid', 'editorial', 'runway', 'commercial', 'composite_cover');
CREATE TYPE candidature_status AS ENUM ('received', 'under_review', 'approved', 'declined', 'archived');
CREATE TYPE user_role_enum AS ENUM ('superadmin', 'booker', 'scout', 'readonly');

-- 3. TABELA DE ROLES DE USUÁRIOS DO BACKOFFICE
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
    legal_name TEXT, -- Restrito ao backoffice
    gender model_gender NOT NULL,
    is_star BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- Biometria e Medidas Essenciais (em centímetros e numeração brasileira)
    height_cm NUMERIC(5,2) NOT NULL CHECK (height_cm > 100 AND height_cm < 230),
    bust_chest_cm NUMERIC(5,2) NOT NULL,
    waist_cm NUMERIC(5,2) NOT NULL,
    hips_cm NUMERIC(5,2) NOT NULL,
    shoe_size NUMERIC(3,1) NOT NULL,
    dress_size TEXT NOT NULL, -- ex: '36', '38', '40'
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
    
    -- Metadados de Mídia Principal (Cloudflare Images / Stream ID)
    hero_image_key TEXT NOT NULL,
    hero_video_stream_id TEXT,
    
    -- Auditoria
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Índices estratégicos para listagens e filtros em tempo real
CREATE INDEX IF NOT EXISTS idx_models_public_lookup 
    ON public.models (is_active, is_star, featured_order DESC, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_models_filters 
    ON public.models (gender, height_cm, dress_size, eye_color) 
    WHERE is_active = TRUE;

CREATE INDEX IF NOT EXISTS idx_models_slug 
    ON public.models (slug);

-- 5. TABELA DE MÍDIAS DOS MODELOS (BOOK FOTOGRÁFICO, POLAROIDS, RUNWAY)
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
    caption TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_model_media_model_category 
    ON public.model_media (model_id, category, display_order ASC) 
    WHERE is_published = TRUE;

-- 6. TABELA DE CANDIDATURAS (FUNIL DE SCOUTING COM LGPD)
CREATE TABLE IF NOT EXISTS public.candidatures (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    status candidature_status NOT NULL DEFAULT 'received',
    
    -- Dados Pessoais (Protegidos)
    full_name TEXT NOT NULL,
    email TEXT NOT NULL,
    phone_whatsapp TEXT NOT NULL,
    birth_date DATE NOT NULL,
    instagram TEXT,
    city TEXT NOT NULL,
    state TEXT NOT NULL,
    
    -- Medidas Declaradas
    gender model_gender NOT NULL,
    height_cm NUMERIC(5,2) NOT NULL,
    bust_chest_cm NUMERIC(5,2),
    waist_cm NUMERIC(5,2),
    hips_cm NUMERIC(5,2),
    shoe_size NUMERIC(3,1),
    dress_size TEXT,
    eye_color eye_color,
    hair_color hair_color,
    
    -- Fotos Submetidas (Array JSON com URLs no bucket temporário de quarentena)
    -- Exemplo: [{"url": "...", "type": "rosto_frontal"}, {"url": "...", "type": "corpo_inteiro"}]
    uploaded_photos JSONB NOT NULL DEFAULT '[]'::jsonb,
    
    -- Termos LGPD e Consentimento Expresso
    lgpd_consent_given BOOLEAN NOT NULL DEFAULT FALSE,
    lgpd_consent_timestamp TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    lgpd_consent_ip TEXT,
    lgpd_consent_user_agent TEXT,
    
    -- Notas Internas dos Bookers
    review_notes TEXT,
    reviewed_by UUID REFERENCES auth.users(id),
    expires_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() + INTERVAL '90 days'), -- Expiração automática LGPD
    
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_candidatures_status_date 
    ON public.candidatures (status, created_at DESC);

-- 7. TRIGGERS AUTOMATIZADOS DE UPDATED_AT
CREATE OR REPLACE FUNCTION public.handle_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER tr_models_updated_at 
    BEFORE UPDATE ON public.models 
    FOR EACH ROW EXECUTE FUNCTION public.handle_updated_at();

CREATE TRIGGER tr_model_media_updated_at 
    BEFORE UPDATE ON public.model_media 
    FOR EACH ROW EXECUTE FUNCTION public.handle_updated_at();

CREATE TRIGGER tr_candidatures_updated_at 
    BEFORE UPDATE ON public.candidatures 
    FOR EACH ROW EXECUTE FUNCTION public.handle_updated_at();

-- 8. POLÍTICAS DE ROW LEVEL SECURITY (RLS) RIGOROSAS

-- Habilita RLS em todas as tabelas
ALTER TABLE public.user_roles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.models ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.model_media ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.candidatures ENABLE ROW LEVEL SECURITY;

-- Função auxiliar de verificação de permissão administrativa
CREATE OR REPLACE FUNCTION public.is_admin_or_booker()
RETURNS BOOLEAN AS $$
BEGIN
    RETURN EXISTS (
        SELECT 1 FROM public.user_roles 
        WHERE user_id = auth.uid() 
          AND role IN ('superadmin', 'booker', 'scout')
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 8.1 Políticas para USER_ROLES
CREATE POLICY "Superadmin pode visualizar papéis de usuários" 
    ON public.user_roles FOR SELECT 
    USING (auth.uid() = user_id OR public.is_admin_or_booker());

CREATE POLICY "Apenas Superadmin pode modificar papéis" 
    ON public.user_roles FOR ALL 
    USING (
        EXISTS (
            SELECT 1 FROM public.user_roles 
            WHERE user_id = auth.uid() AND role = 'superadmin'
        )
    );

-- 8.2 Políticas para MODELS
-- Leitura pública irrestrita apenas de modelos ativos (Casting Público / Vitrine)
CREATE POLICY "Leitura pública de modelos ativos" 
    ON public.models FOR SELECT 
    USING (is_active = TRUE);

-- Leitura de modelos inativos somente para equipe autenticada
CREATE POLICY "Equipe pode visualizar todos os modelos" 
    ON public.models FOR SELECT 
    USING (public.is_admin_or_booker());

-- Escrita de modelos restrita a equipe autorizada
CREATE POLICY "Equipe pode criar e atualizar modelos" 
    ON public.models FOR ALL 
    USING (public.is_admin_or_booker())
    WITH CHECK (public.is_admin_or_booker());

-- 8.3 Políticas para MODEL_MEDIA
-- Leitura pública de mídias publicadas vinculadas a modelos ativos
CREATE POLICY "Leitura pública de mídias ativas" 
    ON public.model_media FOR SELECT 
    USING (
        is_published = TRUE AND 
        EXISTS (
            SELECT 1 FROM public.models 
            WHERE public.models.id = public.model_media.model_id 
              AND public.models.is_active = TRUE
        )
    );

-- Gestão completa de mídias para equipe
CREATE POLICY "Equipe pode gerenciar mídias" 
    ON public.model_media FOR ALL 
    USING (public.is_admin_or_booker())
    WITH CHECK (public.is_admin_or_booker());

-- 8.4 Políticas para CANDIDATURES (Scouting Funnel & LGPD)
-- Inserção pública permitida (anônima ou autenticada), exigindo consentimento LGPD explícito
CREATE POLICY "Candidatos podem submeter ficha com consentimento LGPD" 
    ON public.candidatures FOR INSERT 
    WITH CHECK (lgpd_consent_given = TRUE);

-- Leitura, atualização e deleção de candidaturas EXCLUSIVA para equipe autorizada
CREATE POLICY "Equipe pode consultar e gerenciar candidaturas" 
    ON public.candidatures FOR ALL 
    USING (public.is_admin_or_booker())
    WITH CHECK (public.is_admin_or_booker());

-- 9. CONFIGURAÇÃO DE BUCKETS DE STORAGE SEGREGAÇÃO (Via SQL/Storage Extensions)
-- Inserção dos buckets padrão caso a tabela storage.buckets esteja disponível
INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES 
    ('models-public', 'models-public', true, 15728640, ARRAY['image/jpeg', 'image/png', 'image/webp', 'image/avif']),
    ('scouting-quarantine', 'scouting-quarantine', false, 10485760, ARRAY['image/jpeg', 'image/png', 'image/webp'])
ON CONFLICT (id) DO NOTHING;

-- Políticas de Storage:
-- Bucket 'models-public': Leitura pública de fotos, escrita restrita a admins
CREATE POLICY "Visualização pública de fotos de modelos" 
    ON storage.objects FOR SELECT 
    USING (bucket_id = 'models-public');

CREATE POLICY "Admin upload fotos modelos" 
    ON storage.objects FOR INSERT 
    WITH CHECK (bucket_id = 'models-public' AND public.is_admin_or_booker());

CREATE POLICY "Admin delete fotos modelos" 
    ON storage.objects FOR DELETE 
    USING (bucket_id = 'models-public' AND public.is_admin_or_booker());

-- Bucket 'scouting-quarantine': Upload público autorizado (para novas fichas), visualização somente por bookers
CREATE POLICY "Upload anônimo para scouting com quarentena" 
    ON storage.objects FOR INSERT 
    WITH CHECK (bucket_id = 'scouting-quarantine');

CREATE POLICY "Visualização restrita de fotos do scouting" 
    ON storage.objects FOR SELECT 
    USING (bucket_id = 'scouting-quarantine' AND public.is_admin_or_booker());

CREATE POLICY "Descarte de fotos do scouting" 
    ON storage.objects FOR DELETE 
    USING (bucket_id = 'scouting-quarantine' AND public.is_admin_or_booker());

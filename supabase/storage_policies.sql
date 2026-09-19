-- ==============================================================================
-- WB SCOUTING PLATFORM - SUPABASE STORAGE PROVISIONING & RLS POLICIES
-- Target Schema: storage.buckets & storage.objects
-- Role: Cloud Infrastructure & Security Engineer (Supabase / PostgreSQL)
-- Compliance: Strict LGPD (Quarantine Storage) & CDN Public Distribution
-- ==============================================================================

-- 1. FUNÇÃO AUXILIAR DE SEGURANÇA (VERIFICAÇÃO DE ADMINISTRADOR ATIVO)
-- Permite validar se a requisição provém da 'service_role' (Backend Java / Spring Boot)
-- ou de um usuário autenticado listado como ativo na tabela public.admins.
CREATE OR REPLACE FUNCTION public.is_active_admin()
RETURNS BOOLEAN
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
    -- 1. Permite acesso irrestrito para chamadas com a service_key (Backend Java / Spring Data)
    IF auth.role() = 'service_role' THEN
        RETURN TRUE;
    END IF;

    -- 2. Verifica se o usuário autenticado no Supabase Auth está ativo em public.admins
    RETURN EXISTS (
        SELECT 1 
        FROM public.admins 
        WHERE (id = auth.uid() OR email = (auth.jwt() ->> 'email'))
          AND is_active = TRUE
    );
END;
$$;

-- 2. PROVISIONAMENTO IDEMPOTENTE DOS BUCKETS DE ARMAZENAMENTO
-- Utiliza ON CONFLICT (id) DO UPDATE para permitir re-execuções sem erros de chave duplicada.
INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES 
    (
        'site-assets',
        'site-assets',
        TRUE,
        26214400, -- 25 MB
        ARRAY['image/jpeg', 'image/png', 'image/webp', 'video/mp4', 'video/webm']
    ),
    (
        'models-media',
        'models-media',
        TRUE,
        8388608,  -- 8 MB
        ARRAY['image/jpeg', 'image/png', 'image/webp']
    ),
    (
        'candidates-uploads',
        'candidates-uploads',
        FALSE,    -- Bucket PRIVADO (Sem acesso direto via URL pública - Proteção LGPD)
        5242880,  -- 5 MB
        ARRAY['image/jpeg', 'image/png', 'image/webp']
    )
ON CONFLICT (id) DO UPDATE SET 
    public = EXCLUDED.public,
    file_size_limit = EXCLUDED.file_size_limit,
    allowed_mime_types = EXCLUDED.allowed_mime_types;

-- 3. HABILITAÇÃO DO ROW LEVEL SECURITY (RLS)
ALTER TABLE storage.objects ENABLE ROW LEVEL SECURITY;

-- ==============================================================================
-- 4. POLÍTICAS DE CONTROLE DE ACESSO (RLS EM storage.objects)
-- ==============================================================================

-- ------------------------------------------------------------------------------
-- 4.1. POLÍTICAS DE LEITURA (SELECT)
-- ------------------------------------------------------------------------------

-- Política 1.1: Leitura pública irrestrita para assets institucionais e fotos de casting
-- Justificativa: Permite que o Next.js e o CDN entreguem mídias públicas a visitantes anônimos.
DROP POLICY IF EXISTS "Public Read Access for Public Buckets" ON storage.objects;
CREATE POLICY "Public Read Access for Public Buckets"
    ON storage.objects
    FOR SELECT
    TO anon, authenticated
    USING (bucket_id IN ('site-assets', 'models-media'));

-- Política 1.2: Leitura restrita e criptografada para fotos de candidatos (LGPD)
-- Justificativa: Bloqueia totalmente 'anon'. Apenas o backend Java (service_role) e admins ativos visualizam.
DROP POLICY IF EXISTS "Restricted Admin Read for Candidates Uploads" ON storage.objects;
CREATE POLICY "Restricted Admin Read for Candidates Uploads"
    ON storage.objects
    FOR SELECT
    TO authenticated, service_role
    USING (
        bucket_id = 'candidates-uploads' 
        AND public.is_active_admin()
    );

-- ------------------------------------------------------------------------------
-- 4.2. POLÍTICAS DE UPLOAD (INSERT)
-- ------------------------------------------------------------------------------

-- Política 2.1: Inscrição de Novos Talentos (Upload Anônimo em Quarentena)
-- Justificativa: Visitantes anônimos precisam enviar fotos no formulário "Quero ser modelo".
-- O upload é isolado no bucket privado 'candidates-uploads'.
DROP POLICY IF EXISTS "Public Intake Insert for Candidates Uploads" ON storage.objects;
CREATE POLICY "Public Intake Insert for Candidates Uploads"
    ON storage.objects
    FOR INSERT
    TO anon, authenticated, service_role
    WITH CHECK (
        bucket_id = 'candidates-uploads'
        AND (storage.foldername(name))[1] IS NOT NULL -- Exige organização em pastas/prefixos
    );

-- Política 2.2: Upload de Mídias de Modelos e Assets do Site
-- Justificativa: Bloqueia completamente inserções anônimas em buckets públicos. Exige admin ou backend Java.
DROP POLICY IF EXISTS "Admin Insert for Models Media and Site Assets" ON storage.objects;
CREATE POLICY "Admin Insert for Models Media and Site Assets"
    ON storage.objects
    FOR INSERT
    TO authenticated, service_role
    WITH CHECK (
        bucket_id IN ('site-assets', 'models-media')
        AND public.is_active_admin()
    );

-- ------------------------------------------------------------------------------
-- 4.3. POLÍTICAS DE ATUALIZAÇÃO E EXCLUSÃO (UPDATE / DELETE)
-- ------------------------------------------------------------------------------

-- Política 3.1: Modificação de Objetos (UPDATE)
-- Justificativa: Apenas administradores ativos e o backend Spring podem modificar metadados/arquivos.
DROP POLICY IF EXISTS "Admin Update for All Managed Buckets" ON storage.objects;
CREATE POLICY "Admin Update for All Managed Buckets"
    ON storage.objects
    FOR UPDATE
    TO authenticated, service_role
    USING (
        bucket_id IN ('site-assets', 'models-media', 'candidates-uploads')
        AND public.is_active_admin()
    )
    WITH CHECK (
        bucket_id IN ('site-assets', 'models-media', 'candidates-uploads')
        AND public.is_active_admin()
    );

-- Política 3.2: Exclusão de Objetos (DELETE)
-- Justificativa: Bloqueia qualquer deleção por usuários anônimos.
-- Permite exclusão para o backend Java (ex: expiração de candidatos após 90 dias ou limpeza de fotos).
DROP POLICY IF EXISTS "Admin Delete for All Managed Buckets" ON storage.objects;
CREATE POLICY "Admin Delete for All Managed Buckets"
    ON storage.objects
    FOR DELETE
    TO authenticated, service_role
    USING (
        bucket_id IN ('site-assets', 'models-media', 'candidates-uploads')
        AND public.is_active_admin()
    );

-- ==============================================================================
-- WB SCOUTING / WB AGENCY - EAP-SEG-002: LOTE 1
-- MIGRATION: 20260930_eap_seg_002_storage_and_auth_hardening.sql
-- Objetivo:
--   1.1. Revogar todas as policies de escrita para 'anon' no Supabase Storage e tabelas.
--   1.2. Blindar função public.is_active_admin() com SECURITY DEFINER e search_path seguro.
--   1.3. Forçar bucket 'candidates-uploads' como estritamente privado (LGPD).
-- ==============================================================================

-- 1. Blindagem da função de validação administrativa
CREATE OR REPLACE FUNCTION public.is_active_admin()
RETURNS BOOLEAN
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
BEGIN
    -- Permite acesso total para chamadas administrativas com service_role (Backend Spring Boot)
    IF auth.role() = 'service_role' THEN
        RETURN TRUE;
    END IF;

    -- Verifica na tabela public.admins se o e-mail do JWT corresponde a um admin ativo
    RETURN EXISTS (
        SELECT 1 
        FROM public.admins a
        WHERE a.email = (auth.jwt() ->> 'email')
          AND a.is_active = TRUE
    );
END;
$$;

-- 2. Garantir bucket 'candidates-uploads' como estritamente PRIVADO
UPDATE storage.buckets 
SET public = FALSE 
WHERE id = 'candidates-uploads';

-- 3. Revogação de todas as policies que permitiam INSERT/UPDATE/DELETE para role 'anon' no Storage
DROP POLICY IF EXISTS "Candidates Uploads Public Insert" ON storage.objects;
DROP POLICY IF EXISTS "Public Intake Insert for Candidates Uploads" ON storage.objects;
DROP POLICY IF EXISTS "Allow anon upload" ON storage.objects;
DROP POLICY IF EXISTS "Anon Uploads" ON storage.objects;
DROP POLICY IF EXISTS "Anon Insert" ON storage.objects;
DROP POLICY IF EXISTS "Anon Delete" ON storage.objects;
DROP POLICY IF EXISTS "Anon Update" ON storage.objects;

DO $$
DECLARE
    pol RECORD;
BEGIN
    FOR pol IN (
        SELECT policyname 
        FROM pg_policies 
        WHERE schemaname = 'storage' 
          AND tablename = 'objects' 
          AND 'anon' = ANY(roles) 
          AND cmd IN ('INSERT', 'UPDATE', 'DELETE')
    ) LOOP
        EXECUTE format('DROP POLICY IF EXISTS %I ON storage.objects', pol.policyname);
    END LOOP;
END $$;

-- 4. Criar policy restrita para upload em 'candidates-uploads' exclusivo via service_role ou admin ativo
DROP POLICY IF EXISTS "Restricted Intake Insert for Candidates Uploads" ON storage.objects;
CREATE POLICY "Restricted Intake Insert for Candidates Uploads"
    ON storage.objects
    FOR INSERT
    TO service_role, authenticated
    WITH CHECK (
        bucket_id = 'candidates-uploads'
        AND (auth.role() = 'service_role' OR public.is_active_admin())
    );

-- 5. Habilitar RLS nas tabelas transacionais do schema public e revogar escritas anônimas diretas
DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'candidates') THEN
        ALTER TABLE public.candidates ENABLE ROW LEVEL SECURITY;
        REVOKE INSERT, UPDATE, DELETE ON public.candidates FROM anon;
    END IF;

    IF EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'candidate_submissions') THEN
        ALTER TABLE public.candidate_submissions ENABLE ROW LEVEL SECURITY;
        REVOKE INSERT, UPDATE, DELETE ON public.candidate_submissions FROM anon;
    END IF;

    IF EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'admins') THEN
        ALTER TABLE public.admins ENABLE ROW LEVEL SECURITY;
        REVOKE INSERT, UPDATE, DELETE, TRUNCATE ON public.admins FROM anon;
    END IF;
END $$;

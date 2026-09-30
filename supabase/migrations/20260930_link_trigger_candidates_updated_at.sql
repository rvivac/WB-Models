-- ==============================================================================
-- MIGRATION: 20260930_link_trigger_candidates_updated_at.sql
-- Module: Scouting Funnel / Candidates Auditing (EAP-REM-1.2)
-- Target Engine: PostgreSQL 14+ / Supabase
-- Description: Vincula o trigger de atualização automática de updated_at à tabela public.candidates
-- ==============================================================================

-- 1. Garante a existência da função genérica de timestamp
CREATE OR REPLACE FUNCTION public.fn_set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 2. Vinculação idempotente do trigger na tabela public.candidates
DROP TRIGGER IF EXISTS trg_candidates_updated_at ON public.candidates;

CREATE TRIGGER trg_candidates_updated_at
    BEFORE UPDATE ON public.candidates
    FOR EACH ROW
    EXECUTE FUNCTION public.fn_set_updated_at();

-- 3. Caso exista a tabela candidate_submissions, garante também a amarração padronizada
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'candidate_submissions') THEN
        DROP TRIGGER IF EXISTS trg_candidate_submissions_updated_at ON public.candidate_submissions;
        CREATE TRIGGER trg_candidate_submissions_updated_at
            BEFORE UPDATE ON public.candidate_submissions
            FOR EACH ROW
            EXECUTE FUNCTION public.fn_set_updated_at();
    END IF;
END $$;

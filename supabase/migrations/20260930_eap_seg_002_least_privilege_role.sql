-- ==============================================================================
-- EAP-SEG-002: ITEM 18 - ROLE DE BANCO DE DADOS DEDICADO COM PRINCÍPIO DO MENOR PRIVILÉGIO
-- Target Engine: PostgreSQL 14+ / Supabase
-- ==============================================================================

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'wb_app_user') THEN
        -- Criação da role sem privilégios de superusuário nem bypassrls
        CREATE ROLE wb_app_user WITH LOGIN PASSWORD 'ChangeMeInProduction_WbAppUser2026!' NOSUPERUSER NOCREATEDB NOCREATEROLE NOBYPASSRLS;
        RAISE NOTICE 'Role wb_app_user criada com sucesso.';
    ELSE
        RAISE NOTICE 'Role wb_app_user já existe.';
    END IF;
END $$;

-- 1. Permissão de conexão ao banco de dados
GRANT CONNECT ON DATABASE postgres TO wb_app_user;

-- 2. Concessão estrita de USAGE apenas no schema 'public' (proíbe acesso direto a auth, storage, pg_catalog)
GRANT USAGE ON SCHEMA public TO wb_app_user;

-- 3. Concessão de DML apenas nas tabelas transacionais existentes do schema public
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO wb_app_user;

-- 4. Concessão de uso de sequências no schema public
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO wb_app_user;

-- 5. Privilégios padrão para novos objetos que vierem a ser criados no schema public
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO wb_app_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT USAGE, SELECT ON SEQUENCES TO wb_app_user;

-- 6. Comentário defensivo de auditoria
COMMENT ON ROLE wb_app_user IS 'Role da aplicação Spring Boot WB Agency com privilégios mínimos restritos ao schema public (Item 18 - EAP-SEG-002)';

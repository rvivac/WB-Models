package com.wbscouting.api;

import org.junit.jupiter.api.Test;
import java.io.File;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

public class DatabaseConnectionTest {

    @Test
    void testDatabaseConnectionAndSchema() {
        String url = System.getProperty("spring.datasource.url");
        String user = System.getProperty("spring.datasource.username");
        String pass = System.getProperty("spring.datasource.password");

        // Fallback para .env local se não informado via -D
        if (pass == null || pass.isBlank()) {
            pass = System.getenv("DB_PASSWORD");
        }

        if (pass == null || pass.isBlank()) {
            File envFile = new File(".env");
            if (!envFile.exists()) {
                envFile = new File("backend/.env");
            }
            if (envFile.exists()) {
                try (FileInputStream fis = new FileInputStream(envFile)) {
                    Properties props = new Properties();
                    props.load(fis);
                    if (pass == null || pass.isBlank()) pass = props.getProperty("DB_PASSWORD");
                    if (user == null || user.isBlank()) user = props.getProperty("DB_USERNAME");
                    if (url == null || url.isBlank()) url = props.getProperty("DB_URL");
                } catch (Exception ignored) {}
            }
        }

        if (url == null || url.isBlank()) {
            url = "jdbc:postgresql://aws-0-sa-east-1.pooler.supabase.com:6543/postgres?sslmode=require&prepareThreshold=0";
        }
        if (user == null || user.isBlank()) {
            user = "postgres.stmytwsdlonpnirqiufq";
        }

        System.out.println("=================================================================");
        System.out.println("TESTANDO CONEXÃO COM POSTGRESQL (SUPABASE)");
        System.out.println("URL: " + url);
        System.out.println("USER: " + user);
        System.out.println("SENHA CONFIGURADA: " + (pass != null && !pass.isBlank() ? "****** (tamanho: " + pass.length() + ")" : "NÃO DEFINIDA"));
        System.out.println("=================================================================");

        assertNotNull(pass, "A senha do banco de dados (DB_PASSWORD) não pode ser nula.");
        assertFalse(pass.isBlank(), "A senha do banco de dados (DB_PASSWORD) não pode estar em branco.");

        try {
            Class.forName("org.postgresql.Driver");
            try (Connection conn = DriverManager.getConnection(url, user, pass);
                 Statement stmt = conn.createStatement()) {

                assertNotNull(conn, "A conexão não deve ser nula.");
                assertFalse(conn.isClosed(), "A conexão deve estar aberta.");
                System.out.println("[OK] Conexão física estabelecida com sucesso com o Supabase!");

                // Validar consulta básica
                try (ResultSet rs = stmt.executeQuery("SELECT version();")) {
                    if (rs.next()) {
                        System.out.println("[OK] PostgreSQL Version: " + rs.getString(1));
                    }
                }

                // Validar tabelas principais da EAP
                String checkTablesSql = "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' AND table_name IN ('admins', 'models', 'model_media', 'candidates', 'candidate_submissions', 'site_contents');";
                try (ResultSet rs = stmt.executeQuery(checkTablesSql)) {
                    System.out.println("[OK] Tabelas encontradas no schema public:");
                    int count = 0;
                    while (rs.next()) {
                        count++;
                        System.out.println("   - " + rs.getString(1));
                    }
                    System.out.println("Total de tabelas essenciais validadas: " + count);
                }

                // Validar trigger updated_at na tabela candidates
                String checkTriggerSql = "SELECT tgname FROM pg_trigger WHERE tgname = 'trg_candidates_updated_at';";
                boolean triggerExists = false;
                try (ResultSet rs = stmt.executeQuery(checkTriggerSql)) {
                    if (rs.next()) {
                        triggerExists = true;
                        System.out.println("[OK] Trigger 'trg_candidates_updated_at' está ativo no banco remoto!");
                    } else {
                        System.out.println("[INFO] Trigger 'trg_candidates_updated_at' ainda não aplicada no banco remoto.");
                    }
                }

                // Aplicação da Migração se ausente ou solicitado
                String applyMigration = System.getProperty("applyMigration", "true");
                if (!triggerExists && "true".equalsIgnoreCase(applyMigration)) {
                    System.out.println("[MIGRAÇÃO] Aplicando DDL de '20260930_link_trigger_candidates_updated_at.sql'...");
                    String migrationDdl = """
                        CREATE OR REPLACE FUNCTION public.fn_set_updated_at()
                        RETURNS TRIGGER AS $$
                        BEGIN
                            NEW.updated_at = NOW();
                            RETURN NEW;
                        END;
                        $$ LANGUAGE plpgsql;

                        DROP TRIGGER IF EXISTS trg_candidates_updated_at ON public.candidates;

                        CREATE TRIGGER trg_candidates_updated_at
                            BEFORE UPDATE ON public.candidates
                            FOR EACH ROW
                            EXECUTE FUNCTION public.fn_set_updated_at();

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
                    """;
                    stmt.execute(migrationDdl);
                    System.out.println("[MIGRAÇÃO] DDL executada com sucesso!");

                    // Re-verificar trigger
                    try (ResultSet rs = stmt.executeQuery(checkTriggerSql)) {
                        if (rs.next()) {
                            System.out.println("[OK] Trigger 'trg_candidates_updated_at' verificado e ativo com sucesso após migração!");
                        } else {
                            fail("Falha: Trigger 'trg_candidates_updated_at' não foi localizado após execução da migração.");
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[FALHA DE CONEXÃO] Erro ao conectar ao banco de dados: " + e.getMessage());
            fail("Falha ao autenticar/conectar no Supabase com as credenciais fornecidas: " + e.getMessage());
        }
    }
}

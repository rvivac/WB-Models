package com.wbscouting.api.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("EAP-SEG-002: Lote 1 - Auditoria e Aplicação de Hardening de RLS e Storage")
public class SupabaseRlsHardeningTest {

    private Connection getDatabaseConnection() throws Exception {
        String url = System.getProperty("spring.datasource.url");
        String user = System.getProperty("spring.datasource.username");
        String pass = System.getProperty("spring.datasource.password");

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

        assertNotNull(pass, "DB_PASSWORD não encontrada.");
        return DriverManager.getConnection(url, user, pass);
    }

    @Test
    @DisplayName("1.1 e 1.2. Deve aplicar migration de hardening e auditar ausência de policies anônimas de escrita")
    void testApplyHardeningAndAuditStorageAndRls() throws Exception {
        Path migrationPath = Paths.get("..", "supabase", "migrations", "20260930_eap_seg_002_storage_and_auth_hardening.sql");
        if (!Files.exists(migrationPath)) {
            migrationPath = Paths.get("supabase", "migrations", "20260930_eap_seg_002_storage_and_auth_hardening.sql");
        }
        assertTrue(Files.exists(migrationPath), "Arquivo de migration deve existir: " + migrationPath);

        String migrationSql = Files.readString(migrationPath, StandardCharsets.UTF_8);

        try (Connection conn = getDatabaseConnection();
             Statement stmt = conn.createStatement()) {

            // 1. Executa a migration
            System.out.println("Executando migration de hardening de RLS e Storage...");
            stmt.execute(migrationSql);
            System.out.println("Migration executada com sucesso!");

            // 2. Valida que candidates-uploads é estritamente privado
            try (ResultSet rs = stmt.executeQuery("SELECT public FROM storage.buckets WHERE id = 'candidates-uploads'")) {
                if (rs.next()) {
                    boolean isPublic = rs.getBoolean("public");
                    assertFalse(isPublic, "Bucket 'candidates-uploads' deve ser estritamente privado (public = FALSE)");
                    System.out.println("Bucket 'candidates-uploads' verificado: public = FALSE (LGPD Compliant)");
                } else {
                    System.out.println("Bucket 'candidates-uploads' ainda não provisionado em storage.buckets.");
                }
            }

            // 3. Valida que não existe nenhuma policy em storage.objects permitindo escrita (INSERT/UPDATE/DELETE) para role 'anon'
            String policyCheckSql = """
                SELECT policyname, cmd, roles
                FROM pg_policies
                WHERE schemaname = 'storage'
                  AND tablename = 'objects'
                  AND 'anon' = ANY(roles)
                  AND cmd IN ('INSERT', 'UPDATE', 'DELETE')
                """;

            try (ResultSet rs = stmt.executeQuery(policyCheckSql)) {
                boolean hasAnonWritePolicy = false;
                while (rs.next()) {
                    hasAnonWritePolicy = true;
                    System.err.println("ALERTA: Policy insegura detectada: " + rs.getString("policyname")
                            + " cmd: " + rs.getString("cmd"));
                }
                assertFalse(hasAnonWritePolicy, "Nenhuma policy em storage.objects deve permitir INSERT/UPDATE/DELETE para 'anon'");
                System.out.println("Auditoria de storage.objects: ZERO policies de escrita para 'anon'.");
            }

            // 4. Valida se a função public.is_active_admin() possui SECURITY DEFINER
            String funcCheckSql = """
                SELECT prosecdef 
                FROM pg_proc p 
                JOIN pg_namespace n ON n.oid = p.pronamespace 
                WHERE n.nspname = 'public' 
                  AND p.proname = 'is_active_admin'
                """;

            try (ResultSet rs = stmt.executeQuery(funcCheckSql)) {
                assertTrue(rs.next(), "A função public.is_active_admin() deve existir no banco de dados");
                boolean isSecDefiner = rs.getBoolean("prosecdef");
                assertTrue(isSecDefiner, "A função public.is_active_admin() deve ser configurada com SECURITY DEFINER");
                System.out.println("Função public.is_active_admin() validada com SECURITY DEFINER!");
            }
        }
    }
}

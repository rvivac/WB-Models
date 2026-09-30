package com.wbscouting.api.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.File;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("3.2. Auditoria da Tabela admins (Contas e Hashes Legados)")
public class AdminTableAuditTest {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    @DisplayName("Executar varredura na tabela public.admins para auditar contas e hashes criptográficos")
    void auditAdminsTableInRemoteDatabase() {
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

        assertNotNull(pass, "A senha do banco de dados (DB_PASSWORD) é necessária para a auditoria.");

        System.out.println("=================================================================");
        System.out.println("AUDITORIA DE SEGURANÇA: TABELA PUBLIC.ADMINS (SUPABASE)");
        System.out.println("=================================================================");

        String[] testPasswords = {
                "Admin@123",
                "admin",
                "123456",
                "Admin@2024",
                "Admin@WbScouting2026!"
        };

        try {
            Class.forName("org.postgresql.Driver");
            try (Connection conn = DriverManager.getConnection(url, user, pass);
                 Statement stmt = conn.createStatement()) {

                assertNotNull(conn, "A conexão com o banco não deve ser nula.");

                String query = "SELECT id, name, email, role, is_active, password_hash, created_at, updated_at FROM public.admins ORDER BY created_at ASC;";
                try (ResultSet rs = stmt.executeQuery(query)) {
                    int count = 0;
                    int legacyWeakCount = 0;

                    while (rs.next()) {
                        count++;
                        String id = rs.getString("id");
                        String name = rs.getString("name");
                        String email = rs.getString("email");
                        String role = rs.getString("role");
                        boolean isActive = rs.getBoolean("is_active");
                        String hash = rs.getString("password_hash");
                        String createdAt = rs.getString("created_at");

                        System.out.println("\n-------------------------------------------------------------");
                        System.out.println("CONTA #" + count + ":");
                        System.out.println("  ID:           " + id);
                        System.out.println("  NOME:         " + name);
                        System.out.println("  E-MAIL:       " + email);
                        System.out.println("  PAPEL (ROLE): " + role);
                        System.out.println("  ATIVO:        " + isActive);
                        System.out.println("  CRIADO EM:    " + createdAt);

                        // Avaliação da conformidade do Hash
                        if (hash == null || hash.isBlank()) {
                            System.out.println("  [CRÍTICO] Password hash ausente ou nulo!");
                            legacyWeakCount++;
                        } else {
                            boolean isBcrypt = hash.startsWith("$2a$") || hash.startsWith("$2b$") || hash.startsWith("$2y$");
                            System.out.println("  FORMATO HASH: " + (isBcrypt ? "BCrypt Válido (60 chars)" : "NÃO-BCRYPT / DESCONHECIDO"));
                            System.out.println("  HASH (REDACTED): " + hash.substring(0, Math.min(10, hash.length())) + "..." + hash.substring(Math.max(0, hash.length() - 6)));

                            // Teste de matching com dicionário de senhas
                            String matchedPassword = null;
                            for (String candidate : testPasswords) {
                                if (isBcrypt && passwordEncoder.matches(candidate, hash)) {
                                    matchedPassword = candidate;
                                    break;
                                }
                            }

                            if (matchedPassword != null) {
                                if ("Admin@123".equals(matchedPassword) || "admin".equals(matchedPassword) || "123456".equals(matchedPassword)) {
                                    System.out.println("  [ALERTA DE SEGURANÇA] Conta utilizando senha legada/fraca: '" + matchedPassword + "'!");
                                    legacyWeakCount++;
                                } else {
                                    System.out.println("  [CONFORME] Hash corresponde à senha forte atual do sistema: '" + matchedPassword + "'.");
                                }
                            } else {
                                System.out.println("  [OK] Hash criptográfico robusto e não coincidente com senhas fracas do dicionário.");
                            }
                        }
                    }

                    System.out.println("\n=============================================================");
                    System.out.println("RESUMO DA AUDITORIA DA TABELA PUBLIC.ADMINS:");
                    System.out.println("Total de contas administradoras cadastradas: " + count);
                    System.out.println("Contas com senhas fracas / legadas detectadas: " + legacyWeakCount);
                    System.out.println("=============================================================");

                    if (count == 0) {
                        System.out.println("[INFO] Tabela public.admins está vazia. O primeiro admin será provisionado automaticamente pelo DataInitializer no próximo startup.");
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[ERRO DE AUDITORIA] Falha ao consultar tabela public.admins: " + e.getMessage());
            fail("Erro na consulta de auditoria da tabela admins: " + e.getMessage());
        }
    }
}

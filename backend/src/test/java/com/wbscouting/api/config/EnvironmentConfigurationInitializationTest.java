package com.wbscouting.api.config;

import com.wbscouting.api.security.JwtTokenProvider;
import io.jsonwebtoken.security.WeakKeyException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.io.ClassPathResource;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("4.2. Teste de Inicialização com Variáveis e sem Defaults no application.yml")
class EnvironmentConfigurationInitializationTest {

    private static final String VALID_TEST_JWT_SECRET =
            "5367566B5970337336763979244226452948404D6351655468576D5A71347437";

    @org.springframework.context.annotation.Configuration
    @org.springframework.boot.context.properties.EnableConfigurationProperties({SupabaseProperties.class, MailProperties.class})
    static class TestConfig {
    }

    @Test
    @DisplayName("Deve inicializar contexto e vincular propriedades quando variáveis de ambiente forem injetadas (Staging/Prod)")
    void shouldInitializeAndBindAllPropertiesWhenEnvironmentVariablesAreSupplied() {
        new ApplicationContextRunner()
                .withPropertyValues(
                        "server.port=9090",
                        "spring.datasource.url=jdbc:postgresql://db.wbscouting.internal:5432/wb_prod",
                        "spring.datasource.username=wb_app_user",
                        "spring.datasource.password=SuperSecretProdDbPassword#2026",
                        "app.security.jwt.secret=" + VALID_TEST_JWT_SECRET,
                        "app.security.jwt.expiration-ms=14400000",
                        "app.security.cors.allowed-origins=https://wbscouting.com,https://admin.wbscouting.com",
                        "supabase.url=https://prod-supabase.wbscouting.internal",
                        "supabase.service-role-key=prod-service-role-key-999",
                        "supabase.anon-key=prod-anon-key-111",
                        "supabase.key=prod-key-222",
                        "app.mail.from-address=noreply@wbscouting.com",
                        "app.mail.agency-notification-email=scouting@wbscouting.com"
                )
                .withUserConfiguration(
                        TestConfig.class,
                        StorageConfig.class,
                        JwtTokenProvider.class
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();

                    // Verificação de SupabaseProperties vinculadas às variáveis injetadas
                    SupabaseProperties supabaseProps = context.getBean(SupabaseProperties.class);
                    assertThat(supabaseProps.getUrl()).isEqualTo("https://prod-supabase.wbscouting.internal");
                    assertThat(supabaseProps.getServiceRoleKey()).isEqualTo("prod-service-role-key-999");
                    assertThat(supabaseProps.getEffectiveKey()).isEqualTo("prod-service-role-key-999");
                    assertThat(supabaseProps.isKeyConfigured()).isTrue();

                    // Verificação de MailProperties vinculadas
                    MailProperties mailProps = context.getBean(MailProperties.class);
                    assertThat(mailProps.getFromAddress()).isEqualTo("noreply@wbscouting.com");
                    assertThat(mailProps.getAgencyNotificationEmail()).isEqualTo("scouting@wbscouting.com");

                    // Verificação de StorageConfig / RestClient instanciado com sucesso
                    assertThat(context).hasBean("supabaseStorageRestClient");

                    // Verificação de JwtTokenProvider funcional com a chave injetada
                    JwtTokenProvider jwtTokenProvider = context.getBean(JwtTokenProvider.class);
                    org.springframework.security.core.userdetails.UserDetails userDetails =
                            org.springframework.security.core.userdetails.User.withUsername("admin@wbscouting.com")
                                    .password("unused")
                                    .roles("ADMIN")
                                    .build();
                    org.springframework.security.core.Authentication auth =
                            new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());
                    String testToken = jwtTokenProvider.generateToken(auth);
                    assertThat(testToken).isNotBlank();
                    assertThat(jwtTokenProvider.validateToken(testToken)).isTrue();
                    assertThat(jwtTokenProvider.getUsernameFromToken(testToken)).isEqualTo("admin@wbscouting.com");

                    // Verificação do Environment de propriedades do datasource e porta
                    assertThat(context.getEnvironment().getProperty("server.port")).isEqualTo("9090");
                    assertThat(context.getEnvironment().getProperty("spring.datasource.url"))
                            .isEqualTo("jdbc:postgresql://db.wbscouting.internal:5432/wb_prod");
                    assertThat(context.getEnvironment().getProperty("spring.datasource.username"))
                            .isEqualTo("wb_app_user");
                    assertThat(context.getEnvironment().getProperty("spring.datasource.password"))
                            .isEqualTo("SuperSecretProdDbPassword#2026");
                });
    }

    @Test
    @DisplayName("Deve falhar na inicialização (Fail-Fast) quando JWT_SECRET for omitido ou vazio sem default inseguro")
    void shouldFailContextInitializationWhenJwtSecretIsOmittedOrEmptyWithoutDefault() {
        new ApplicationContextRunner()
                .withPropertyValues(
                        "app.security.jwt.secret=", // Simula ausência de JWT_SECRET no ambiente
                        "app.security.jwt.expiration-ms=28800000"
                )
                .withUserConfiguration(JwtTokenProvider.class)
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(WeakKeyException.class);
                });
    }

    @Test
    @DisplayName("Deve validar que application.yml não contém segredos hardcoded como defaults")
    void shouldVerifyApplicationYmlContainsNoHardcodedSecretsAsDefaults() throws Exception {
        ClassPathResource resource = new ClassPathResource("application.yml");
        assertThat(resource.exists()).isTrue();

        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }

        // Validação da linha de senha do banco de dados (deve ser ${DB_PASSWORD:} vazia, sem fallback com texto)
        String dbPasswordLine = lines.stream()
                .filter(l -> l.contains("password:") && l.contains("DB_PASSWORD"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Linha de password do datasource com DB_PASSWORD não encontrada em application.yml"));

        assertThat(dbPasswordLine.trim())
                .isEqualTo("password: ${DB_PASSWORD:}")
                .as("A senha do banco de dados no application.yml deve usar '${DB_PASSWORD:}' sem fallback em texto plano");

        // Validação da linha de segredo JWT (deve ser ${JWT_SECRET:} vazia, sem fallback com texto)
        String jwtSecretLine = lines.stream()
                .filter(l -> l.contains("secret:") && l.contains("JWT_SECRET"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Linha de secret JWT com JWT_SECRET não encontrada em application.yml"));

        assertThat(jwtSecretLine.trim())
                .isEqualTo("secret: ${JWT_SECRET:}")
                .as("O segredo JWT no application.yml deve usar '${JWT_SECRET:}' sem fallback em texto plano");

        // Validação de ausência absoluta de resíduos legados no application.yml
        for (String line : lines) {
            assertThat(line)
                    .doesNotContain("ptyC15gru")
                    .doesNotContain("Admin@123")
                    .doesNotContain(":404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970")
                    .as("O application.yml não deve conter resíduos de senhas ou chaves legadas hardcoded");
        }
    }

    @Test
    @DisplayName("Deve gerenciar SupabaseProperties com segurança quando chaves não forem configuradas")
    void shouldHandleSupabasePropertiesGracefullyWhenKeysAreUnsetOrDummy() {
        new ApplicationContextRunner()
                .withPropertyValues(
                        "supabase.url=https://stmytwsdlonpnirqiufq.supabase.co",
                        "supabase.service-role-key=",
                        "supabase.key=",
                        "supabase.anon-key="
                )
                .withUserConfiguration(
                        TestConfig.class,
                        StorageConfig.class
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();

                    SupabaseProperties supabaseProps = context.getBean(SupabaseProperties.class);
                    assertThat(supabaseProps.isKeyConfigured()).isFalse();
                    assertThat(supabaseProps.getEffectiveKey()).isEqualTo("dummy-key");

                    // RestClient ainda é criado para permitir que a camada de storage ative o fallback local
                    assertThat(context).hasBean("supabaseStorageRestClient");
                });
    }

    @Test
    @DisplayName("4.3. Deve verificar que .gitignore ignora arquivos .env e preserva .env.example")
    void shouldVerifyGitignoreRulesIgnoreDotEnvFiles() throws Exception {
        java.nio.file.Path rootGitignore = java.nio.file.Paths.get("..", ".gitignore");
        if (!java.nio.file.Files.exists(rootGitignore)) {
            rootGitignore = java.nio.file.Paths.get(".gitignore");
        }
        assertThat(java.nio.file.Files.exists(rootGitignore)).isTrue();

        List<String> lines = java.nio.file.Files.readAllLines(rootGitignore, StandardCharsets.UTF_8);
        assertThat(lines).anyMatch(l -> l.trim().equals(".env"));
        assertThat(lines).anyMatch(l -> l.trim().equals(".env.*") || l.trim().equals("**/.env.*"));
        assertThat(lines).anyMatch(l -> l.trim().equals("!.env.example") || l.trim().equals("!**/.env.example"));
    }

    @Test
    @DisplayName("4.3. Deve verificar que .env.example não contém valores reais ou credenciais ativas")
    void shouldVerifyDotEnvExampleContainsNoRealValuesOrActiveSecrets() throws Exception {
        java.nio.file.Path envExamplePath = java.nio.file.Paths.get(".env.example");
        if (!java.nio.file.Files.exists(envExamplePath)) {
            envExamplePath = java.nio.file.Paths.get("backend", ".env.example");
        }
        assertThat(java.nio.file.Files.exists(envExamplePath)).isTrue();

        String content = java.nio.file.Files.readString(envExamplePath, StandardCharsets.UTF_8);

        // Não deve conter senhas ou chaves reais
        assertThat(content)
                .doesNotContain("U7MevYDCOEjBOBS3")
                .doesNotContain("ptyC15gru")
                .doesNotContain("Admin@123")
                .doesNotContain("Admin@WbScouting2026!")
                .doesNotContain("sb_secret_JTi2OnYNgP0K7_rKM9ZRXQ_QdjxMg0j")
                .doesNotContain("sb_publishable_YIExzbXgaVtJPVh1X9A7nQ_oECtRKFM")
                .doesNotContain("404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");

        // Deve conter placeholders explícitos para todas as credenciais sensíveis
        assertThat(content).contains("DB_PASSWORD=sua_senha_do_banco_aqui");
        assertThat(content).contains("JWT_SECRET=sua_chave_secreta_jwt_minimo_256_bits_base64_aqui");
        assertThat(content).contains("SUPABASE_KEY=sua_chave_supabase_aqui");
        assertThat(content).contains("SUPABASE_SERVICE_ROLE_KEY=sua_service_role_key_aqui");
        assertThat(content).contains("SUPABASE_ANON_KEY=sua_anon_key_aqui");
    }
}

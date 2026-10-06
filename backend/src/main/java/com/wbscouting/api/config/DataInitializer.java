package com.wbscouting.api.config;

import com.wbscouting.api.entity.Admin;
import com.wbscouting.api.entity.Model;
import com.wbscouting.api.enums.AdminRole;
import com.wbscouting.api.repository.AdminRepository;
import com.wbscouting.api.repository.ModelRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;

@Slf4j
@Component
public class DataInitializer implements CommandLineRunner {

    private final AdminRepository adminRepository;
    private final ModelRepository modelRepository;
    private final PasswordEncoder passwordEncoder;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    // ============================================================
    // Environment injectado via @Autowired field (nao final)
    // evita erro se @RequiredArgsConstructor do Lombok nao for gerado.
    // ============================================================
    @Autowired
    private Environment env;

    @Value("${app.security.initial-admin.name:Webmaster WB Agency}")
    private String defaultName;

    @Value("${app.security.initial-admin.email:webmaster@wbagency.com.br}")
    private String defaultEmail;

    @Value("${app.security.initial-admin.password:Admin@WbScouting2026!}")
    private String defaultPassword;

    // Constructor injection (Lombok @RequiredArgsConstructor nao de certeza no Wrapper)
    @Autowired
    public DataInitializer(
            AdminRepository adminRepository,
            ModelRepository modelRepository,
            PasswordEncoder passwordEncoder,
            org.springframework.jdbc.core.JdbcTemplate jdbcTemplate) {
        this.adminRepository = adminRepository;
        this.modelRepository = modelRepository;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        boolean isLocalDevProfile = isLocalOrDevProfile(env);

        // -------------- PASSO 1: Migracoes DDL APENAS em PROD (PostgreSQL/Supabase) --------------
        // Motivo blindagem: H2 NAO entende SQL PostgreSQL especifico
        // (IF NOT EXISTS em ALTER TABLE, schema public., cast ::media_type, gen_random_uuid()).
        if (!isLocalDevProfile) {
            migrateDatabaseSchemaPostgresOnly();
        } else {
            log.info("[DATA INIT][H2] Perfil LOCAL/DEV detectado. Pulando migracoes DDL nativas PostgreSQL.");
        }

        // -------------- PASSO 2: Garantir usuario ADMIN ativo --------------
        // Se count == 0, cria. Se for LOCAL, garante mesmo que ja exista (upsert).
        long adminCount = 0L;
        try { adminCount = adminRepository.count(); } catch (Exception e) { log.warn("[ADMIN INIT] Nao foi possivel contar admins. H2 ainda nao criou schema? Motivo: {}", e.getMessage()); }

        if (adminCount == 0L) {
            log.info("[ADMIN INIT] Nenhum admin encontrado. Criando admin inicial {}.", defaultEmail);
            criarAdminInicial();
        } else if (isLocalDevProfile) {
            // TASK: Ambiente LOCAL = sempre garantimos credencial coerente = application-local.yml.
            // Atualiza senha / role / nome caso exista por algum motivo (seed anterior).
            log.info("[ADMIN INIT][H2] Ambiente local. Garantindo credenciais admin {} via UPSERT.", defaultEmail);
            upsertAdminInicialLocal();
        } else {
            log.info("[ADMIN INIT] Admins ja existentes (count={}). Nenhuma acao.", adminCount);
        }

        // -------------- PASSO 3: Carga 2 a 4 modelos de exemplo (CATALOGO) --------------
        // Apenas se NAO tiver nenhum modelo salvo. Ideal para profile H2 limpo.
        long modelCount = 0L;
        try { modelCount = modelRepository.count(); } catch (Exception e) { log.warn("[MODEL INIT] Nao foi possivel contar models. Motivo: {}", e.getMessage()); }

        if (modelCount == 0L) {
            log.info("[MODEL INIT] Catálogo vazio. Inserindo 4 modelos de exemplo para H2 Local.");
            inserirModelosExemploH2();
        } else {
            log.info("[MODEL INIT] Catalogo com {} registros. Pulando carga de exemplos.", modelCount);
        }
    }

    private void criarAdminInicial() {
        Admin initialAdmin = Admin.builder()
                .name(defaultName)
                .email(defaultEmail.trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(defaultPassword))
                .role(AdminRole.WEBMASTER)
                .isActive(true)
                .mustChangePassword(false)
                .is2faEnabled(false)
                .build();
        adminRepository.save(initialAdmin);
        log.info("[ADMIN INIT] Admin CRIADO: {} [role={}, senha BCrypt OK, isActive=true] Login frontend = {} / {}",
                defaultEmail, AdminRole.WEBMASTER, defaultEmail, defaultPassword);
    }

    private void upsertAdminInicialLocal() {
        try {
            var opt = adminRepository.findByEmail(defaultEmail.trim().toLowerCase());
            if (opt.isPresent()) {
                Admin a = opt.get();
                a.setName(defaultName);
                a.setPasswordHash(passwordEncoder.encode(defaultPassword));
                a.setRole(AdminRole.WEBMASTER);
                a.setIsActive(true);
                a.setMustChangePassword(false);
                a.setIs2faEnabled(false);
                adminRepository.save(a);
                log.info("[ADMIN INIT][H2] Admin ATUALIZADO via UPSERT: {}. Senha atualizada.", defaultEmail);
            } else {
                criarAdminInicial();
            }
        } catch (Exception e) {
            log.warn("[ADMIN INIT][H2] Falhou findByEmail UPSERT local. Criando admin novo. Motivo: {}", e.getMessage());
            criarAdminInicial();
        }
    }

    private void inserirModelosExemploH2() {
        String[] nomes = {
                "Isabella Fontana",
                "Sophia Almeida",
                "Lorena Bittencourt",
                "Manuela Andrade"
        };
        int[] idades = { 19, 21, 20, 22 };
        int[] alturasCm = { 175, 178, 172, 180 };
        double[] busto = { 86.0, 88.0, 84.0, 90.0 };
        double[] cintura = { 61.0, 62.0, 60.0, 63.0 };
        double[] quadril = { 90.0, 92.0, 88.0, 94.0 };
        String[] olhos = { "Castanhos", "Verdes", "Azuis", "Castanhos escuros" };
        String[] cabelos = { "Castanho longo", "Ondulado preto", "Loiro claro", "Crespo preto" };
        String[] cidades = { "São Paulo", "Rio de Janeiro", "Belo Horizonte", "Brasília" };
        String[] nacionalidades = { "Brasileira", "Brasileira", "Brasileira", "Brasileira" };
        String[] tamanhoVestido = { "36", "38", "36", "40" };
        String[] tamanhoCalcado = { "38", "39", "38", "39" };
        String[] fotoRosto = {
                "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=600&auto=format&fit=crop",
                "https://images.unsplash.com/photo-1529139574466-a303027c1d8b?w=600&auto=format&fit=crop",
                "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=600&auto=format&fit=crop",
                "https://images.unsplash.com/photo-1502823403499-6ccfcf4fb453?w=600&auto=format&fit=crop"
        };

        for (int i = 0; i < nomes.length; i++) {
            try {
                Model m = new Model();
                m.setStageName(nomes[i]);
                m.setGender(com.wbscouting.api.enums.GenderType.FEMALE);
                m.setBirthDate(LocalDate.now().minusYears(idades[i]).minusDays((long)(Math.random() * 300)));
                m.setHeightCm(alturasCm[i]);
                m.setBustChestCm(BigDecimal.valueOf(busto[i]));
                m.setWaistCm(BigDecimal.valueOf(cintura[i]));
                m.setHipsCm(BigDecimal.valueOf(quadril[i]));
                m.setEyesColor(olhos[i]);
                m.setHairColor(cabelos[i]);
                m.setCity(cidades[i]);
                m.setNationality(nacionalidades[i]);
                m.setDressSize(tamanhoVestido[i]);
                m.setShoeSize(tamanhoCalcado[i]);
                m.setPrimaryPhotoUrl(fotoRosto[i]);
                m.setInstagramUrl("@" + nomes[i].split(" ")[0].toLowerCase() + ".wb");
                m.setIsActive(true);
                m.setIsFeaturedHome(i == 0);
                m.setIsStar(i == 0);
                m.setFeaturedOrder(i == 0 ? 1 : null);
                modelRepository.save(m);
                log.info("[MODEL INIT][H2][{}] Modelo exemplo criado: {} ({}, {}cm)", (i+1), m.getStageName(), idades[i], alturasCm[i]);
            } catch (Exception ex) {
                log.error("[MODEL INIT][H2] Falhou modelo exemplo {} ({}). Motivo: {}", (i+1), nomes[i], ex.getMessage(), ex);
            }
        }
        log.info("[MODEL INIT][H2] {} modelos exemplo inseridos no catalogo.", nomes.length);
    }

    private void migrateDatabaseSchemaPostgresOnly() {
        try {
            log.info("[DLL MIGRATE][POSTGRES] Verificando integridade das colunas do banco PostgreSQL/Supabase...");

            // Garante coluna is_cover em model_media
            jdbcTemplate.execute("ALTER TABLE public.model_media ADD COLUMN IF NOT EXISTS is_cover BOOLEAN NOT NULL DEFAULT FALSE;");

            // Garante coluna converted_to_model_id em candidate_submissions
            jdbcTemplate.execute("ALTER TABLE public.candidate_submissions ADD COLUMN IF NOT EXISTS converted_to_model_id UUID NULL REFERENCES public.models(id) ON DELETE SET NULL;");
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_candidate_submissions_converted_model ON public.candidate_submissions(converted_to_model_id);");

            // Garante coluna updated_by em site_contents
            jdbcTemplate.execute("ALTER TABLE public.site_contents ADD COLUMN IF NOT EXISTS updated_by UUID NULL;");

            // Garante composite inicial de demonstração para Isabella Fontana
            jdbcTemplate.execute("""
                INSERT INTO public.model_media (id, model_id, media_type, file_url, file_path, display_order, is_cover, is_active, created_at, updated_at)
                SELECT gen_random_uuid(), '487b27d7-206f-422d-a46c-8961ed8c827c', 'COMPOSITE'::media_type, 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?q=80&w=1200&auto=format&fit=crop', 'composites/isabella_fontana_comp.jpg', 1, false, true, NOW(), NOW()
                WHERE EXISTS (SELECT 1 FROM public.models WHERE id = '487b27d7-206f-422d-a46c-8961ed8c827c')
                  AND NOT EXISTS (SELECT 1 FROM public.model_media WHERE model_id = '487b27d7-206f-422d-a46c-8961ed8c827c' AND media_type = 'COMPOSITE'::media_type);
            """);

            log.info("[DLL MIGRATE][POSTGRES] OK.");
        } catch (Exception ex) {
            log.error("[DLL MIGRATE][POSTGRES] Erro ao aplicar migracoes DDL PostgreSQL. (Continuando app porque nao eh critico). Motivo: {}", ex.getMessage(), ex);
        }
    }

    // ============================================================
    // Helpers
    // ============================================================

    private static boolean isLocalOrDevProfile(Environment env) {
        if (env == null) return false;
        String[] profiles = env.getActiveProfiles();
        if (profiles == null || profiles.length == 0) {
            // Sem profile setado = fallback LOCAL (eh default do usuario)
            return true;
        }
        for (String p : profiles) {
            if (p == null) continue;
            String low = p.toLowerCase();
            if (low.contains("prod") || low.contains("render") || low.contains("prd") || low.contains("hostinger") || low.contains("staging") || low.contains("cloud")) {
                return false;
            }
            if (low.contains("local") || low.contains("dev") || low.contains("h2") || low.contains("development")) {
                return true;
            }
        }
        // Default seguro: se nenhum profile bater, NÃO consideramos local (nao queremos rodar nada perigoso em prod por acidente)
        return false;
    }
}

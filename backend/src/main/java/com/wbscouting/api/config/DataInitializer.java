package com.wbscouting.api.config;

import com.wbscouting.api.entity.Admin;
import com.wbscouting.api.entity.Model;
import com.wbscouting.api.enums.AdminRole;
import com.wbscouting.api.repository.AdminRepository;
import com.wbscouting.api.repository.ModelRepository;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.UUID;

@Slf4j
@Component
public class DataInitializer implements CommandLineRunner {

    // ============================================================
    // Logger fallback static (nao depende @Slf4j processar)
    // ============================================================
    private static final Logger LOG = LoggerFactory.getLogger(DataInitializer.class);

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
            LOG.info("[DATA INIT][H2] Perfil LOCAL/DEV detectado. Pulando migracoes DDL nativas PostgreSQL.");
        }

        // -------------- PASSO 2: Garantir usuario ADMIN ativo --------------
        // Se count == 0, cria. Se for LOCAL, garante mesmo que ja exista (upsert).
        long adminCount = 0L;
        try { adminCount = adminRepository.count(); } catch (Exception e) { LOG.warn("[ADMIN INIT] Nao foi possivel contar admins. H2 ainda nao criou schema? Motivo: {}", e.getMessage()); }

        if (adminCount == 0L) {
            LOG.info("[ADMIN INIT] Nenhum admin encontrado. Criando admin inicial {}.", defaultEmail);
            criarAdminInicial();
        } else if (isLocalDevProfile) {
            // TASK: Ambiente LOCAL = sempre garantimos credencial coerente = application-local.yml.
            // Atualiza senha / role / nome caso exista por algum motivo (seed anterior).
            LOG.info("[ADMIN INIT][H2] Ambiente local. Garantindo credenciais admin {} via UPSERT.", defaultEmail);
            upsertAdminInicialLocal();
        } else {
            LOG.info("[ADMIN INIT] Admins ja existentes (count={}). Nenhuma acao.", adminCount);
        }

        // -------------- PASSO 3: Carga 2 a 4 modelos de exemplo (CATALOGO) --------------
        // Apenas se NAO tiver nenhum modelo salvo. Ideal para profile H2 limpo.
        long modelCount = 0L;
        try { modelCount = modelRepository.count(); } catch (Exception e) { LOG.warn("[MODEL INIT] Nao foi possivel contar models. Motivo: {}", e.getMessage()); }

        if (modelCount == 0L) {
            LOG.info("[MODEL INIT] Catálogo vazio. Inserindo 4 modelos de exemplo para H2 Local.");
            inserirModelosExemploH2();
        } else {
            LOG.info("[MODEL INIT] Catalogo com {} registros. Pulando carga de exemplos.", modelCount);
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
        LOG.info("[ADMIN INIT] Admin CRIADO: {} [role={}, senha BCrypt OK, isActive=true] Login frontend = {} / {}",
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
                LOG.info("[ADMIN INIT][H2] Admin ATUALIZADO via UPSERT: {}. Senha atualizada.", defaultEmail);
            } else {
                criarAdminInicial();
            }
        } catch (Exception e) {
            LOG.warn("[ADMIN INIT][H2] Falhou findByEmail UPSERT local. Criando admin novo. Motivo: {}", e.getMessage());
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
        double[] alturasCm = { 175.0, 178.0, 172.0, 180.0 };
        double[] busto = { 86.0, 88.0, 84.0, 90.0 };
        double[] cintura = { 61.0, 62.0, 60.0, 63.0 };
        double[] quadril = { 90.0, 92.0, 88.0, 94.0 };
        String[] etnias = { "Branca", "Parda", "Branca", "Preta" };
        String[] olhos = { "Castanhos", "Verdes", "Azuis", "Castanhos escuros" };
        String[] cabelos = { "Castanho longo", "Ondulado preto", "Loiro claro", "Crespo preto" };
        String[] cidades = { "São Paulo", "Rio de Janeiro", "Belo Horizonte", "Brasília" };
        String[] fotoRosto = {
                "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=600&auto=format&fit=crop",
                "https://images.unsplash.com/photo-1529139574466-a303027c1d8b?w=600&auto=format&fit=crop",
                "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=600&auto=format&fit=crop",
                "https://images.unsplash.com/photo-1502823403499-6ccfcf4fb453?w=600&auto=format&fit=crop"
        };
        String[] fotoCorpo = {
                "https://images.unsplash.com/photo-1496747611176-843222e1e57c?w=900&auto=format&fit=crop",
                "https://images.unsplash.com/photo-1524638431109-93d95c968f03?w=900&auto=format&fit=crop",
                "https://images.unsplash.com/photo-1515372039744-b8f02a3ae446?w=900&auto=format&fit=crop",
                "https://images.unsplash.com/photo-1520975922284-9f505439a830?w=900&auto=format&fit=crop"
        };

        for (int i = 0; i < nomes.length; i++) {
            try {
                Model m = new Model();
                m.setId(UUID.randomUUID());
                m.setName(nomes[i]);
                m.setStageName(nomes[i].split(" ")[0] + " W.");
                m.setBirthDate(LocalDate.now().minusYears(idades[i]).minusDays((long)(Math.random() * 300)));
                m.setHeightCm(BigDecimal.valueOf(alturasCm[i]));
                m.setBustCm(BigDecimal.valueOf(busto[i]));
                m.setWaistCm(BigDecimal.valueOf(cintura[i]));
                m.setHipCm(BigDecimal.valueOf(quadril[i]));
                m.setEthnicity(etnias[i]);
                m.setEyeColor(olhos[i]);
                m.setHairColor(cabelos[i]);
                m.setCity(cidades[i]);
                m.setState("SP");
                m.setCountry("Brasil");
                m.setPhone("+55 11 9" + (10000000 + (int)(Math.random() * 89999999)));
                try { m.setEmail((nomes[i].split(" ")[0].toLowerCase() + "." + nomes[i].split(" ")[1].toLowerCase() + "@wbscouting.com").replaceAll("[^a-z0-9@.\\-]", "")); } catch (Exception ignore) {}
                m.setFacePhotoUrl(fotoRosto[i]);
                m.setProfilePhotoUrl(fotoRosto[i]);
                m.setFullBodyPhotoUrl(fotoCorpo[i]);
                m.setShoeSizeEu(38 + i % 2);
                m.setDressSizeBr(36 + i % 3);
                m.setIsActive(true);
                m.setIsFeatured(i == 0);
                m.setCategories(new HashSet<>(Arrays.asList("EDITORIAL", "COMERCIAL", "RUNWAY")));
                modelRepository.save(m);
                LOG.info("[MODEL INIT][H2][{}] Modelo exemplo criado: {} ({}, {}cm)", (i+1), m.getName(), idades[i], alturasCm[i]);
            } catch (Exception ex) {
                LOG.error("[MODEL INIT][H2] Falhou modelo exemplo {} ({}). Motivo: {}", (i+1), nomes[i], ex.getMessage());
            }
        }
        LOG.info("[MODEL INIT][H2] {} modelos exemplo inseridos no catalogo.", nomes.length);
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
            LOG.error("[DLL MIGRATE][POSTGRES] Erro ao aplicar migracoes DDL PostgreSQL. (Continuando app porque nao eh critico). Motivo: {}", ex.getMessage(), ex);
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

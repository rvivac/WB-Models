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

    @Value("${app.security.admin-webmaster.name:Webmaster WB Agency}")
    private String webmasterName;

    @Value("${app.security.admin-webmaster.email:webmaster@wbagency.com.br}")
    private String webmasterEmail;

    @Value("${app.security.admin-webmaster.password:K+nZVbCpl@3}")
    private String webmasterPassword;

    @Value("${app.security.admin-webmaster.role:WEBMASTER}")
    private String webmasterRoleStr;

    @Value("${app.security.admin-webmaster.must-change-password:false}")
    private boolean webmasterMustChangePassword;

    @Value("${app.security.admin-info.name:Administrativo WB Agency}")
    private String infoName;

    @Value("${app.security.admin-info.email:info@wbagency.com.br}")
    private String infoEmail;

    @Value("${app.security.admin-info.password:U%6>aw8Prw@?PP~}")
    private String infoPassword;

    @Value("${app.security.admin-info.role:ADMIN}")
    private String infoRoleStr;

    @Value("${app.security.admin-info.must-change-password:false}")
    private boolean infoMustChangePassword;

    @Value("${app.security.initial-admin.name:Webmaster WB Agency}")
    private String defaultName;

    @Value("${app.security.initial-admin.email:webmaster@wbagency.com.br}")
    private String defaultEmail;

    @Value("${app.security.initial-admin.password:K+nZVbCpl@3}")
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

        // -------------- PASSO 2: Garantir usuarios ADMIN ativos (upsert idempotente por e-mail) --------------
        // Sempre executa, independente do adminCount. Assim:
        //   - Ambiente LOCAL: credenciais sempre coerentes com application-local.yml
        //   - Ambiente RENDER/SUPABASE (producao): sempre atualiza senhas dos admins padrao,
        //     sem remover admins extras criados manualmente pela interface.
        try {
            AdminRole webmasterRole = parseRole(webmasterRoleStr, AdminRole.WEBMASTER);
            garantirAdminPorEmail(
                    "WEBMASTER",
                    webmasterEmail,
                    webmasterName,
                    webmasterPassword,
                    webmasterRole,
                    webmasterMustChangePassword
            );
        } catch (Exception ex) {
            log.error("[ADMIN INIT][WEBMASTER] Falha ao garantir admin webmaster '{}': {}", webmasterEmail, ex.getMessage(), ex);
        }

        try {
            AdminRole infoRole = parseRole(infoRoleStr, AdminRole.ADMIN);
            garantirAdminPorEmail(
                    "INFO-ADMIN",
                    infoEmail,
                    infoName,
                    infoPassword,
                    infoRole,
                    infoMustChangePassword
            );
        } catch (Exception ex) {
            log.error("[ADMIN INIT][INFO-ADMIN] Falha ao garantir admin info '{}': {}", infoEmail, ex.getMessage(), ex);
        }

        // Compatibilidade LEGACY fallback: seed antigo apenas se banco TOTALMENTE vazio
        long adminCount = 0L;
        try { adminCount = adminRepository.count(); } catch (Exception e) { log.warn("[ADMIN INIT] Nao foi possivel contar admins. H2 ainda nao criou schema? Motivo: {}", e.getMessage()); }

        if (adminCount == 0L) {
            log.warn("[ADMIN INIT][LEGACY] count=0 — nenhum admin persistiu dos upserts. Criando fallback legacy {}.", defaultEmail);
            criarAdminInicial();
        } else if (isLocalDevProfile) {
            log.info("[ADMIN INIT][H2] Admins persistidos no ambiente local (count={}).", adminCount);
        } else {
            log.info("[ADMIN INIT] Admins garantidos via upsert por e-mail (count={}). Nenhum admin manual foi removido.", adminCount);
        }

        // -------------- PASSO 3: Carga 2 a 4 modelos de exemplo (CATALOGO) --------------
        // ⛔ PRODUÇÃO SEGURA: Seed de modelos fictícios SOMENTE em H2 LOCAL/DEV.
        //    Em PostgreSQL/Supabase de produção, mesmo se catálogo estiver VAZIO, NAO insere nada.
        //    (Evita aparecerem modelos demo com fotos Unsplash no site real do cliente)
        long modelCount = 0L;
        try { modelCount = modelRepository.count(); } catch (Exception e) { log.warn("[MODEL INIT] Nao foi possivel contar models. Motivo: {}", e.getMessage()); }

        if (modelCount == 0L && isLocalDevProfile) {
            log.info("[MODEL INIT][H2-LOCAL] Catálogo vazio detectado em ambiente LOCAL/DEV. Inserindo 4 modelos exemplo.");
            inserirModelosExemploH2();
        } else if (modelCount == 0L) {
            log.info("[MODEL INIT][PROD] Catálogo vazio detectado em ambiente PRODUCAO. Seed de exemplos BLOQUEADO (seguranca). Admin deve cadastrar via painel.");
        } else {
            log.info("[MODEL INIT] Catalogo com {} registros. Pulando carga de exemplos.", modelCount);
        }
    }

    /**
     * UPSERT idempotente por email: cria se nao existir, atualiza se existir.
     * Garante que os admins padrao sempre tenham senha/nome/role corretos.
     * NUNCA remove admins criados manualmente (outros emails).
     */
    private void garantirAdminPorEmail(String tag, String email, String nome, String senhaBruta, AdminRole role, boolean mustChangePassword) {
        if (email == null || email.isBlank()) {
            log.warn("[ADMIN INIT][{}] E-mail vazio, ignorando seed.", tag);
            return;
        }
        String emailSanitizado = email.trim().toLowerCase();
        var opt = adminRepository.findByEmail(emailSanitizado);

        if (opt.isPresent()) {
            Admin a = opt.get();
            boolean alterado = false;
            if (nome != null && !nome.equals(a.getName())) { a.setName(nome); alterado = true; }
            if (role != null && role != a.getRole()) { a.setRole(role); alterado = true; }
            if (senhaBruta != null && !senhaBruta.isBlank() && !passwordEncoder.matches(senhaBruta, a.getPasswordHash())) {
                a.setPasswordHash(passwordEncoder.encode(senhaBruta));
                alterado = true;
            }
            if (!Boolean.TRUE.equals(a.getIsActive())) { a.setIsActive(true); alterado = true; }
            if (Boolean.TRUE.equals(a.getIs2faEnabled())) { /* nao altera */ }
            if (mustChangePassword && !Boolean.TRUE.equals(a.getMustChangePassword())) { a.setMustChangePassword(true); alterado = true; }
            if (!mustChangePassword && Boolean.TRUE.equals(a.getMustChangePassword())) { a.setMustChangePassword(false); alterado = true; }

            if (alterado) {
                adminRepository.save(a);
                log.info("[ADMIN INIT][{}] ADMIN ATUALIZADO: {} [role={}, isActive=true, senha={}]. Login frontend = {} / {}",
                        tag, emailSanitizado, role, senhaBruta != null && !senhaBruta.isBlank() ? "SINCRONIZADA" : "MANTIDA",
                        emailSanitizado, senhaBruta != null ? senhaBruta : "<não modificada>");
            } else {
                log.info("[ADMIN INIT][{}] ADMIN já coerente (sem alterações): {} [role={}].", tag, emailSanitizado, role);
            }
        } else {
            Admin a = Admin.builder()
                    .name(nome != null ? nome : "Admin WB")
                    .email(emailSanitizado)
                    .passwordHash(passwordEncoder.encode(senhaBruta != null && !senhaBruta.isBlank() ? senhaBruta : "Admin@Trocasenha123!"))
                    .role(role != null ? role : AdminRole.ADMIN)
                    .isActive(true)
                    .mustChangePassword(mustChangePassword)
                    .is2faEnabled(false)
                    .build();
            adminRepository.save(a);
            log.info("[ADMIN INIT][{}] ADMIN CRIADO: {} [role={}, isActive=true, senha BCrypt OK]. Login frontend = {} / {}",
                    tag, emailSanitizado, role, emailSanitizado, senhaBruta != null ? senhaBruta : "Admin@Trocasenha123!");
        }
    }

    private static AdminRole parseRole(String roleStr, AdminRole fallback) {
        if (roleStr == null || roleStr.isBlank()) return fallback;
        try {
            return AdminRole.valueOf(roleStr.trim().toUpperCase());
        } catch (Exception ex) {
            return fallback;
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

            // Garante conversão de media_type em model_media para varchar(30) se ainda for tipo enum nativo
            jdbcTemplate.execute("""
                DO $$
                BEGIN
                    IF EXISTS (
                        SELECT 1 FROM information_schema.columns
                        WHERE table_schema = 'public'
                          AND table_name = 'model_media'
                          AND column_name = 'media_type'
                          AND udt_name = 'media_type'
                    ) THEN
                        ALTER TABLE public.model_media ALTER COLUMN media_type TYPE varchar(30) USING media_type::varchar;
                    END IF;
                END $$;
            """);

            // Garante coluna converted_to_model_id em candidate_submissions
            jdbcTemplate.execute("ALTER TABLE public.candidate_submissions ADD COLUMN IF NOT EXISTS converted_to_model_id UUID NULL REFERENCES public.models(id) ON DELETE SET NULL;");
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_candidate_submissions_converted_model ON public.candidate_submissions(converted_to_model_id);");

            // Garante coluna updated_by em site_contents
            jdbcTemplate.execute("ALTER TABLE public.site_contents ADD COLUMN IF NOT EXISTS updated_by UUID NULL;");

            // Garante que candidate_photos referencie 'candidates' (e não a tabela legada 'candidate_submissions')
            jdbcTemplate.execute("""
                DO $$
                BEGIN
                    IF EXISTS (
                        SELECT 1 FROM information_schema.table_constraints
                        WHERE table_schema = 'public'
                          AND table_name = 'candidate_photos'
                          AND constraint_name = 'fk_candidate_photos_submission'
                    ) THEN
                        ALTER TABLE public.candidate_photos DROP CONSTRAINT fk_candidate_photos_submission;
                    END IF;

                    IF NOT EXISTS (
                        SELECT 1 FROM information_schema.table_constraints
                        WHERE table_schema = 'public'
                          AND table_name = 'candidate_photos'
                          AND constraint_name = 'fk_candidate_photos_candidate'
                    ) THEN
                        ALTER TABLE public.candidate_photos
                        ADD CONSTRAINT fk_candidate_photos_candidate
                        FOREIGN KEY (candidate_id) REFERENCES public.candidates(id) ON DELETE CASCADE;
                    END IF;
                END $$;
            """);

            // 🔄 MIGRAÇÃO AUTOMÁTICA: Sincroniza registros legados de candidate_submissions para candidates
            jdbcTemplate.execute("""
                DO $$
                BEGIN
                    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'candidate_submissions') THEN
                        -- 1. Insere em candidates qualquer registro de candidate_submissions que ainda não exista
                        INSERT INTO public.candidates (
                            id, protocol, full_name, email, phone, birth_date, age, gender,
                            height_cm, city, state, bust_chest_cm, waist_cm, hips_cm, shoe_size,
                            instagram_handle, guardian_name, legal_guardian_name, legal_guardian_contact,
                            status, lgpd_accepted, lgpd_accepted_at, created_at, updated_at, archived_at
                        )
                        SELECT 
                            cs.id, cs.protocol, cs.full_name, cs.email, cs.phone, cs.birth_date, cs.age, cs.gender::text,
                            CASE 
                                WHEN cs.height IS NULL THEN 175.00
                                WHEN cs.height < 3.0 THEN (cs.height * 100)
                                ELSE cs.height 
                            END AS height_cm,
                            cs.city, cs.state, cs.bust, cs.waist, cs.hips, cs.shoe_size::text,
                            cs.instagram_handle, cs.guardian_name, cs.guardian_name, cs.guardian_phone,
                            cs.status::text, COALESCE(cs.lgpd_consent, true),
                            COALESCE(cs.lgpd_consent_at, cs.created_at, NOW()),
                            COALESCE(cs.created_at, NOW()), COALESCE(cs.updated_at, NOW()), cs.archived_at
                        FROM public.candidate_submissions cs
                        WHERE NOT EXISTS (
                            SELECT 1 FROM public.candidates c WHERE c.id = cs.id OR (cs.protocol IS NOT NULL AND c.protocol = cs.protocol)
                        );

                        -- 2. Migra foto de Rosto (face) para candidate_photos
                        INSERT INTO public.candidate_photos (id, candidate_id, storage_path, file_url, file_path, display_order, photo_position, uploaded_at, created_at)
                        SELECT gen_random_uuid(), cs.id, cs.face_photo_url, cs.face_photo_url, cs.face_photo_url, 1, 1, COALESCE(cs.created_at, NOW()), COALESCE(cs.created_at, NOW())
                        FROM public.candidate_submissions cs
                        WHERE cs.face_photo_url IS NOT NULL AND cs.face_photo_url <> ''
                          AND EXISTS (SELECT 1 FROM public.candidates c WHERE c.id = cs.id)
                          AND NOT EXISTS (SELECT 1 FROM public.candidate_photos cp WHERE cp.candidate_id = cs.id AND cp.display_order = 1);

                        -- 3. Migra foto de Perfil (profile) para candidate_photos
                        INSERT INTO public.candidate_photos (id, candidate_id, storage_path, file_url, file_path, display_order, photo_position, uploaded_at, created_at)
                        SELECT gen_random_uuid(), cs.id, cs.profile_photo_url, cs.profile_photo_url, cs.profile_photo_url, 2, 2, COALESCE(cs.created_at, NOW()), COALESCE(cs.created_at, NOW())
                        FROM public.candidate_submissions cs
                        WHERE cs.profile_photo_url IS NOT NULL AND cs.profile_photo_url <> ''
                          AND EXISTS (SELECT 1 FROM public.candidates c WHERE c.id = cs.id)
                          AND NOT EXISTS (SELECT 1 FROM public.candidate_photos cp WHERE cp.candidate_id = cs.id AND cp.display_order = 2);

                        -- 4. Migra foto de Corpo Inteiro (fullBody) para candidate_photos
                        INSERT INTO public.candidate_photos (id, candidate_id, storage_path, file_url, file_path, display_order, photo_position, uploaded_at, created_at)
                        SELECT gen_random_uuid(), cs.id, cs.full_body_photo_url, cs.full_body_photo_url, cs.full_body_photo_url, 3, 3, COALESCE(cs.created_at, NOW()), COALESCE(cs.created_at, NOW())
                        FROM public.candidate_submissions cs
                        WHERE cs.full_body_photo_url IS NOT NULL AND cs.full_body_photo_url <> ''
                          AND EXISTS (SELECT 1 FROM public.candidates c WHERE c.id = cs.id)
                          AND NOT EXISTS (SELECT 1 FROM public.candidate_photos cp WHERE cp.candidate_id = cs.id AND cp.display_order = 3);
                    END IF;
                END $$;
            """);

            // ⛔ PRODUÇÃO SEGURA: Composite demo Isabella Fontana APENAS se o modelo ID realmente
            //    pertence a um registro seed H2 (nao queremos inserir midia demo em modelos REAIS
            //    que por coincidencia tenham mesmo UUID em outro ambiente).
            //    Verificacao adicional: helper isLocalOrDevProfile(this.env) BLOQUEIA em producao.
            if (isLocalOrDevProfile(this.env)) {
                log.info("[DLL MIGRATE][LOCAL-H2] Inserindo composite demo Isabella Fontana (seed local).");
                jdbcTemplate.execute("""
                    INSERT INTO public.model_media (id, model_id, media_type, file_url, file_path, display_order, is_cover, is_active, created_at, updated_at)
                    SELECT gen_random_uuid(), '487b27d7-206f-422d-a46c-8961ed8c827c', 'COMPOSITE'::media_type, 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?q=80&w=1200&auto=format&fit=crop', 'composites/isabella_fontana_comp.jpg', 1, false, true, NOW(), NOW()
                    WHERE EXISTS (SELECT 1 FROM public.models WHERE id = '487b27d7-206f-422d-a46c-8961ed8c827c')
                      AND NOT EXISTS (SELECT 1 FROM public.model_media WHERE model_id = '487b27d7-206f-422d-a46c-8961ed8c827c' AND media_type = 'COMPOSITE'::media_type);
                """);
            } else {
                log.info("[DLL MIGRATE][PROD] Bloco composite demo Isabella BLOQUEADO em ambiente de producao.");
            }

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

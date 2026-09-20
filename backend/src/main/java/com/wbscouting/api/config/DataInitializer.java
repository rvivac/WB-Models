package com.wbscouting.api.config;

import com.wbscouting.api.entity.Admin;
import com.wbscouting.api.enums.AdminRole;
import com.wbscouting.api.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Value("${app.security.initial-admin.name:Administrador WB Scouting}")
    private String defaultName;

    @Value("${app.security.initial-admin.email:admin@wbscouting.com}")
    private String defaultEmail;

    @Value("${app.security.initial-admin.password:Admin@WbScouting2026!}")
    private String defaultPassword;

    @Override
    public void run(String... args) {
        migrateDatabaseSchema();

        if (adminRepository.count() == 0) {
            log.info("Nenhum administrador encontrado no banco de dados. Criando administrador padrão...");

            Admin initialAdmin = Admin.builder()
                    .name(defaultName)
                    .email(defaultEmail.trim().toLowerCase())
                    .passwordHash(passwordEncoder.encode(defaultPassword))
                    .role(AdminRole.SUPER_ADMIN)
                    .isActive(true)
                    .build();

            adminRepository.save(initialAdmin);
            log.info("Administrador padrão criado com sucesso: {} ({})", defaultName, defaultEmail);
        } else {
            log.debug("Administradores já existentes no banco de dados. Inicialização de admin ignorada.");
        }
    }

    private void migrateDatabaseSchema() {
        try {
            log.info("Verificando integridade das colunas do banco de dados...");
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

            log.info("Migrações DDL complementares executadas com sucesso.");
        } catch (Exception ex) {
            log.error("Erro ao aplicar migrações DDL em DataInitializer: {}", ex.getMessage(), ex);
        }
    }
}

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

    @Value("${app.security.initial-admin.name:Administrador WB Scouting}")
    private String defaultName;

    @Value("${app.security.initial-admin.email:admin@wbscouting.com}")
    private String defaultEmail;

    @Value("${app.security.initial-admin.password:Admin@WbScouting2026!}")
    private String defaultPassword;

    @Override
    public void run(String... args) {
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
}

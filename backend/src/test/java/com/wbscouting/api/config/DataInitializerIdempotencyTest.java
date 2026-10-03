package com.wbscouting.api.config;

import com.wbscouting.api.entity.Admin;
import com.wbscouting.api.enums.AdminRole;
import com.wbscouting.api.repository.AdminRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@DisplayName("3.3. Teste do DataInitializer (Sem Recriação de Admin Padrão)")
class DataInitializerIdempotencyTest {

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JdbcTemplate jdbcTemplate;

    private DataInitializer dataInitializer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        dataInitializer = new DataInitializer(adminRepository, passwordEncoder, jdbcTemplate);

        ReflectionTestUtils.setField(dataInitializer, "defaultName", "Webmaster WB Agency");
        ReflectionTestUtils.setField(dataInitializer, "defaultEmail", "webmaster@wbagency.com.br");
        ReflectionTestUtils.setField(dataInitializer, "defaultPassword", "Admin@WbScouting2026!");
    }

    @Test
    @DisplayName("NÃO deve recriar nem sobrescrever admin quando já existir pelo menos um registro (Idempotência)")
    void shouldNotRecreateAdminWhenAdminAlreadyExists() {
        // Simula que a tabela public.admins já possui 1 administrador cadastrado (como em produção)
        when(adminRepository.count()).thenReturn(1L);

        dataInitializer.run();

        // Verifica que o repositório foi consultado
        verify(adminRepository, times(1)).count();

        // NENHUM salvamento ou redefinição deve ocorrer
        verify(adminRepository, never()).save(any(Admin.class));

        // NENHUMA codificação de senha padrão deve ser disparada
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("NÃO deve recriar admin quando existirem múltiplos administradores cadastrados")
    void shouldNotRecreateAdminWhenMultipleAdminsExist() {
        when(adminRepository.count()).thenReturn(5L);

        dataInitializer.run();

        verify(adminRepository, times(1)).count();
        verify(adminRepository, never()).save(any(Admin.class));
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("DEVE criar o administrador padrão inicial APENAS se a tabela estiver completamente vazia (count == 0)")
    void shouldCreateInitialAdminOnlyWhenTableIsEmpty() {
        when(adminRepository.count()).thenReturn(0L);
        when(passwordEncoder.encode("Admin@WbScouting2026!")).thenReturn("bcrypt-encoded-strong-hash");

        dataInitializer.run();

        verify(adminRepository, times(1)).count();
        verify(passwordEncoder, times(1)).encode("Admin@WbScouting2026!");

        ArgumentCaptor<Admin> adminCaptor = ArgumentCaptor.forClass(Admin.class);
        verify(adminRepository, times(1)).save(adminCaptor.capture());

        Admin createdAdmin = adminCaptor.getValue();
        assertThat(createdAdmin.getName()).isEqualTo("Webmaster WB Agency");
        assertThat(createdAdmin.getEmail()).isEqualTo("webmaster@wbagency.com.br");
        assertThat(createdAdmin.getPasswordHash()).isEqualTo("bcrypt-encoded-strong-hash");
        assertThat(createdAdmin.getRole()).isEqualTo(AdminRole.WEBMASTER);
        assertThat(createdAdmin.getIsActive()).isTrue();
    }

    @Test
    @DisplayName("Deve executar migrações DDL idempotentes sem lançar exceções")
    void shouldExecuteSchemaMigrationsResiliently() {
        when(adminRepository.count()).thenReturn(1L);

        dataInitializer.run();

        // Verifica que as instruções de DDL defensivas foram submetidas ao JdbcTemplate
        verify(jdbcTemplate, atLeast(3)).execute(anyString());
    }
}

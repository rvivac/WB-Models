package com.wbscouting.api.service.admin;

import com.wbscouting.api.dto.admin.AdminUserResponseDto;
import com.wbscouting.api.entity.Admin;
import com.wbscouting.api.enums.AdminRole;
import com.wbscouting.api.repository.AdminRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceImplTest {

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminUserServiceImpl adminUserService;

    private Admin webmaster;
    private Admin secondaryAdmin;

    @BeforeEach
    void setUp() {
        webmaster = Admin.builder()
                .id(UUID.randomUUID())
                .name("Webmaster WB Agency")
                .email("webmaster@wbagency.com.br")
                .role(AdminRole.WEBMASTER)
                .isActive(true)
                .build();

        secondaryAdmin = Admin.builder()
                .id(UUID.randomUUID())
                .name("Admin Secundário")
                .email("info@wbagency.com.br")
                .role(AdminRole.ADMIN)
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("Deve bloquear auto-desativação do Webmaster logado")
    void shouldBlockSelfDeactivation() {
        when(adminRepository.findById(webmaster.getId())).thenReturn(Optional.of(webmaster));

        assertThatThrownBy(() -> adminUserService.toggleUserStatus(webmaster.getId(), "webmaster@wbagency.com.br"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Não é permitido desativar sua própria conta");

        verify(adminRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve bloquear desativação quando for o único Webmaster/SuperAdmin ativo")
    void shouldBlockDeactivationWhenOnlyOneSuperAdminRemains() {
        Admin otherWebmaster = Admin.builder()
                .id(UUID.randomUUID())
                .name("Outro Webmaster")
                .email("outro@wbagency.com.br")
                .role(AdminRole.WEBMASTER)
                .isActive(true)
                .build();

        when(adminRepository.findById(otherWebmaster.getId())).thenReturn(Optional.of(otherWebmaster));
        when(adminRepository.countByRoleInAndIsActiveTrue(anyList())).thenReturn(1L);

        assertThatThrownBy(() -> adminUserService.toggleUserStatus(otherWebmaster.getId(), "webmaster@wbagency.com.br"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("único Webmaster/SuperAdmin ativo");

        verify(adminRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve bloquear rebaixamento do próprio papel de Webmaster")
    void shouldBlockSelfDemotion() {
        when(adminRepository.findById(webmaster.getId())).thenReturn(Optional.of(webmaster));

        assertThatThrownBy(() -> adminUserService.updateRole(webmaster.getId(), AdminRole.ADMIN, "webmaster@wbagency.com.br"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Não é permitido rebaixar seu próprio papel");

        verify(adminRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve bloquear rebaixamento caso reste apenas um Webmaster/SuperAdmin ativo")
    void shouldBlockDemotionWhenOnlyOneSuperAdminRemains() {
        Admin otherWebmaster = Admin.builder()
                .id(UUID.randomUUID())
                .name("Outro Webmaster")
                .email("outro@wbagency.com.br")
                .role(AdminRole.WEBMASTER)
                .isActive(true)
                .build();

        when(adminRepository.findById(otherWebmaster.getId())).thenReturn(Optional.of(otherWebmaster));
        when(adminRepository.countByRoleInAndIsActiveTrue(anyList())).thenReturn(1L);

        assertThatThrownBy(() -> adminUserService.updateRole(otherWebmaster.getId(), AdminRole.SCOUT, "webmaster@wbagency.com.br"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("único Webmaster/SuperAdmin ativo");

        verify(adminRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve bloquear auto-exclusão do Webmaster logado")
    void shouldBlockSelfDeletion() {
        when(adminRepository.findById(webmaster.getId())).thenReturn(Optional.of(webmaster));

        assertThatThrownBy(() -> adminUserService.deleteSecondaryAdmin(webmaster.getId(), "webmaster@wbagency.com.br"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Não é permitido excluir sua própria conta");

        verify(adminRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Deve bloquear exclusão caso reste apenas um Webmaster/SuperAdmin ativo")
    void shouldBlockDeletionWhenOnlyOneSuperAdminRemains() {
        Admin otherWebmaster = Admin.builder()
                .id(UUID.randomUUID())
                .name("Outro Webmaster")
                .email("outro@wbagency.com.br")
                .role(AdminRole.WEBMASTER)
                .isActive(true)
                .build();

        when(adminRepository.findById(otherWebmaster.getId())).thenReturn(Optional.of(otherWebmaster));
        when(adminRepository.countByRoleInAndIsActiveTrue(anyList())).thenReturn(1L);

        assertThatThrownBy(() -> adminUserService.deleteSecondaryAdmin(otherWebmaster.getId(), "webmaster@wbagency.com.br"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("único Webmaster/SuperAdmin ativo");

        verify(adminRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Deve permitir exclusão de administrador secundário com sucesso")
    void shouldAllowDeletionOfSecondaryAdmin() {
        when(adminRepository.findById(secondaryAdmin.getId())).thenReturn(Optional.of(secondaryAdmin));

        adminUserService.deleteSecondaryAdmin(secondaryAdmin.getId(), "webmaster@wbagency.com.br");

        verify(adminRepository, times(1)).delete(secondaryAdmin);
    }
}

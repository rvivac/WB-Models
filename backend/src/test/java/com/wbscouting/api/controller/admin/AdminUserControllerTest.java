package com.wbscouting.api.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wbscouting.api.dto.admin.AdminUserResponseDto;
import com.wbscouting.api.dto.admin.CreateAdminUserRequestDto;
import com.wbscouting.api.dto.admin.CreateAdminUserResponseDto;
import com.wbscouting.api.dto.admin.UpdateAdminRoleRequestDto;
import com.wbscouting.api.enums.AdminRole;
import com.wbscouting.api.security.JwtAuthenticationEntryPoint;
import com.wbscouting.api.security.JwtService;
import com.wbscouting.api.security.TokenBlacklistService;
import com.wbscouting.api.security.ratelimit.RateLimitingFilter;
import com.wbscouting.api.service.admin.AdminUserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.wbscouting.api.exception.GlobalExceptionHandler;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

import org.springframework.boot.test.context.TestConfiguration;

@WebMvcTest(AdminUserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, AdminUserControllerTest.TestSecurityConfig.class})
class AdminUserControllerTest {

    @TestConfiguration
    @EnableMethodSecurity(prePostEnabled = true)
    static class TestSecurityConfig {}

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AdminUserService adminUserService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockitoBean
    private TokenBlacklistService tokenBlacklistService;

    @MockitoBean
    private RateLimitingFilter rateLimitingFilter;

    @Test
    @WithMockUser(username = "webmaster@wbagency.com.br", roles = {"WEBMASTER"})
    @DisplayName("Webmaster deve listar usuários com sucesso")
    void webmasterShouldListUsers() throws Exception {
        UUID id = UUID.randomUUID();
        AdminUserResponseDto dto = AdminUserResponseDto.builder()
                .id(id)
                .name("Operador Bookings")
                .email("booker@wbagency.com.br")
                .role(AdminRole.ADMIN)
                .isActive(true)
                .is2faEnabled(false)
                .createdAt(OffsetDateTime.now())
                .build();

        when(adminUserService.listUsers(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(dto)));

        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").value("booker@wbagency.com.br"))
                .andExpect(jsonPath("$.content[0].role").value("ADMIN"));
    }

    @Test
    @WithMockUser(username = "scout@wbagency.com.br", roles = {"SCOUT"})
    @DisplayName("Usuário SCOUT deve receber 403 Forbidden ao tentar listar usuários")
    void scoutShouldBeForbiddenToAccessUsers() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@wbagency.com.br", roles = {"ADMIN"})
    @DisplayName("Usuário ADMIN comum deve receber 403 Forbidden ao tentar listar usuários")
    void adminShouldBeForbiddenToAccessUsers() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "webmaster@wbagency.com.br", roles = {"WEBMASTER"})
    @DisplayName("Webmaster deve criar novo administrador com sucesso")
    void webmasterShouldCreateUser() throws Exception {
        CreateAdminUserRequestDto request = CreateAdminUserRequestDto.builder()
                .name("Novo Scout")
                .email("scout.novo@wbagency.com.br")
                .role(AdminRole.SCOUT)
                .build();

        AdminUserResponseDto userDto = AdminUserResponseDto.builder()
                .id(UUID.randomUUID())
                .name("Novo Scout")
                .email("scout.novo@wbagency.com.br")
                .role(AdminRole.SCOUT)
                .isActive(true)
                .mustChangePassword(true)
                .build();

        CreateAdminUserResponseDto response = CreateAdminUserResponseDto.builder()
                .user(userDto)
                .temporaryPassword("Wb@Temp1234#")
                .build();

        when(adminUserService.createUser(any(CreateAdminUserRequestDto.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/admin/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.temporaryPassword").value("Wb@Temp1234#"))
                .andExpect(jsonPath("$.user.email").value("scout.novo@wbagency.com.br"));
    }

    @Test
    @WithMockUser(username = "webmaster@wbagency.com.br", roles = {"WEBMASTER"})
    @DisplayName("Webmaster deve alternar status de administrador com sucesso")
    void webmasterShouldToggleStatus() throws Exception {
        UUID id = UUID.randomUUID();
        AdminUserResponseDto userDto = AdminUserResponseDto.builder()
                .id(id)
                .name("Admin Alvo")
                .email("alvo@wbagency.com.br")
                .role(AdminRole.ADMIN)
                .isActive(false)
                .build();

        when(adminUserService.toggleUserStatus(eq(id), any()))
                .thenReturn(userDto);

        mockMvc.perform(patch("/api/v1/admin/users/" + id + "/toggle-status")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false));
    }

    @Test
    @WithMockUser(username = "webmaster@wbagency.com.br", roles = {"WEBMASTER"})
    @DisplayName("Webmaster deve atualizar papel de administrador com sucesso")
    void webmasterShouldUpdateRole() throws Exception {
        UUID id = UUID.randomUUID();
        UpdateAdminRoleRequestDto request = UpdateAdminRoleRequestDto.builder()
                .role(AdminRole.SCOUT)
                .build();

        AdminUserResponseDto userDto = AdminUserResponseDto.builder()
                .id(id)
                .name("Admin Alvo")
                .email("alvo@wbagency.com.br")
                .role(AdminRole.SCOUT)
                .isActive(true)
                .build();

        when(adminUserService.updateRole(eq(id), eq(AdminRole.SCOUT), any()))
                .thenReturn(userDto);

        mockMvc.perform(patch("/api/v1/admin/users/" + id + "/role")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("SCOUT"));
    }

    @Test
    @WithMockUser(username = "webmaster@wbagency.com.br", roles = {"WEBMASTER"})
    @DisplayName("Webmaster deve excluir administrador secundário com sucesso")
    void webmasterShouldDeleteSecondaryAdmin() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/admin/users/" + id)
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "admin@wbagency.com.br", roles = {"ADMIN"})
    @DisplayName("Usuário ADMIN comum deve receber 403 Forbidden ao tentar excluir administrador")
    void adminShouldBeForbiddenToDeleteAdmin() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/admin/users/" + id)
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }
}

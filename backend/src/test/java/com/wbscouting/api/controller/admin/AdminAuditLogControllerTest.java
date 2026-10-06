package com.wbscouting.api.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wbscouting.api.dto.admin.AdminAuditLogResponseDto;
import com.wbscouting.api.exception.GlobalExceptionHandler;
import com.wbscouting.api.security.JwtAuthenticationEntryPoint;
import com.wbscouting.api.security.JwtService;
import com.wbscouting.api.security.TokenBlacklistService;
import com.wbscouting.api.security.ratelimit.RateLimitingFilter;
import com.wbscouting.api.service.audit.AuditLogService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminAuditLogController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, AdminAuditLogControllerTest.TestSecurityConfig.class})
class AdminAuditLogControllerTest {

    @TestConfiguration
    @EnableMethodSecurity(prePostEnabled = true)
    static class TestSecurityConfig {}

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuditLogService auditLogService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private TokenBlacklistService tokenBlacklistService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockitoBean
    private RateLimitingFilter rateLimitingFilter;

    @Test
    @WithMockUser(username = "webmaster@wbagency.com.br", roles = {"WEBMASTER"})
    @DisplayName("Webmaster deve conseguir listar trilha de auditoria com paginação e filtros")
    void shouldAllowWebmasterToListAuditLogs() throws Exception {
        UUID logId = UUID.randomUUID();
        AdminAuditLogResponseDto dto = AdminAuditLogResponseDto.builder()
                .id(logId)
                .adminEmail("webmaster@wbagency.com.br")
                .action("UPDATE")
                .resourceType("MODEL")
                .resourceId("MOD-101")
                .description("Atualização cadastral do modelo")
                .detailsJson(Map.of("field", "status"))
                .ipAddress("189.10.20.30")
                .userAgent("Mozilla/5.0")
                .createdAt(OffsetDateTime.now())
                .build();

        when(auditLogService.getAuditLogs(any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(dto)));

        mockMvc.perform(get("/api/v1/admin/audit-logs")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].action").value("UPDATE"))
                .andExpect(jsonPath("$.content[0].resourceType").value("MODEL"))
                .andExpect(jsonPath("$.content[0].adminEmail").value("webmaster@wbagency.com.br"));
    }

    @Test
    @WithMockUser(username = "admin@wbagency.com.br", roles = {"ADMIN"})
    @DisplayName("Admin comum deve ser bloqueado com 403 Forbidden ao tentar listar trilha de auditoria")
    void shouldDenyOrdinaryAdminFromListingAuditLogs() throws Exception {
        mockMvc.perform(get("/api/v1/admin/audit-logs")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "scout@wbagency.com.br", roles = {"SCOUT"})
    @DisplayName("Scout deve ser bloqueado com 403 Forbidden ao tentar listar trilha de auditoria")
    void shouldDenyScoutFromListingAuditLogs() throws Exception {
        mockMvc.perform(get("/api/v1/admin/audit-logs")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "webmaster@wbagency.com.br", roles = {"WEBMASTER"})
    @DisplayName("Webmaster deve conseguir buscar log de auditoria específico por ID")
    void shouldAllowWebmasterToGetAuditLogById() throws Exception {
        UUID logId = UUID.randomUUID();
        AdminAuditLogResponseDto dto = AdminAuditLogResponseDto.builder()
                .id(logId)
                .adminEmail("webmaster@wbagency.com.br")
                .action("LOGIN")
                .resourceType("AUTH")
                .description("Autenticação com sucesso")
                .ipAddress("127.0.0.1")
                .createdAt(OffsetDateTime.now())
                .build();

        when(auditLogService.getAuditLogById(eq(logId))).thenReturn(dto);

        mockMvc.perform(get("/api/v1/admin/audit-logs/" + logId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(logId.toString()))
                .andExpect(jsonPath("$.action").value("LOGIN"));
    }
}

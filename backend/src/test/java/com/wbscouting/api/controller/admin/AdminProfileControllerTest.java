package com.wbscouting.api.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wbscouting.api.dto.admin.ChangePasswordRequestDto;
import com.wbscouting.api.dto.auth.TwoFactorConfirmRequestDto;
import com.wbscouting.api.dto.auth.TwoFactorConfirmResponseDto;
import com.wbscouting.api.dto.auth.TwoFactorDisableRequestDto;
import com.wbscouting.api.dto.auth.TwoFactorSetupResponseDto;
import com.wbscouting.api.security.JwtAuthenticationEntryPoint;
import com.wbscouting.api.security.JwtService;
import com.wbscouting.api.security.TokenBlacklistService;
import com.wbscouting.api.security.ratelimit.RateLimitingFilter;
import com.wbscouting.api.service.admin.AdminProfileService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.wbscouting.api.exception.GlobalExceptionHandler;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;

@WebMvcTest(AdminProfileController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AdminProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdminProfileService adminProfileService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockBean
    private TokenBlacklistService tokenBlacklistService;

    @MockBean
    private RateLimitingFilter rateLimitingFilter;

    @Test
    @WithMockUser(username = "admin@wbagency.com.br")
    @DisplayName("Deve alterar senha autenticada com sucesso")
    void shouldChangePasswordSuccessfully() throws Exception {
        ChangePasswordRequestDto request = ChangePasswordRequestDto.builder()
                .currentPassword("SenhaAtual@123")
                .newPassword("NovaSenhaForte@456")
                .confirmPassword("NovaSenhaForte@456")
                .build();

        doNothing().when(adminProfileService).changePassword(eq("admin@wbagency.com.br"), any());

        mockMvc.perform(put("/api/v1/admin/profile/password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Senha alterada com sucesso."));
    }

    @Test
    @WithMockUser(username = "admin@wbagency.com.br")
    @DisplayName("Deve gerar credenciais de setup 2FA com QR Code")
    void shouldSetup2faSuccessfully() throws Exception {
        TwoFactorSetupResponseDto response = TwoFactorSetupResponseDto.builder()
                .secret("JBSWY3DPEHPK3PXP")
                .otpauthUrl("otpauth://totp/WB%20Agency:admin@wbagency.com.br?secret=JBSWY3DPEHPK3PXP")
                .qrCodeDataUrl("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA")
                .build();

        when(adminProfileService.setup2fa("admin@wbagency.com.br")).thenReturn(response);

        mockMvc.perform(post("/api/v1/admin/profile/2fa/setup")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.secret").value("JBSWY3DPEHPK3PXP"))
                .andExpect(jsonPath("$.qrCodeDataUrl").value("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA"));
    }

    @Test
    @WithMockUser(username = "admin@wbagency.com.br")
    @DisplayName("Deve confirmar 2FA e receber códigos de backup")
    void shouldConfirm2faSuccessfully() throws Exception {
        TwoFactorConfirmRequestDto request = TwoFactorConfirmRequestDto.builder()
                .code("123456")
                .build();

        TwoFactorConfirmResponseDto response = TwoFactorConfirmResponseDto.builder()
                .enabled(true)
                .backupCodes(List.of("ABCD-1234", "EFGH-5678"))
                .build();

        when(adminProfileService.confirm2fa(eq("admin@wbagency.com.br"), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/admin/profile/2fa/confirm")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.backupCodes[0]").value("ABCD-1234"));
    }

    @Test
    @WithMockUser(username = "admin@wbagency.com.br")
    @DisplayName("Deve consultar status do 2FA")
    void shouldGet2faStatus() throws Exception {
        when(adminProfileService.get2faStatus("admin@wbagency.com.br"))
                .thenReturn(Map.of("is2faEnabled", true));

        mockMvc.perform(get("/api/v1/admin/profile/2fa/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is2faEnabled").value(true));
    }
}

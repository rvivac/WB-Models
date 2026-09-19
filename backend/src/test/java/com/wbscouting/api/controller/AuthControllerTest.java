package com.wbscouting.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wbscouting.api.dto.AuthDTO;
import com.wbscouting.api.security.JwtAuthenticationFilter;
import com.wbscouting.api.security.JwtTokenProvider;
import com.wbscouting.api.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @DisplayName("Deve processar requisição de esqueci minha senha com sucesso (HTTP 200)")
    void shouldProcessForgotPasswordSuccessfully() throws Exception {
        AuthDTO.ForgotPasswordRequest request = new AuthDTO.ForgotPasswordRequest("admin@wbscouting.com");
        when(authService.forgotPassword(any(AuthDTO.ForgotPasswordRequest.class)))
                .thenReturn(new AuthDTO.MessageResponse("Instruções enviadas com sucesso"));

        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Instruções enviadas com sucesso"));
    }

    @Test
    @DisplayName("Deve processar requisição de redefinição de senha com sucesso (HTTP 200)")
    void shouldProcessResetPasswordSuccessfully() throws Exception {
        AuthDTO.ResetPasswordRequest request = new AuthDTO.ResetPasswordRequest("token-123", "NovaSenha@2026");
        when(authService.resetPassword(any(AuthDTO.ResetPasswordRequest.class)))
                .thenReturn(new AuthDTO.MessageResponse("Senha redefinida com sucesso"));

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Senha redefinida com sucesso"));
    }

    @Test
    @DisplayName("Deve rejeitar requisição com dados inválidos (HTTP 400)")
    void shouldRejectInvalidRequest() throws Exception {
        AuthDTO.ForgotPasswordRequest request = new AuthDTO.ForgotPasswordRequest("email-invalido");

        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}

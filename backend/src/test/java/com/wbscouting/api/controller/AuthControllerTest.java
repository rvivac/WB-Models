package com.wbscouting.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wbscouting.api.dto.AuthDTO;
import com.wbscouting.api.dto.auth.ForgotPasswordRequestDto;
import com.wbscouting.api.dto.auth.LoginRequestDto;
import com.wbscouting.api.dto.auth.LoginResponseDto;
import com.wbscouting.api.dto.auth.ResetPasswordRequestDto;
import com.wbscouting.api.security.JwtAuthenticationEntryPoint;
import com.wbscouting.api.security.JwtAuthenticationFilter;
import com.wbscouting.api.security.JwtService;
import com.wbscouting.api.security.JwtTokenProvider;
import com.wbscouting.api.service.auth.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
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
    private JwtService jwtService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Test
    @DisplayName("Deve autenticar administrador com sucesso (HTTP 200)")
    void shouldAuthenticateAdminSuccessfully() throws Exception {
        LoginRequestDto request = new LoginRequestDto("admin@wbscouting.com", "Admin@123");
        LoginResponseDto response = LoginResponseDto.builder()
                .accessToken("mocked-jwt-token")
                .tokenType("Bearer")
                .expiresIn(28800L)
                .adminName("Administrador WB Scouting")
                .adminEmail("admin@wbscouting.com")
                .build();

        when(authService.login(any(LoginRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("mocked-jwt-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(28800))
                .andExpect(jsonPath("$.adminName").value("Administrador WB Scouting"))
                .andExpect(jsonPath("$.adminEmail").value("admin@wbscouting.com"));
    }

    @Test
    @DisplayName("Deve retornar HTTP 401 quando credenciais forem inválidas")
    void shouldReturn401WhenBadCredentials() throws Exception {
        LoginRequestDto request = new LoginRequestDto("admin@wbscouting.com", "senha-errada");

        when(authService.login(any(LoginRequestDto.class)))
                .thenThrow(new BadCredentialsException("Email ou senha inválidos."));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Credenciais Inválidas"));
    }

    @Test
    @DisplayName("Deve processar requisição de esqueci minha senha com sucesso (HTTP 200)")
    void shouldProcessForgotPasswordSuccessfully() throws Exception {
        ForgotPasswordRequestDto request = new ForgotPasswordRequestDto("admin@wbscouting.com");

        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Wenn die E-Mail im System registriert ist, wurde ein Wiederherstellungslink gesendet."));
    }

    @Test
    @DisplayName("Deve processar requisição de redefinição de senha com sucesso (HTTP 200)")
    void shouldProcessResetPasswordSuccessfully() throws Exception {
        ResetPasswordRequestDto request = new ResetPasswordRequestDto("token-123", "NovaSenha@2026");

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Passwort erfolgreich zurückgesetzt."));
    }

    @Test
    @DisplayName("Deve rejeitar requisição de esqueci minha senha com email inválido (HTTP 400)")
    void shouldRejectInvalidForgotPasswordRequest() throws Exception {
        ForgotPasswordRequestDto request = new ForgotPasswordRequestDto("email-invalido");

        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Deve rejeitar redefinição com senha fraca (HTTP 400)")
    void shouldRejectWeakPasswordResetRequest() throws Exception {
        ResetPasswordRequestDto request = new ResetPasswordRequestDto("token-123", "curta");

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}



package com.wbscouting.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wbscouting.api.controller.AuthController;
import com.wbscouting.api.dto.auth.LoginRequestDto;
import com.wbscouting.api.entity.Admin;
import com.wbscouting.api.enums.AdminRole;
import com.wbscouting.api.exception.GlobalExceptionHandler;
import com.wbscouting.api.repository.AdminRepository;
import com.wbscouting.api.service.auth.AuthServiceImpl;
import com.wbscouting.api.service.email.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("3.1. Teste de Rejeição do Login com a Senha Padrão Antiga")
class AdminInitialPasswordRejectionTest {

    private static final String ADMIN_EMAIL = "admin@wbscouting.com";
    private static final String OLD_DEFAULT_PASSWORD = "Admin@123";
    private static final String CURRENT_SECURE_PASSWORD = "Admin@WbScouting2026!";

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private EmailService emailService;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    private AuthServiceImpl authService;
    private MockMvc mockMvc;
    private Admin initialAdmin;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // O administrador no banco de dados está cadastrado com o hash da nova senha forte
        String currentPasswordHash = passwordEncoder.encode(CURRENT_SECURE_PASSWORD);

        initialAdmin = Admin.builder()
                .id(UUID.randomUUID())
                .name("Administrador WB Scouting")
                .email(ADMIN_EMAIL)
                .passwordHash(currentPasswordHash)
                .role(AdminRole.SUPER_ADMIN)
                .isActive(true)
                .build();

        when(adminRepository.findByEmailAndIsActiveTrue(ADMIN_EMAIL))
                .thenReturn(Optional.of(initialAdmin));

        authService = new AuthServiceImpl(
                adminRepository,
                passwordEncoder,
                jwtService,
                emailService,
                tokenBlacklistService
        );

        AuthController authController = new AuthController(authService);
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("Deve rejeitar o login no AuthService com BadCredentialsException quando utilizada a senha padrão antiga")
    void shouldRejectLoginWithOldDefaultPasswordInAuthService() {
        LoginRequestDto request = new LoginRequestDto(ADMIN_EMAIL, OLD_DEFAULT_PASSWORD);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Credenciais inválidas");

        // Nenhum token JWT pode ser emitido
        verify(jwtService, never()).generateToken(any(Admin.class));
    }

    @Test
    @DisplayName("Deve rejeitar senhas fracas comuns antigas (admin, 123456, Admin@2024)")
    void shouldRejectCommonLegacyWeakPasswords() {
        String[] legacyPasswords = {"admin", "123456", "Admin@2024", "password", "wbscouting123"};

        for (String legacyPassword : legacyPasswords) {
            LoginRequestDto request = new LoginRequestDto(ADMIN_EMAIL, legacyPassword);

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessageContaining("Credenciais inválidas");
        }

        verify(jwtService, never()).generateToken(any(Admin.class));
    }

    @Test
    @DisplayName("Endpoint HTTP POST /api/v1/auth/login deve retornar HTTP 401 Unauthorized para a senha antiga")
    void shouldReturnHttp401UnauthorizedForOldDefaultPassword() throws Exception {
        LoginRequestDto request = new LoginRequestDto(ADMIN_EMAIL, OLD_DEFAULT_PASSWORD);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Credenciais Inválidas"))
                .andExpect(jsonPath("$.detail").value("Credenciais inválidas. Verifique seu e-mail e senha."))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.accessToken").doesNotExist());
    }

    @Test
    @DisplayName("Deve autenticar com sucesso (HTTP 200) e emitir JWT apenas com a nova senha forte configurada")
    void shouldAuthenticateSuccessfullyWithCurrentSecurePassword() throws Exception {
        when(jwtService.generateToken(initialAdmin)).thenReturn("mocked.jwt.token");
        when(jwtService.getExpirationInSeconds()).thenReturn(28800L);

        LoginRequestDto request = new LoginRequestDto(ADMIN_EMAIL, CURRENT_SECURE_PASSWORD);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("mocked.jwt.token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.adminEmail").value(ADMIN_EMAIL))
                .andExpect(jsonPath("$.adminName").value("Administrador WB Scouting"));

        verify(jwtService, times(1)).generateToken(initialAdmin);
    }
}

package com.wbscouting.api.service;

import com.wbscouting.api.dto.AuthDTO;
import com.wbscouting.api.entity.Admin;
import com.wbscouting.api.enums.AdminRole;
import com.wbscouting.api.repository.AdminRepository;
import com.wbscouting.api.security.JwtTokenProvider;
import com.wbscouting.api.service.email.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    private Admin admin;

    @BeforeEach
    void setUp() {
        admin = Admin.builder()
                .id(UUID.randomUUID())
                .name("Diretor de Scouting")
                .email("admin@wbscouting.com")
                .passwordHash("hashed-current-pwd")
                .role(AdminRole.SUPER_ADMIN)
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("Deve gerar token temporal e enviar e-mail ao solicitar recuperação de senha para admin ativo")
    void shouldGenerateTokenAndSendEmailWhenAdminExists() {
        when(adminRepository.findByEmail("admin@wbscouting.com")).thenReturn(Optional.of(admin));

        AuthDTO.ForgotPasswordRequest request = new AuthDTO.ForgotPasswordRequest("admin@wbscouting.com");
        AuthDTO.MessageResponse response = authService.forgotPassword(request);

        assertThat(response.getMessage()).contains("Se o e-mail informado estiver cadastrado");
        assertThat(admin.getPasswordResetToken()).isNotNull();
        assertThat(admin.getPasswordResetExpiresAt()).isAfter(OffsetDateTime.now());

        verify(adminRepository, times(1)).save(admin);
        verify(emailService, times(1)).sendPasswordResetEmail(eq(admin.getEmail()), eq(admin.getName()), any(String.class));
    }

    @Test
    @DisplayName("Deve responder neutro e não falhar quando e-mail não existir na base (anti-enumeração)")
    void shouldReturnNeutralMessageWhenAdminNotFound() {
        when(adminRepository.findByEmail("inexistente@wbscouting.com")).thenReturn(Optional.empty());

        AuthDTO.ForgotPasswordRequest request = new AuthDTO.ForgotPasswordRequest("inexistente@wbscouting.com");
        AuthDTO.MessageResponse response = authService.forgotPassword(request);

        assertThat(response.getMessage()).contains("Se o e-mail informado estiver cadastrado");
        verify(adminRepository, never()).save(any(Admin.class));
        verify(emailService, never()).sendPasswordResetEmail(any(), any(), any());
    }

    @Test
    @DisplayName("Deve redefinir senha com sucesso quando token for válido e não expirado")
    void shouldResetPasswordSuccessfullyWithValidToken() {
        String token = "valid-token-123";
        admin.setPasswordResetToken(token);
        admin.setPasswordResetExpiresAt(OffsetDateTime.now().plusMinutes(30));

        when(adminRepository.findByPasswordResetToken(token)).thenReturn(Optional.of(admin));
        when(passwordEncoder.encode("NovaSenha@2026")).thenReturn("new-bcrypt-hash");

        AuthDTO.ResetPasswordRequest request = new AuthDTO.ResetPasswordRequest(token, "NovaSenha@2026");
        AuthDTO.MessageResponse response = authService.resetPassword(request);

        assertThat(response.getMessage()).contains("Senha redefinida com sucesso");
        assertThat(admin.getPasswordHash()).isEqualTo("new-bcrypt-hash");
        assertThat(admin.getPasswordResetToken()).isNull();
        assertThat(admin.getPasswordResetExpiresAt()).isNull();

        verify(adminRepository, times(1)).save(admin);
    }

    @Test
    @DisplayName("Deve lançar exceção quando o token de recuperação estiver expirado")
    void shouldThrowExceptionWhenTokenIsExpired() {
        String token = "expired-token-123";
        admin.setPasswordResetToken(token);
        admin.setPasswordResetExpiresAt(OffsetDateTime.now().minusMinutes(5));

        when(adminRepository.findByPasswordResetToken(token)).thenReturn(Optional.of(admin));

        AuthDTO.ResetPasswordRequest request = new AuthDTO.ResetPasswordRequest(token, "NovaSenha@2026");

        assertThatThrownBy(() -> authService.resetPassword(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expirou");

        verify(adminRepository, never()).save(admin);
    }

    @Test
    @DisplayName("Deve lançar exceção quando o token não for encontrado")
    void shouldThrowExceptionWhenTokenNotFound() {
        when(adminRepository.findByPasswordResetToken("token-inexistente")).thenReturn(Optional.empty());

        AuthDTO.ResetPasswordRequest request = new AuthDTO.ResetPasswordRequest("token-inexistente", "NovaSenha@2026");

        assertThatThrownBy(() -> authService.resetPassword(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("inválido ou inexistente");
    }
}

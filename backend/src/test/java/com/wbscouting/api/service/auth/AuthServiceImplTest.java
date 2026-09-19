package com.wbscouting.api.service.auth;

import com.wbscouting.api.dto.auth.ForgotPasswordRequestDto;
import com.wbscouting.api.dto.auth.LoginRequestDto;
import com.wbscouting.api.dto.auth.LoginResponseDto;
import com.wbscouting.api.dto.auth.ResetPasswordRequestDto;
import com.wbscouting.api.entity.Admin;
import com.wbscouting.api.enums.AdminRole;
import com.wbscouting.api.repository.AdminRepository;
import com.wbscouting.api.security.JwtService;
import com.wbscouting.api.service.email.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthServiceImpl authService;

    private Admin admin;

    @BeforeEach
    void setUp() {
        admin = Admin.builder()
                .id(UUID.randomUUID())
                .name("Super Admin")
                .email("admin@wbscouting.com")
                .passwordHash("bcrypt-hash-encoded")
                .role(AdminRole.SUPER_ADMIN)
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("Deve autenticar com sucesso quando credenciais forem válidas e administrador ativo")
    void shouldLoginSuccessfully() {
        LoginRequestDto request = new LoginRequestDto("admin@wbscouting.com", "Admin@123");

        when(adminRepository.findByEmailAndIsActiveTrue("admin@wbscouting.com")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("Admin@123", "bcrypt-hash-encoded")).thenReturn(true);
        when(jwtService.generateToken(admin)).thenReturn("valid.jwt.token");
        when(jwtService.getExpirationInSeconds()).thenReturn(28800L);

        LoginResponseDto response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("valid.jwt.token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresIn()).isEqualTo(28800L);
        assertThat(response.getAdminName()).isEqualTo("Super Admin");
        assertThat(response.getAdminEmail()).isEqualTo("admin@wbscouting.com");

        verify(jwtService, times(1)).generateToken(admin);
    }

    @Test
    @DisplayName("Deve lançar BadCredentialsException quando administrador não for encontrado ou estiver inativo")
    void shouldThrowBadCredentialsWhenAdminNotFoundOrInactive() {
        LoginRequestDto request = new LoginRequestDto("inativo@wbscouting.com", "Admin@123");

        when(adminRepository.findByEmailAndIsActiveTrue("inativo@wbscouting.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Email ou senha inválidos.");

        verify(jwtService, never()).generateToken(any());
    }

    @Test
    @DisplayName("Deve lançar BadCredentialsException quando a senha estiver incorreta")
    void shouldThrowBadCredentialsWhenPasswordDoesNotMatch() {
        LoginRequestDto request = new LoginRequestDto("admin@wbscouting.com", "senha-errada");

        when(adminRepository.findByEmailAndIsActiveTrue("admin@wbscouting.com")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("senha-errada", "bcrypt-hash-encoded")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Email ou senha inválidos.");

        verify(jwtService, never()).generateToken(any());
    }

    @Test
    @DisplayName("Deve processar recuperação de senha com sucesso para admin ativo")
    void shouldProcessForgotPasswordSuccessfully() {
        ForgotPasswordRequestDto request = new ForgotPasswordRequestDto("admin@wbscouting.com");
        when(adminRepository.findByEmailAndIsActiveTrue("admin@wbscouting.com")).thenReturn(Optional.of(admin));

        authService.processForgotPassword(request);

        assertThat(admin.getPasswordResetToken()).isNotNull();
        assertThat(admin.getPasswordResetExpiresAt()).isNotNull();
        verify(adminRepository, times(1)).save(admin);
        verify(emailService, times(1)).sendPasswordResetEmail(eq("admin@wbscouting.com"), eq("Super Admin"), anyString());
    }

    @Test
    @DisplayName("Não deve expor erro nem enviar e-mail se administrador não for encontrado (anti-enumeração)")
    void shouldNotFailWhenAdminNotFoundForForgotPassword() {
        ForgotPasswordRequestDto request = new ForgotPasswordRequestDto("desconhecido@wbscouting.com");
        when(adminRepository.findByEmailAndIsActiveTrue("desconhecido@wbscouting.com")).thenReturn(Optional.empty());

        authService.processForgotPassword(request);

        verify(adminRepository, never()).save(any());
        verify(emailService, never()).sendPasswordResetEmail(any(), any(), any());
    }

    @Test
    @DisplayName("Deve redefinir a senha com sucesso quando o token for válido e não expirado")
    void shouldResetPasswordSuccessfully() {
        admin.setPasswordResetToken("valid-token-xyz");
        admin.setPasswordResetExpiresAt(java.time.OffsetDateTime.now().plusMinutes(20));

        ResetPasswordRequestDto request = new ResetPasswordRequestDto("valid-token-xyz", "NovaSenha@2026");

        when(adminRepository.findByPasswordResetToken("valid-token-xyz")).thenReturn(Optional.of(admin));
        when(passwordEncoder.encode("NovaSenha@2026")).thenReturn("new-bcrypt-hash");

        authService.resetPassword(request);

        assertThat(admin.getPasswordHash()).isEqualTo("new-bcrypt-hash");
        assertThat(admin.getPasswordResetToken()).isNull();
        assertThat(admin.getPasswordResetExpiresAt()).isNull();

        verify(adminRepository, times(1)).save(admin);
    }

    @Test
    @DisplayName("Deve lançar InvalidTokenException quando token de redefinição estiver expirado")
    void shouldThrowInvalidTokenExceptionWhenTokenExpired() {
        admin.setPasswordResetToken("expired-token-xyz");
        admin.setPasswordResetExpiresAt(java.time.OffsetDateTime.now().minusMinutes(5));

        ResetPasswordRequestDto request = new ResetPasswordRequestDto("expired-token-xyz", "NovaSenha@2026");

        when(adminRepository.findByPasswordResetToken("expired-token-xyz")).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> authService.resetPassword(request))
                .isInstanceOf(com.wbscouting.api.exception.InvalidTokenException.class)
                .hasMessageContaining("Ungültiges oder abgelaufenes Token.");

        verify(adminRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar InvalidTokenException quando token não existir")
    void shouldThrowInvalidTokenExceptionWhenTokenNotFound() {
        ResetPasswordRequestDto request = new ResetPasswordRequestDto("non-existent-token", "NovaSenha@2026");

        when(adminRepository.findByPasswordResetToken("non-existent-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.resetPassword(request))
                .isInstanceOf(com.wbscouting.api.exception.InvalidTokenException.class)
                .hasMessageContaining("Ungültiges oder abgelaufenes Token.");
    }
}


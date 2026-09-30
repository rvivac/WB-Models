package com.wbscouting.api.service.auth;

import com.wbscouting.api.dto.AuthDTO;
import com.wbscouting.api.dto.auth.ForgotPasswordRequestDto;
import com.wbscouting.api.dto.auth.LoginRequestDto;
import com.wbscouting.api.dto.auth.LoginResponseDto;
import com.wbscouting.api.dto.auth.ResetPasswordRequestDto;
import com.wbscouting.api.entity.Admin;
import com.wbscouting.api.exception.InvalidTokenException;
import com.wbscouting.api.repository.AdminRepository;
import com.wbscouting.api.security.JwtService;
import com.wbscouting.api.service.email.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final com.wbscouting.api.security.TokenBlacklistService tokenBlacklistService;

    @Override
    @Transactional(readOnly = true)
    public LoginResponseDto login(LoginRequestDto request) {
        String email = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";

        Admin admin = adminRepository.findByEmailAndIsActiveTrue(email)
                .orElseThrow(() -> {
                    log.warn("Tentativa de login com e-mail não encontrado ou inativo: {}", email);
                    return new BadCredentialsException("Credenciais inválidas. Verifique seu e-mail e senha.");
                });

        if (!passwordEncoder.matches(request.getPassword(), admin.getPasswordHash())) {
            log.warn("Tentativa de login com senha incorreta para: {}", email);
            throw new BadCredentialsException("Credenciais inválidas. Verifique seu e-mail e senha.");
        }

        String token = jwtService.generateToken(admin);

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                admin, null, admin.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        log.info("Administrador autenticado com sucesso: {}", admin.getEmail());

        return LoginResponseDto.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationInSeconds())
                .adminName(admin.getName())
                .adminEmail(admin.getEmail())
                .build();
    }

    @Override
    @Transactional
    public void processForgotPassword(ForgotPasswordRequestDto request) {
        String email = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";
        Optional<Admin> optionalAdmin = adminRepository.findByEmailAndIsActiveTrue(email);

        if (optionalAdmin.isPresent()) {
            Admin admin = optionalAdmin.get();
            String resetToken = UUID.randomUUID().toString();
            admin.setPasswordResetToken(resetToken);
            admin.setPasswordResetExpiresAt(OffsetDateTime.now().plusMinutes(30)); // 30 minutos de validade
            adminRepository.save(admin);

            emailService.sendPasswordResetEmail(admin.getEmail(), admin.getName(), resetToken);
            log.info("Token de recuperação de senha gerado para admin ID: {}", admin.getId());
        } else {
            log.info("Solicitação de recuperação de senha para e-mail não cadastrado ou inativo: {}", email);
        }
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequestDto request) {
        Admin admin = adminRepository.findByPasswordResetToken(request.getToken())
                .orElseThrow(() -> new InvalidTokenException("O link de redefinição de senha é inválido ou expirou."));

        if (!Boolean.TRUE.equals(admin.getIsActive())) {
            log.warn("Tentativa de redefinição de senha para administrador inativo: {}", admin.getEmail());
            throw new InvalidTokenException("O link de redefinição de senha é inválido ou expirou.");
        }

        if (admin.getPasswordResetExpiresAt() == null || admin.getPasswordResetExpiresAt().isBefore(OffsetDateTime.now())) {
            log.warn("Tentativa de redefinição com token expirado para admin ID: {}", admin.getId());
            throw new InvalidTokenException("O link de redefinição de senha é inválido ou expirou.");
        }

        if (request.getConfirmPassword() != null && !request.getConfirmPassword().isBlank()) {
            if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                throw new IllegalArgumentException("As senhas não coincidem.");
            }
        }

        validatePasswordComplexity(request.getNewPassword());

        admin.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        admin.setPasswordResetToken(null);
        admin.setPasswordResetExpiresAt(null);
        adminRepository.save(admin);

        log.info("Senha redefinida com sucesso para o administrador ID: {}", admin.getId());
    }

    private void validatePasswordComplexity(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("A senha deve ter no mínimo 8 caracteres.");
        }
        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasSpecialOrDigit = false;

        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else hasSpecialOrDigit = true;
        }

        if (!hasUpper || !hasLower || !hasSpecialOrDigit) {
            throw new IllegalArgumentException("A senha deve conter letras maiúsculas, minúsculas e pelo menos um dígito ou caractere especial.");
        }
    }

    @Override
    @Transactional
    public AuthDTO.MessageResponse forgotPassword(AuthDTO.ForgotPasswordRequest request) {
        processForgotPassword(new ForgotPasswordRequestDto(request.getEmail()));
        return new AuthDTO.MessageResponse(
                "Se o e-mail informado estiver cadastrado em nosso sistema, as instruções para redefinição de senha serão enviadas em instantes."
        );
    }

    @Override
    @Transactional
    public AuthDTO.MessageResponse resetPassword(AuthDTO.ResetPasswordRequest request) {
        resetPassword(new ResetPasswordRequestDto(request.getToken(), request.getNewPassword()));
        return new AuthDTO.MessageResponse("Senha redefinida com sucesso.");
    }

    @Override
    public void logout(jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response) {
        String bearerToken = request.getHeader("Authorization");
        if (org.springframework.util.StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7).trim();
            try {
                java.util.Date expiration = jwtService.extractExpiration(token);
                tokenBlacklistService.blacklistToken(token, expiration);
            } catch (Exception ex) {
                tokenBlacklistService.blacklistToken(token, null);
            }
        }

        // Limpeza de cookies de sessão / refresh token
        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if (cookie.getName().equalsIgnoreCase("refreshToken") ||
                        cookie.getName().equalsIgnoreCase("wb_refresh_token")) {
                    cookie.setValue("");
                    cookie.setPath("/");
                    cookie.setMaxAge(0);
                    cookie.setHttpOnly(true);
                    response.addCookie(cookie);
                }
            }
        }

        org.springframework.http.ResponseCookie deleteCookie = org.springframework.http.ResponseCookie.from("wb_refresh_token", "")
                .path("/")
                .maxAge(0)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .build();
        response.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE, deleteCookie.toString());

        log.info("Sessão administrativa encerrada e credenciais revogadas com sucesso (Hard Logout).");
    }
}

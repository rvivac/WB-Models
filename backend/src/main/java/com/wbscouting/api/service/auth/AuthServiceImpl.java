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

    @Override
    @Transactional(readOnly = true)
    public LoginResponseDto login(LoginRequestDto request) {
        String email = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";

        Admin admin = adminRepository.findByEmailAndIsActiveTrue(email)
                .orElseThrow(() -> {
                    log.warn("Tentativa de login com e-mail não encontrado ou inativo: {}", email);
                    return new BadCredentialsException("Email ou senha inválidos.");
                });

        if (!passwordEncoder.matches(request.getPassword(), admin.getPasswordHash())) {
            log.warn("Tentativa de login com senha incorreta para: {}", email);
            throw new BadCredentialsException("Email ou senha inválidos.");
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
            admin.setPasswordResetExpiresAt(OffsetDateTime.now().plusMinutes(30)); // 30 Minuten Gültigkeit
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
                .orElseThrow(() -> new InvalidTokenException("Ungültiges oder abgelaufenes Token."));

        if (!Boolean.TRUE.equals(admin.getIsActive())) {
            log.warn("Tentativa de redefinição de senha para administrador inativo: {}", admin.getEmail());
            throw new InvalidTokenException("Ungültiges oder abgelaufenes Token.");
        }

        if (admin.getPasswordResetExpiresAt() == null || admin.getPasswordResetExpiresAt().isBefore(OffsetDateTime.now())) {
            log.warn("Tentativa de redefinição com token expirado para admin ID: {}", admin.getId());
            throw new InvalidTokenException("Ungültiges oder abgelaufenes Token.");
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
            throw new IllegalArgumentException("Das Passwort muss mindestens 8 Zeichen lang sein.");
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
            throw new IllegalArgumentException("Das Passwort muss Groß- und Kleinbuchstaben sowie mindestens eine Ziffer oder ein Sonderzeichen enthalten.");
        }
    }

    @Override
    @Transactional
    public AuthDTO.MessageResponse forgotPassword(AuthDTO.ForgotPasswordRequest request) {
        processForgotPassword(new ForgotPasswordRequestDto(request.getEmail()));
        return new AuthDTO.MessageResponse(
                "Wenn die E-Mail im System registriert ist, wurde ein Wiederherstellungslink gesendet."
        );
    }

    @Override
    @Transactional
    public AuthDTO.MessageResponse resetPassword(AuthDTO.ResetPasswordRequest request) {
        resetPassword(new ResetPasswordRequestDto(request.getToken(), request.getNewPassword()));
        return new AuthDTO.MessageResponse("Passwort erfolgreich zurückgesetzt.");
    }
}

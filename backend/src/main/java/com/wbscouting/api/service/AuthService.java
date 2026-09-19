package com.wbscouting.api.service;

import com.wbscouting.api.dto.AuthDTO;
import com.wbscouting.api.entity.Admin;
import com.wbscouting.api.exception.ResourceNotFoundException;
import com.wbscouting.api.repository.AdminRepository;
import com.wbscouting.api.security.JwtTokenProvider;
import com.wbscouting.api.service.email.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
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
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Transactional(readOnly = true)
    public AuthDTO.AuthResponse login(AuthDTO.LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);

        Admin admin = adminRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Administrador", "email", request.getEmail()));

        return AuthDTO.AuthResponse.builder()
                .token(jwt)
                .tokenType("Bearer")
                .name(admin.getName())
                .email(admin.getEmail())
                .role(admin.getRole().name())
                .build();
    }

    @Transactional
    public AuthDTO.MessageResponse forgotPassword(AuthDTO.ForgotPasswordRequest request) {
        Optional<Admin> optionalAdmin = adminRepository.findByEmail(request.getEmail().trim().toLowerCase());

        if (optionalAdmin.isPresent()) {
            Admin admin = optionalAdmin.get();
            if (Boolean.TRUE.equals(admin.getIsActive())) {
                String resetToken = UUID.randomUUID().toString();
                admin.setPasswordResetToken(resetToken);
                admin.setPasswordResetExpiresAt(OffsetDateTime.now().plusHours(1));
                adminRepository.save(admin);

                emailService.sendPasswordResetEmail(admin.getEmail(), admin.getName(), resetToken);
                log.info("Token de recuperação de senha gerado para admin ID: {}", admin.getId());
            } else {
                log.warn("Tentativa de recuperação de senha para admin inativo: {}", request.getEmail());
            }
        } else {
            log.info("Solicitação de recuperação de senha para e-mail não cadastrado: {}", request.getEmail());
        }

        // Resposta neutra para proteção contra enumeração de e-mails
        return new AuthDTO.MessageResponse(
                "Se o e-mail informado estiver cadastrado, as instruções para redefinição de senha foram enviadas."
        );
    }

    @Transactional
    public AuthDTO.MessageResponse resetPassword(AuthDTO.ResetPasswordRequest request) {
        Admin admin = adminRepository.findByPasswordResetToken(request.getToken())
                .orElseThrow(() -> new IllegalArgumentException("Token de recuperação de senha inválido ou inexistente."));

        if (admin.getPasswordResetExpiresAt() == null || admin.getPasswordResetExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new IllegalArgumentException("O token de recuperação de senha expirou. Por favor, solicite um novo link.");
        }

        admin.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        admin.setPasswordResetToken(null);
        admin.setPasswordResetExpiresAt(null);
        adminRepository.save(admin);

        log.info("Senha redefinida com sucesso para o administrador ID: {}", admin.getId());
        return new AuthDTO.MessageResponse("Senha redefinida com sucesso! Você já pode realizar o login.");
    }
}

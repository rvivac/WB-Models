package com.wbscouting.api.service.admin;

import com.wbscouting.api.dto.admin.ChangePasswordRequestDto;
import com.wbscouting.api.dto.auth.TwoFactorConfirmRequestDto;
import com.wbscouting.api.dto.auth.TwoFactorConfirmResponseDto;
import com.wbscouting.api.dto.auth.TwoFactorDisableRequestDto;
import com.wbscouting.api.dto.auth.TwoFactorSetupResponseDto;
import com.wbscouting.api.entity.Admin;
import com.wbscouting.api.exception.ResourceNotFoundException;
import com.wbscouting.api.repository.AdminRepository;
import com.wbscouting.api.service.auth.TotpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminProfileServiceImpl implements AdminProfileService {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final TotpService totpService;

    @Override
    @Transactional
    public void changePassword(String email, ChangePasswordRequestDto request) {
        Admin admin = getAdminByEmail(email);

        if (!passwordEncoder.matches(request.getCurrentPassword(), admin.getPasswordHash())) {
            throw new BadCredentialsException("A senha atual informada está incorreta.");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("A confirmação da nova senha não confere.");
        }

        if (request.getNewPassword().equals(request.getCurrentPassword())) {
            throw new IllegalArgumentException("A nova senha deve ser diferente da senha atual.");
        }

        validatePasswordComplexity(request.getNewPassword());

        admin.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        admin.setMustChangePassword(false);
        adminRepository.save(admin);

        log.info("Senha alterada com sucesso para o administrador ID: {}", admin.getId());
    }

    @Override
    @Transactional
    public TwoFactorSetupResponseDto setup2fa(String email) {
        Admin admin = getAdminByEmail(email);

        String secret = totpService.generateSecret();
        admin.setTotpSecret(secret);
        adminRepository.save(admin);

        String otpauthUrl = totpService.getOtpauthUrl(admin.getEmail(), secret);
        String qrCodeDataUrl = totpService.generateQrCodeDataUrl(otpauthUrl);

        log.info("Setup 2FA iniciado para o administrador ID: {}", admin.getId());

        return TwoFactorSetupResponseDto.builder()
                .secret(secret)
                .otpauthUrl(otpauthUrl)
                .qrCodeDataUrl(qrCodeDataUrl)
                .build();
    }

    @Override
    @Transactional
    public TwoFactorConfirmResponseDto confirm2fa(String email, TwoFactorConfirmRequestDto request) {
        Admin admin = getAdminByEmail(email);

        if (admin.getTotpSecret() == null || admin.getTotpSecret().isBlank()) {
            throw new IllegalStateException("O setup de 2FA ainda não foi iniciado.");
        }

        boolean valid = totpService.verifyCode(admin.getTotpSecret(), request.getCode());
        if (!valid) {
            throw new IllegalArgumentException("Código de autenticação inválido ou expirado. Tente novamente.");
        }

        List<String> rawBackupCodes = totpService.generateBackupCodes(8);
        List<String> hashedBackupCodes = rawBackupCodes.stream()
                .map(totpService::hashBackupCode)
                .toList();

        admin.setIs2faEnabled(true);
        admin.setBackupCodes(hashedBackupCodes);
        adminRepository.save(admin);

        log.info("2FA ativado com sucesso para o administrador ID: {}", admin.getId());

        return TwoFactorConfirmResponseDto.builder()
                .enabled(true)
                .backupCodes(rawBackupCodes)
                .build();
    }

    @Override
    @Transactional
    public void disable2fa(String email, TwoFactorDisableRequestDto request) {
        Admin admin = getAdminByEmail(email);

        if (!passwordEncoder.matches(request.getPassword(), admin.getPasswordHash())) {
            throw new BadCredentialsException("Senha incorreta. Não foi possível desativar a autenticação em duas etapas.");
        }

        admin.setIs2faEnabled(false);
        admin.setTotpSecret(null);
        admin.setBackupCodes(null);
        adminRepository.save(admin);

        log.info("2FA desativado para o administrador ID: {}", admin.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> get2faStatus(String email) {
        Admin admin = getAdminByEmail(email);
        return Map.of(
                "is2faEnabled", Boolean.TRUE.equals(admin.getIs2faEnabled())
        );
    }

    private Admin getAdminByEmail(String email) {
        String cleanEmail = email != null ? email.trim().toLowerCase(Locale.ROOT) : "";
        return adminRepository.findByEmailAndIsActiveTrue(cleanEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Administrador não encontrado ou inativo: " + cleanEmail));
    }

    private void validatePasswordComplexity(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("A nova senha deve ter no mínimo 8 caracteres.");
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
            throw new IllegalArgumentException("A nova senha deve conter letras maiúsculas, minúsculas e pelo menos um número ou caractere especial.");
        }
    }
}

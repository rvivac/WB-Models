package com.wbscouting.api.service.admin;

import com.wbscouting.api.dto.admin.AdminUserResponseDto;
import com.wbscouting.api.dto.admin.CreateAdminUserRequestDto;
import com.wbscouting.api.dto.admin.CreateAdminUserResponseDto;
import com.wbscouting.api.entity.Admin;
import com.wbscouting.api.enums.AdminRole;
import com.wbscouting.api.exception.ResourceNotFoundException;
import com.wbscouting.api.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional(readOnly = true)
    public Page<AdminUserResponseDto> listUsers(Pageable pageable) {
        return adminRepository.findAll(pageable).map(this::toResponseDto);
    }

    @Override
    @Transactional
    public CreateAdminUserResponseDto createUser(CreateAdminUserRequestDto request) {
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        if (adminRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Já existe um administrador cadastrado com o e-mail: " + email);
        }

        String rawPassword = request.getPassword();
        if (rawPassword == null || rawPassword.isBlank()) {
            rawPassword = generateSecureTemporaryPassword();
        } else {
            validatePasswordComplexity(rawPassword);
        }

        Admin admin = Admin.builder()
                .name(request.getName().trim())
                .email(email)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .role(request.getRole())
                .isActive(true)
                .is2faEnabled(false)
                .mustChangePassword(true)
                .build();

        Admin saved = adminRepository.save(admin);
        log.info("Novo administrador cadastrado com sucesso. ID: {}, E-mail: {}, Role: {}", saved.getId(), saved.getEmail(), saved.getRole());

        return CreateAdminUserResponseDto.builder()
                .user(toResponseDto(saved))
                .temporaryPassword(rawPassword)
                .build();
    }

    @Override
    @Transactional
    public AdminUserResponseDto toggleUserStatus(UUID id, String authenticatedEmail) {
        Admin target = adminRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Administrador não encontrado com o ID: " + id));

        if (target.getEmail().equalsIgnoreCase(authenticatedEmail.trim())) {
            throw new IllegalArgumentException("Operação bloqueada: Não é permitido desativar sua própria conta de administrador.");
        }

        boolean willDeactivate = Boolean.TRUE.equals(target.getIsActive());

        if (willDeactivate && isSuperRole(target.getRole())) {
            long activeSuperAdmins = adminRepository.countByRoleInAndIsActiveTrue(List.of(AdminRole.SUPER_ADMIN, AdminRole.WEBMASTER));
            if (activeSuperAdmins <= 1) {
                throw new IllegalStateException("Operação bloqueada: Não é permitido desativar o único Webmaster/SuperAdmin ativo no sistema.");
            }
        }

        target.setIsActive(!willDeactivate);
        Admin updated = adminRepository.save(target);
        log.info("Status do administrador ID: {} alterado para isActive={}", updated.getId(), updated.getIsActive());

        return toResponseDto(updated);
    }

    @Override
    @Transactional
    public AdminUserResponseDto updateRole(UUID id, AdminRole newRole, String authenticatedEmail) {
        Admin target = adminRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Administrador não encontrado com o ID: " + id));

        if (isSuperRole(target.getRole()) && !isSuperRole(newRole)) {
            long activeSuperAdmins = adminRepository.countByRoleInAndIsActiveTrue(List.of(AdminRole.SUPER_ADMIN, AdminRole.WEBMASTER));
            if (activeSuperAdmins <= 1) {
                throw new IllegalStateException("Operação bloqueada: Não é permitido rebaixar o único Webmaster/SuperAdmin ativo no sistema.");
            }
        }

        target.setRole(newRole);
        Admin updated = adminRepository.save(target);
        log.info("Papel do administrador ID: {} atualizado para {}", updated.getId(), updated.getRole());

        return toResponseDto(updated);
    }

    private boolean isSuperRole(AdminRole role) {
        return role == AdminRole.SUPER_ADMIN || role == AdminRole.WEBMASTER;
    }

    private String generateSecureTemporaryPassword() {
        String upper = "ABCDEFGHJKLMNPQRSTUVWXYZ";
        String lower = "abcdefghijkmnopqrstuvwxyz";
        String digits = "23456789";
        String special = "@#$%&*!";

        StringBuilder sb = new StringBuilder();
        sb.append("Wb@");
        for (int i = 0; i < 3; i++) {
            sb.append(upper.charAt(secureRandom.nextInt(upper.length())));
            sb.append(lower.charAt(secureRandom.nextInt(lower.length())));
        }
        sb.append(digits.charAt(secureRandom.nextInt(digits.length())));
        sb.append(special.charAt(secureRandom.nextInt(special.length())));
        return sb.toString();
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
            throw new IllegalArgumentException("A senha deve conter letras maiúsculas, minúsculas e pelo menos um número ou caractere especial.");
        }
    }

    private AdminUserResponseDto toResponseDto(Admin admin) {
        return AdminUserResponseDto.builder()
                .id(admin.getId())
                .name(admin.getName())
                .email(admin.getEmail())
                .role(admin.getRole())
                .isActive(admin.getIsActive())
                .is2faEnabled(admin.getIs2faEnabled())
                .mustChangePassword(admin.getMustChangePassword())
                .lastLoginAt(admin.getLastLoginAt())
                .createdAt(admin.getCreatedAt())
                .build();
    }
}

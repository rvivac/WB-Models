package com.wbscouting.api.controller.admin;

import com.wbscouting.api.dto.admin.ChangePasswordRequestDto;
import com.wbscouting.api.dto.auth.TwoFactorConfirmRequestDto;
import com.wbscouting.api.dto.auth.TwoFactorConfirmResponseDto;
import com.wbscouting.api.dto.auth.TwoFactorDisableRequestDto;
import com.wbscouting.api.dto.auth.TwoFactorSetupResponseDto;
import com.wbscouting.api.service.admin.AdminProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping({"/api/v1/admin/profile", "/admin/profile"})
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class AdminProfileController {

    private final AdminProfileService adminProfileService;

    private String resolveEmail(java.security.Principal principal) {
        if (principal != null && principal.getName() != null && !principal.getName().isBlank()) {
            return principal.getName();
        }
        org.springframework.security.core.Authentication auth =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null && !auth.getName().isBlank()) {
            return auth.getName();
        }
        throw new org.springframework.security.access.AccessDeniedException("Usuário não autenticado.");
    }

    @PutMapping("/password")
    public ResponseEntity<Map<String, String>> changePassword(
            @Valid @RequestBody ChangePasswordRequestDto request,
            java.security.Principal principal) {
        adminProfileService.changePassword(resolveEmail(principal), request);
        return ResponseEntity.ok(Map.of("message", "Senha alterada com sucesso."));
    }

    @PostMapping("/2fa/setup")
    public ResponseEntity<TwoFactorSetupResponseDto> setup2fa(
            java.security.Principal principal) {
        TwoFactorSetupResponseDto response = adminProfileService.setup2fa(resolveEmail(principal));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/2fa/confirm")
    public ResponseEntity<TwoFactorConfirmResponseDto> confirm2fa(
            @Valid @RequestBody TwoFactorConfirmRequestDto request,
            java.security.Principal principal) {
        TwoFactorConfirmResponseDto response = adminProfileService.confirm2fa(resolveEmail(principal), request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/2fa/disable")
    public ResponseEntity<Map<String, String>> disable2fa(
            @Valid @RequestBody TwoFactorDisableRequestDto request,
            java.security.Principal principal) {
        adminProfileService.disable2fa(resolveEmail(principal), request);
        return ResponseEntity.ok(Map.of("message", "Autenticação em duas etapas (2FA) desativada com sucesso."));
    }

    @GetMapping("/2fa/status")
    public ResponseEntity<Map<String, Object>> get2faStatus(
            java.security.Principal principal) {
        Map<String, Object> status = adminProfileService.get2faStatus(resolveEmail(principal));
        return ResponseEntity.ok(status);
    }
}

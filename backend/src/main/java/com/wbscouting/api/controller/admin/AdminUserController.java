package com.wbscouting.api.controller.admin;

import com.wbscouting.api.dto.admin.AdminUserResponseDto;
import com.wbscouting.api.dto.admin.CreateAdminUserRequestDto;
import com.wbscouting.api.dto.admin.CreateAdminUserResponseDto;
import com.wbscouting.api.dto.admin.UpdateAdminRoleRequestDto;
import com.wbscouting.api.service.admin.AdminUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/admin/users", "/admin/users"})
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('WEBMASTER', 'SUPER_ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    public ResponseEntity<Page<AdminUserResponseDto>> listUsers(
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        Page<AdminUserResponseDto> users = adminUserService.listUsers(pageable);
        return ResponseEntity.ok(users);
    }

    @PostMapping
    public ResponseEntity<CreateAdminUserResponseDto> createUser(
            @Valid @RequestBody CreateAdminUserRequestDto request) {
        CreateAdminUserResponseDto response = adminUserService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private String resolveEmail(java.security.Principal principal) {
        if (principal != null && principal.getName() != null && !principal.getName().isBlank()) {
            return principal.getName();
        }
        org.springframework.security.core.Authentication auth =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null && !auth.getName().isBlank()) {
            return auth.getName();
        }
        return "";
    }

    @PatchMapping("/{id}/toggle-status")
    public ResponseEntity<AdminUserResponseDto> toggleUserStatus(
            @PathVariable UUID id,
            java.security.Principal principal) {
        String email = resolveEmail(principal);
        AdminUserResponseDto response = adminUserService.toggleUserStatus(id, email);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<AdminUserResponseDto> updateRole(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAdminRoleRequestDto request,
            java.security.Principal principal) {
        String email = resolveEmail(principal);
        AdminUserResponseDto response = adminUserService.updateRole(id, request.getRole(), email);
        return ResponseEntity.ok(response);
    }
}

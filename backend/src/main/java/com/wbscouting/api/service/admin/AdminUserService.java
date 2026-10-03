package com.wbscouting.api.service.admin;

import com.wbscouting.api.dto.admin.AdminUserResponseDto;
import com.wbscouting.api.dto.admin.CreateAdminUserRequestDto;
import com.wbscouting.api.dto.admin.CreateAdminUserResponseDto;
import com.wbscouting.api.enums.AdminRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AdminUserService {

    Page<AdminUserResponseDto> listUsers(Pageable pageable);

    CreateAdminUserResponseDto createUser(CreateAdminUserRequestDto request);

    AdminUserResponseDto toggleUserStatus(UUID id, String authenticatedEmail);

    AdminUserResponseDto updateRole(UUID id, AdminRole newRole, String authenticatedEmail);

    void deleteSecondaryAdmin(UUID id, String authenticatedEmail);
}

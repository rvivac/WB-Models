package com.wbscouting.api.dto.admin;

import com.wbscouting.api.enums.AdminRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminUserResponseDto {
    private UUID id;
    private String name;
    private String email;
    private AdminRole role;
    private Boolean isActive;
    private Boolean is2faEnabled;
    private Boolean mustChangePassword;
    private OffsetDateTime lastLoginAt;
    private OffsetDateTime createdAt;
}

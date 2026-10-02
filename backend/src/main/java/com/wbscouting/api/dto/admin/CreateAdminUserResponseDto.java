package com.wbscouting.api.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateAdminUserResponseDto {
    private AdminUserResponseDto user;
    private String temporaryPassword;
}

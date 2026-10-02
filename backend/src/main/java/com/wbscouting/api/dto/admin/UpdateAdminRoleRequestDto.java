package com.wbscouting.api.dto.admin;

import com.wbscouting.api.enums.AdminRole;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateAdminRoleRequestDto {

    @NotNull(message = "O papel (role) é obrigatório.")
    private AdminRole role;
}

package com.wbscouting.api.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TwoFactorDisableRequestDto {

    @NotBlank(message = "A senha atual é obrigatória para desativar o 2FA.")
    private String password;
}

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
public class TwoFactorChallengeRequestDto {

    @NotBlank(message = "O token provisório de desafio 2FA é obrigatório.")
    private String tempToken;

    @NotBlank(message = "O código de verificação (TOTP de 6 dígitos ou código de backup) é obrigatório.")
    private String code;
}

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
public class TwoFactorConfirmRequestDto {

    @NotBlank(message = "O código TOTP de 6 dígitos é obrigatório.")
    private String code;
}

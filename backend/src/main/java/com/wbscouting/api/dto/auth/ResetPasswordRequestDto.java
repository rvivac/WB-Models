package com.wbscouting.api.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResetPasswordRequestDto {

    @NotBlank(message = "Token ist erforderlich.")
    private String token;

    @NotBlank(message = "Das neue Passwort ist erforderlich.")
    @Size(min = 8, message = "Das Passwort muss mindestens 8 Zeichen lang sein.")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9\\W]).+$",
            message = "Das Passwort muss mindestens einen Großbuchstaben, einen Kleinbuchstaben und eine Ziffer oder ein Sonderzeichen enthalten."
    )
    private String newPassword;
}

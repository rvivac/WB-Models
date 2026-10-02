package com.wbscouting.api.dto.admin;

import com.wbscouting.api.enums.AdminRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateAdminUserRequestDto {

    @NotBlank(message = "O nome é obrigatório.")
    @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres.")
    private String name;

    @NotBlank(message = "O e-mail é obrigatório.")
    @Email(message = "Informe um e-mail válido.")
    @Size(max = 255, message = "O e-mail deve ter no máximo 255 caracteres.")
    private String email;

    @NotNull(message = "O nível de acesso (role) é obrigatório.")
    private AdminRole role;

    private String password;
}

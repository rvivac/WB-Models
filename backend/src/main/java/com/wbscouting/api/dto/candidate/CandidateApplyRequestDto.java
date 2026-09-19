package com.wbscouting.api.dto.candidate;

import com.wbscouting.api.enums.GenderType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateApplyRequestDto {

    @NotBlank(message = "O nome completo é obrigatório.")
    @Size(min = 3, max = 200, message = "O nome completo deve conter no mínimo 3 caracteres.")
    private String fullName;

    @NotBlank(message = "O e-mail é obrigatório.")
    @Email(message = "Formato de e-mail inválido.")
    private String email;

    @NotBlank(message = "O telefone de contato é obrigatório.")
    @Pattern(regexp = "^\\+?[0-9\\s()\\-\\.\\/]{10,25}$", message = "O telefone deve incluir DDD e possuir formato válido.")
    private String phone;

    @NotNull(message = "A data de nascimento é obrigatória.")
    @Past(message = "A data de nascimento deve ser uma data no passado.")
    private LocalDate birthDate;

    @NotNull(message = "O gênero é obrigatório (FEMALE ou MALE).")
    private GenderType gender;

    @NotNull(message = "A altura em centímetros é obrigatória.")
    @Min(value = 140, message = "A altura mínima permitida é de 140 cm.")
    @Max(value = 220, message = "A altura máxima permitida é de 220 cm.")
    private Integer heightCm;

    @NotBlank(message = "A cidade é obrigatória.")
    private String city;

    @NotBlank(message = "O estado (UF) é obrigatório.")
    private String state;

    // Campos de Responsável Legal (obrigatórios se menor de 18 anos)
    private String legalGuardianName;
    private String legalGuardianContact;

    // Medidas corporais opcionais
    @DecimalMin(value = "30.00", message = "Medida de busto/tórax inválida.")
    @DecimalMax(value = "200.00", message = "Medida de busto/tórax inválida.")
    private BigDecimal bustChestCm;

    @DecimalMin(value = "30.00", message = "Medida de cintura inválida.")
    @DecimalMax(value = "200.00", message = "Medida de cintura inválida.")
    private BigDecimal waistCm;

    @DecimalMin(value = "30.00", message = "Medida de quadril inválida.")
    @DecimalMax(value = "200.00", message = "Medida de quadril inválida.")
    private BigDecimal hipsCm;

    private String shoeSize;
    private String dressSize;

    // Redes sociais e portfólio opcionais
    private String instagramHandle;
    private String portfolioUrl;
}

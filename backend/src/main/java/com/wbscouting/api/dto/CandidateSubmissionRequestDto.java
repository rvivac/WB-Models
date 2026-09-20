package com.wbscouting.api.dto;

import com.wbscouting.api.enums.SubmissionGender;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateSubmissionRequestDto {

    @NotBlank(message = "O nome completo é obrigatório")
    @Size(min = 3, max = 120, message = "O nome completo deve conter entre 3 e 120 caracteres")
    private String fullName;

    @NotBlank(message = "O e-mail é obrigatório")
    @Email(regexp = "^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$", message = "E-mail com formato inválido")
    @Size(max = 100, message = "O e-mail não pode exceder 100 caracteres")
    private String email;

    @NotBlank(message = "O telefone de contato é obrigatório")
    @Pattern(
        regexp = "^(\\+?[1-9]\\d{1,14}|\\(?\\d{2}\\)?\\s?\\d{4,5}-?\\d{4})$",
        message = "Telefone inválido. Utilize o formato internacional E.164 ou nacional (ex: (11) 98765-4321)"
    )
    @Size(max = 25, message = "Telefone deve ter no máximo 25 caracteres")
    private String phone;

    @NotNull(message = "A data de nascimento é obrigatória")
    @Past(message = "A data de nascimento deve ser uma data passada")
    private LocalDate birthDate;

    @NotNull(message = "O gênero é obrigatório")
    private SubmissionGender gender;

    @NotBlank(message = "A cidade é obrigatória")
    @Size(max = 80, message = "A cidade não pode exceder 80 caracteres")
    private String city;

    @NotBlank(message = "O estado (UF) é obrigatório")
    @Pattern(regexp = "^[A-Za-z]{2}$", message = "UF deve ter exatamente 2 caracteres alfabéticos")
    private String state;

    @NotNull(message = "A altura é obrigatória")
    @DecimalMin(value = "1.20", message = "A altura mínima permitida é de 1.20m")
    @DecimalMax(value = "2.30", message = "A altura máxima permitida é de 2.30m")
    private BigDecimal height;

    @Positive(message = "A medida do busto deve ser um número positivo")
    private BigDecimal bust;

    @Positive(message = "A medida da cintura deve ser um número positivo")
    private BigDecimal waist;

    @Positive(message = "A medida do quadril deve ser um número positivo")
    private BigDecimal hips;

    @Min(value = 30, message = "Tamanho do calçado deve ser no mínimo 30")
    @Max(value = 50, message = "Tamanho do calçado deve ser no máximo 50")
    private Integer shoeSize;

    @Size(max = 50, message = "Cor dos olhos deve ter no máximo 50 caracteres")
    private String eyeColor;

    @Size(max = 50, message = "Cor do cabelo deve ter no máximo 50 caracteres")
    private String hairColor;

    @Size(max = 80, message = "Instagram deve ter no máximo 80 caracteres")
    private String instagramHandle;

    // Campos para menores de 18 anos (validados adicionalmente pelo serviço com base na data de nascimento)
    @Size(max = 120, message = "Nome do responsável não pode exceder 120 caracteres")
    private String guardianName;

    @Size(max = 25, message = "Telefone do responsável não pode exceder 25 caracteres")
    private String guardianPhone;

    @Email(message = "E-mail do responsável inválido")
    @Size(max = 100, message = "E-mail do responsável não pode exceder 100 caracteres")
    private String guardianEmail;

    @NotNull(message = "O aceite dos termos da LGPD é obrigatório")
    @AssertTrue(message = "O aceite dos termos da LGPD deve ser explicitamente concedido")
    private Boolean lgpdConsent;
}

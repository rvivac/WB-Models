package com.wbscouting.api.dto.model;

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
public class ModelCreateRequestDto {

    @NotBlank(message = "O nome artístico é obrigatório.")
    @Size(max = 150, message = "O nome artístico deve conter no máximo 150 caracteres.")
    private String stageName;

    @NotNull(message = "O gênero é obrigatório.")
    private GenderType gender;

    @Builder.Default
    private Boolean isStar = false;

    @Builder.Default
    private Boolean isFeaturedHome = false;

    private Integer featuredOrder;

    @Builder.Default
    private Boolean isActive = true;

    private String primaryPhotoUrl;

    @Size(max = 255, message = "A URL do Instagram deve conter no máximo 255 caracteres.")
    private String instagramUrl;

    @Past(message = "A data de nascimento deve ser uma data no passado.")
    private LocalDate birthDate;

    @Min(value = 50, message = "A altura mínima permitida é 50 cm.")
    @Max(value = 250, message = "A altura máxima permitida é 250 cm.")
    private Integer heightCm;

    @Size(max = 100, message = "A cidade deve conter no máximo 100 caracteres.")
    private String city;

    @Size(max = 100, message = "A nacionalidade deve conter no máximo 100 caracteres.")
    private String nationality;

    @Size(max = 20, message = "O tamanho de manequim deve conter no máximo 20 caracteres.")
    private String dressSize;

    @Size(max = 20, message = "O tamanho de calçado deve conter no máximo 20 caracteres.")
    private String shoeSize;

    @DecimalMin(value = "20.00", message = "A medida do busto/tórax deve ser no mínimo 20 cm.")
    @DecimalMax(value = "200.00", message = "A medida do busto/tórax deve ser no máximo 200 cm.")
    private BigDecimal bustChestCm;

    @DecimalMin(value = "20.00", message = "A medida da cintura deve ser no mínimo 20 cm.")
    @DecimalMax(value = "200.00", message = "A medida da cintura deve ser no máximo 200 cm.")
    private BigDecimal waistCm;

    @DecimalMin(value = "20.00", message = "A medida do quadril deve ser no mínimo 20 cm.")
    @DecimalMax(value = "200.00", message = "A medida do quadril deve ser no máximo 200 cm.")
    private BigDecimal hipsCm;

    @Size(max = 50, message = "A cor do cabelo deve conter no máximo 50 caracteres.")
    private String hairColor;

    @Size(max = 50, message = "A cor dos olhos deve conter no máximo 50 caracteres.")
    private String eyesColor;
}

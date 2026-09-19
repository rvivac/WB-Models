package com.wbscouting.api.dto.model;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModelStatusPatchDto {

    @NotNull(message = "O status de ativação é obrigatório.")
    private Boolean isActive;
}

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
public class ModelStarPatchDto {

    @NotNull(message = "O status de Star é obrigatório.")
    private Boolean isStar;
}

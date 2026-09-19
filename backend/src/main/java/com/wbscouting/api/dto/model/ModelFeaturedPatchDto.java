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
public class ModelFeaturedPatchDto {

    @NotNull(message = "O status de destaque na home é obrigatório.")
    private Boolean isFeaturedHome;

    private Integer featuredOrder;
}

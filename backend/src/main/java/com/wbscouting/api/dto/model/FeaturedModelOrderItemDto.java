package com.wbscouting.api.dto.model;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeaturedModelOrderItemDto {

    @NotNull(message = "O ID do modelo é obrigatório.")
    private UUID modelId;

    @NotNull(message = "A ordem de exibição é obrigatória.")
    private Integer displayOrder;
}

package com.wbscouting.api.dto.media;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaOrderItemDto {

    @NotNull(message = "O ID da mídia é obrigatório.")
    private UUID mediaId;

    @NotNull(message = "A ordem de exibição é obrigatória.")
    @Min(value = 0, message = "A ordem de exibição deve ser maior ou igual a zero.")
    private Integer displayOrder;
}

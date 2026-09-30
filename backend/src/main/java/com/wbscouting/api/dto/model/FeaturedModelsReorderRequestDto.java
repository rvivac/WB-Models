package com.wbscouting.api.dto.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeaturedModelsReorderRequestDto {

    @NotEmpty(message = "A lista de itens não pode estar vazia.")
    @Valid
    private List<FeaturedModelOrderItemDto> items;
}

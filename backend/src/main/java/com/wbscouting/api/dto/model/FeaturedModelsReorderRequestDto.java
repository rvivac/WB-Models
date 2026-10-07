package com.wbscouting.api.dto.model;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeaturedModelsReorderRequestDto {

    @Valid
    @Builder.Default
    private List<FeaturedModelOrderItemDto> items = new ArrayList<>();
}


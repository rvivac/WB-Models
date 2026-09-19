package com.wbscouting.api.dto.media;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaReorderRequestDto {

    @NotEmpty(message = "A lista de mídias para reordenação não pode ser vazia.")
    @Valid
    private List<MediaOrderItemDto> items;
}

package com.wbscouting.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AboutPillarDto {
    private Integer order;

    @NotBlank(message = "O título do pilar é obrigatório")
    private String titulo;

    @NotBlank(message = "A descrição do pilar é obrigatória")
    private String descricao;
}

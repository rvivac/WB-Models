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
public class ApplyFaqCreateUpdateDto {

    @NotBlank(message = "A pergunta é obrigatória")
    private String question;

    @NotBlank(message = "A resposta é obrigatória")
    private String answer;

    private Integer displayOrder;

    private Boolean isActive;
}

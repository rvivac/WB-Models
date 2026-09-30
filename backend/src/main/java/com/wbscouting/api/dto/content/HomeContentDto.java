package com.wbscouting.api.dto.content;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HomeContentDto {

    @NotBlank(message = "O título principal (heroTitle) é obrigatório.")
    private String heroTitle;

    @NotBlank(message = "O subtítulo editorial (heroSubtitle) é obrigatório.")
    private String heroSubtitle;

    @NotBlank(message = "A descrição/manifesto (heroDescription) é obrigatória.")
    private String heroDescription;

    @NotBlank(message = "O rótulo do scroll (scrollLabel) é obrigatório.")
    private String scrollLabel;

    @NotBlank(message = "O meta título de SEO (metaTitle) é obrigatório.")
    private String metaTitle;

    @NotBlank(message = "A meta descrição de SEO (metaDescription) é obrigatória.")
    private String metaDescription;

    private String videoUrl;
    private String posterUrl;
}

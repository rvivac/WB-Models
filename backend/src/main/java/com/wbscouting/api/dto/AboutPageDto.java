package com.wbscouting.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AboutPageDto {

    @NotBlank(message = "O título principal é obrigatório")
    private String title;

    private String subtitle;

    private String description;

    private String heroQuote;

    private String manifestoTitle;

    private String manifestoText;

    private String pillarsTitle;

    @Valid
    private List<AboutPillarDto> pillars;

    private AboutSeoDto seo;

    private OffsetDateTime updatedAt;
}

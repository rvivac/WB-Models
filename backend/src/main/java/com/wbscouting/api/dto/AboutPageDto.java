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

    private String pageTitle;

    private String headline;

    private String subtitle;

    private String description;

    private String heroQuote;

    private String quote;

    private String sectionTitle;

    private String manifestoTitle;

    private String manifestoText;

    private String bodyText;

    private String body;

    private String pillarsTitle;

    @Valid
    private List<AboutPillarDto> pillars;

    private AboutSeoDto seo;

    private OffsetDateTime updatedAt;

    public String getPageTitle() {
        return pageTitle != null && !pageTitle.isBlank() ? pageTitle : (headline != null && !headline.isBlank() ? headline : title);
    }

    public String getHeadline() {
        return headline != null && !headline.isBlank() ? headline : (title != null && !title.isBlank() ? title : pageTitle);
    }

    public String getQuote() {
        return quote != null && !quote.isBlank() ? quote : heroQuote;
    }

    public String getSectionTitle() {
        return sectionTitle != null && !sectionTitle.isBlank() ? sectionTitle : (manifestoTitle != null && !manifestoTitle.isBlank() ? manifestoTitle : "Nossa Filosofia");
    }

    public String getManifestoTitle() {
        return manifestoTitle != null && !manifestoTitle.isBlank() ? manifestoTitle : getSectionTitle();
    }

    public String getBodyText() {
        return bodyText != null && !bodyText.isBlank() ? bodyText : (body != null && !body.isBlank() ? body : (manifestoText != null && !manifestoText.isBlank() ? manifestoText : description));
    }

    public String getBody() {
        return body != null && !body.isBlank() ? body : getBodyText();
    }
}

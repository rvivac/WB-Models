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
        return pageTitle != null ? pageTitle : (headline != null ? headline : title);
    }

    public String getHeadline() {
        return headline != null ? headline : (title != null ? title : pageTitle);
    }

    public String getQuote() {
        return quote != null ? quote : heroQuote;
    }

    public String getSectionTitle() {
        return sectionTitle != null ? sectionTitle : manifestoTitle;
    }

    public String getManifestoTitle() {
        return manifestoTitle != null ? manifestoTitle : sectionTitle;
    }

    public String getBodyText() {
        return bodyText != null ? bodyText : (body != null ? body : (manifestoText != null ? manifestoText : description));
    }

    public String getBody() {
        return body != null ? body : getBodyText();
    }
}

package com.wbscouting.api.dto.content;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HomeContentDto {

    private String heroTitle;
    private String heroSubtitle;
    private String heroDescription;
    private String scrollLabel;
    private String videoUrl;
    private String posterUrl;

    // Novo Disclaimer Opcional
    private Boolean disclaimerActive;
    private String disclaimerTitle;
    private String disclaimerText;
    private String disclaimerLinkUrl;
    private String disclaimerLinkLabel;

    // Metadados SEO
    private String metaTitle;
    private String metaDescription;

    // Tempo de exibição do splash em ms (padrão: 1000ms)
    private Integer splashDurationMs;

    // ============================================================
    // 🔥 GETTERS EXPLICITOS (Lombok fallback para Maven)
    // ============================================================
    public String getHeroTitle() { return heroTitle; }
    public String getHeroSubtitle() { return heroSubtitle; }
    public String getHeroDescription() { return heroDescription; }
    public String getScrollLabel() { return scrollLabel; }
    public String getVideoUrl() { return videoUrl; }
    public String getPosterUrl() { return posterUrl; }

    public Boolean getDisclaimerActive() { return disclaimerActive; }
    public String getDisclaimerTitle() { return disclaimerTitle; }
    public String getDisclaimerText() { return disclaimerText; }
    public String getDisclaimerLinkUrl() { return disclaimerLinkUrl; }
    public String getDisclaimerLinkLabel() { return disclaimerLinkLabel; }

    public String getMetaTitle() { return metaTitle; }
    public String getMetaDescription() { return metaDescription; }
    public Integer getSplashDurationMs() { return splashDurationMs; }

    // ============================================================
    // 🔥 SETTERS EXPLICITOS
    // ============================================================
    public void setHeroTitle(String heroTitle) { this.heroTitle = heroTitle; }
    public void setHeroSubtitle(String heroSubtitle) { this.heroSubtitle = heroSubtitle; }
    public void setHeroDescription(String heroDescription) { this.heroDescription = heroDescription; }
    public void setScrollLabel(String scrollLabel) { this.scrollLabel = scrollLabel; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }
    public void setPosterUrl(String posterUrl) { this.posterUrl = posterUrl; }

    public void setDisclaimerActive(Boolean disclaimerActive) { this.disclaimerActive = disclaimerActive; }
    public void setDisclaimerTitle(String disclaimerTitle) { this.disclaimerTitle = disclaimerTitle; }
    public void setDisclaimerText(String disclaimerText) { this.disclaimerText = disclaimerText; }
    public void setDisclaimerLinkUrl(String disclaimerLinkUrl) { this.disclaimerLinkUrl = disclaimerLinkUrl; }
    public void setDisclaimerLinkLabel(String disclaimerLinkLabel) { this.disclaimerLinkLabel = disclaimerLinkLabel; }

    public void setMetaTitle(String metaTitle) { this.metaTitle = metaTitle; }
    public void setMetaDescription(String metaDescription) { this.metaDescription = metaDescription; }
    public void setSplashDurationMs(Integer splashDurationMs) { this.splashDurationMs = splashDurationMs; }

    // ============================================================
    // 🔥 BUILDER MANUAL FALLBACK (evita erro cannot find symbol builder())
    // ============================================================
    public static HomeContentDtoBuilder manualBuilder() { return new HomeContentDtoBuilder(); }
    public static HomeContentDtoBuilder builder() { return new HomeContentDtoBuilder(); }

    public static class HomeContentDtoBuilder {
        private final HomeContentDto h = new HomeContentDto();
        public HomeContentDtoBuilder heroTitle(String v) { h.setHeroTitle(v); return this; }
        public HomeContentDtoBuilder heroSubtitle(String v) { h.setHeroSubtitle(v); return this; }
        public HomeContentDtoBuilder heroDescription(String v) { h.setHeroDescription(v); return this; }
        public HomeContentDtoBuilder scrollLabel(String v) { h.setScrollLabel(v); return this; }
        public HomeContentDtoBuilder videoUrl(String v) { h.setVideoUrl(v); return this; }
        public HomeContentDtoBuilder posterUrl(String v) { h.setPosterUrl(v); return this; }

        public HomeContentDtoBuilder disclaimerActive(Boolean v) { h.setDisclaimerActive(v); return this; }
        public HomeContentDtoBuilder disclaimerTitle(String v) { h.setDisclaimerTitle(v); return this; }
        public HomeContentDtoBuilder disclaimerText(String v) { h.setDisclaimerText(v); return this; }
        public HomeContentDtoBuilder disclaimerLinkUrl(String v) { h.setDisclaimerLinkUrl(v); return this; }
        public HomeContentDtoBuilder disclaimerLinkLabel(String v) { h.setDisclaimerLinkLabel(v); return this; }

        public HomeContentDtoBuilder metaTitle(String v) { h.setMetaTitle(v); return this; }
        public HomeContentDtoBuilder metaDescription(String v) { h.setMetaDescription(v); return this; }
        public HomeContentDtoBuilder splashDurationMs(Integer v) { h.setSplashDurationMs(v); return this; }

        public HomeContentDto build() { return h; }
    }
}

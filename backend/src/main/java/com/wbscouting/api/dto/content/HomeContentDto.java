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
    private String scrollLabel;
    private String metaTitle;
    private String metaDescription;
    private String videoUrl;
    private String posterUrl;

    // ============================================================
    // 🔥 GETTERS EXPLICITOS (Lombok @Data NAO processa no mvnw 3.6.3 Render)
    // ============================================================
    public String getHeroTitle() { return heroTitle; }
    public String getHeroSubtitle() { return heroSubtitle; }
    public String getScrollLabel() { return scrollLabel; }
    public String getMetaTitle() { return metaTitle; }
    public String getMetaDescription() { return metaDescription; }
    public String getVideoUrl() { return videoUrl; }
    public String getPosterUrl() { return posterUrl; }

    // ============================================================
    // 🔥 SETTERS EXPLICITOS
    // ============================================================
    public void setHeroTitle(String heroTitle) { this.heroTitle = heroTitle; }
    public void setHeroSubtitle(String heroSubtitle) { this.heroSubtitle = heroSubtitle; }
    public void setScrollLabel(String scrollLabel) { this.scrollLabel = scrollLabel; }
    public void setMetaTitle(String metaTitle) { this.metaTitle = metaTitle; }
    public void setMetaDescription(String metaDescription) { this.metaDescription = metaDescription; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }
    public void setPosterUrl(String posterUrl) { this.posterUrl = posterUrl; }

    // ============================================================
    // 🔥 BUILDER MANUAL FALLBACK (evita erro cannot find symbol builder() no mvnw)
    // ============================================================
    public static HomeContentDtoBuilder manualBuilder() { return new HomeContentDtoBuilder(); }
    public static HomeContentDtoBuilder builder() { return new HomeContentDtoBuilder(); }

    public static class HomeContentDtoBuilder {
        private final HomeContentDto h = new HomeContentDto();
        public HomeContentDtoBuilder heroTitle(String v) { h.setHeroTitle(v); return this; }
        public HomeContentDtoBuilder heroSubtitle(String v) { h.setHeroSubtitle(v); return this; }
        public HomeContentDtoBuilder scrollLabel(String v) { h.setScrollLabel(v); return this; }
        public HomeContentDtoBuilder metaTitle(String v) { h.setMetaTitle(v); return this; }
        public HomeContentDtoBuilder metaDescription(String v) { h.setMetaDescription(v); return this; }
        public HomeContentDtoBuilder videoUrl(String v) { h.setVideoUrl(v); return this; }
        public HomeContentDtoBuilder posterUrl(String v) { h.setPosterUrl(v); return this; }

        public HomeContentDto build() { return h; }
    }
}

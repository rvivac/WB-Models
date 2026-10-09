package com.wbscouting.api.dto.content;

import com.fasterxml.jackson.annotation.JsonInclude;
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

    private String heroTitle;
    private String heroSubtitle;
    private String heroDescription;
    private String scrollLabel;
    private String metaTitle;
    private String metaDescription;
    private String videoUrl;
    private String posterUrl;

    // Novos campos editoriais do Rodapé
    private String footerDescription;
    private String footerHubs;
    private String footerPressBookingUrl;
    private String footerApplyUrl;

    // ============================================================
    // 🔥 GETTERS EXPLICITOS (Lombok @Data NAO processa no mvnw 3.6.3 Render)
    // ============================================================
    public String getHeroTitle() { return heroTitle; }
    public String getHeroSubtitle() { return heroSubtitle; }
    public String getHeroDescription() { return heroDescription; }
    public String getScrollLabel() { return scrollLabel; }
    public String getMetaTitle() { return metaTitle; }
    public String getMetaDescription() { return metaDescription; }
    public String getVideoUrl() { return videoUrl; }
    public String getPosterUrl() { return posterUrl; }

    public String getFooterDescription() { return footerDescription; }
    public String getFooterHubs() { return footerHubs; }
    public String getFooterPressBookingUrl() { return footerPressBookingUrl; }
    public String getFooterApplyUrl() { return footerApplyUrl; }

    // ============================================================
    // 🔥 SETTERS EXPLICITOS
    // ============================================================
    public void setHeroTitle(String heroTitle) { this.heroTitle = heroTitle; }
    public void setHeroSubtitle(String heroSubtitle) { this.heroSubtitle = heroSubtitle; }
    public void setHeroDescription(String heroDescription) { this.heroDescription = heroDescription; }
    public void setScrollLabel(String scrollLabel) { this.scrollLabel = scrollLabel; }
    public void setMetaTitle(String metaTitle) { this.metaTitle = metaTitle; }
    public void setMetaDescription(String metaDescription) { this.metaDescription = metaDescription; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }
    public void setPosterUrl(String posterUrl) { this.posterUrl = posterUrl; }

    public void setFooterDescription(String footerDescription) { this.footerDescription = footerDescription; }
    public void setFooterHubs(String footerHubs) { this.footerHubs = footerHubs; }
    public void setFooterPressBookingUrl(String footerPressBookingUrl) { this.footerPressBookingUrl = footerPressBookingUrl; }
    public void setFooterApplyUrl(String footerApplyUrl) { this.footerApplyUrl = footerApplyUrl; }

    // ============================================================
    // 🔥 BUILDER MANUAL FALLBACK (evita erro cannot find symbol builder() no mvnw)
    // ============================================================
    public static HomeContentDtoBuilder manualBuilder() { return new HomeContentDtoBuilder(); }
    public static HomeContentDtoBuilder builder() { return new HomeContentDtoBuilder(); }

    public static class HomeContentDtoBuilder {
        private final HomeContentDto h = new HomeContentDto();
        public HomeContentDtoBuilder heroTitle(String v) { h.setHeroTitle(v); return this; }
        public HomeContentDtoBuilder heroSubtitle(String v) { h.setHeroSubtitle(v); return this; }
        public HomeContentDtoBuilder heroDescription(String v) { h.setHeroDescription(v); return this; }
        public HomeContentDtoBuilder scrollLabel(String v) { h.setScrollLabel(v); return this; }
        public HomeContentDtoBuilder metaTitle(String v) { h.setMetaTitle(v); return this; }
        public HomeContentDtoBuilder metaDescription(String v) { h.setMetaDescription(v); return this; }
        public HomeContentDtoBuilder videoUrl(String v) { h.setVideoUrl(v); return this; }
        public HomeContentDtoBuilder posterUrl(String v) { h.setPosterUrl(v); return this; }

        public HomeContentDtoBuilder footerDescription(String v) { h.setFooterDescription(v); return this; }
        public HomeContentDtoBuilder footerHubs(String v) { h.setFooterHubs(v); return this; }
        public HomeContentDtoBuilder footerPressBookingUrl(String v) { h.setFooterPressBookingUrl(v); return this; }
        public HomeContentDtoBuilder footerApplyUrl(String v) { h.setFooterApplyUrl(v); return this; }

        public HomeContentDto build() { return h; }
    }
}

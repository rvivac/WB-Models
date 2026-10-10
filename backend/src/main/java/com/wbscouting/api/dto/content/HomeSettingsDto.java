package com.wbscouting.api.dto.content;

import com.wbscouting.api.entity.HomeSettings;
import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HomeSettingsDto {

    private UUID id;
    private String heroTitle;
    private String heroSubtitle;
    private String heroDescription;
    private String bannerImageUrl;
    private String videoUrl;
    private String aboutPreview;
    private String metaTitle;
    private String metaDescription;
    private String scrollLabel;
    private String footerDescription;
    private String footerHubs;
    private String footerPressBookingUrl;
    private String footerApplyUrl;

    // Novo Disclaimer Opcional
    private Boolean disclaimerActive;
    private String disclaimerTitle;
    private String disclaimerText;
    private String disclaimerLinkUrl;
    private String disclaimerLinkLabel;

    // Explicit getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getHeroTitle() { return heroTitle; }
    public void setHeroTitle(String heroTitle) { this.heroTitle = heroTitle; }
    public String getHeroSubtitle() { return heroSubtitle; }
    public void setHeroSubtitle(String heroSubtitle) { this.heroSubtitle = heroSubtitle; }
    public String getHeroDescription() { return heroDescription; }
    public void setHeroDescription(String heroDescription) { this.heroDescription = heroDescription; }
    public String getBannerImageUrl() { return bannerImageUrl; }
    public void setBannerImageUrl(String bannerImageUrl) { this.bannerImageUrl = bannerImageUrl; }
    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }
    public String getAboutPreview() { return aboutPreview; }
    public void setAboutPreview(String aboutPreview) { this.aboutPreview = aboutPreview; }
    public String getMetaTitle() { return metaTitle; }
    public void setMetaTitle(String metaTitle) { this.metaTitle = metaTitle; }
    public String getMetaDescription() { return metaDescription; }
    public void setMetaDescription(String metaDescription) { this.metaDescription = metaDescription; }
    public String getScrollLabel() { return scrollLabel; }
    public void setScrollLabel(String scrollLabel) { this.scrollLabel = scrollLabel; }
    public String getFooterDescription() { return footerDescription; }
    public void setFooterDescription(String footerDescription) { this.footerDescription = footerDescription; }
    public String getFooterHubs() { return footerHubs; }
    public void setFooterHubs(String footerHubs) { this.footerHubs = footerHubs; }
    public String getFooterPressBookingUrl() { return footerPressBookingUrl; }
    public void setFooterPressBookingUrl(String footerPressBookingUrl) { this.footerPressBookingUrl = footerPressBookingUrl; }
    public String getFooterApplyUrl() { return footerApplyUrl; }
    public void setFooterApplyUrl(String footerApplyUrl) { this.footerApplyUrl = footerApplyUrl; }

    public Boolean getDisclaimerActive() { return disclaimerActive; }
    public void setDisclaimerActive(Boolean disclaimerActive) { this.disclaimerActive = disclaimerActive; }
    public String getDisclaimerTitle() { return disclaimerTitle; }
    public void setDisclaimerTitle(String disclaimerTitle) { this.disclaimerTitle = disclaimerTitle; }
    public String getDisclaimerText() { return disclaimerText; }
    public void setDisclaimerText(String disclaimerText) { this.disclaimerText = disclaimerText; }
    public String getDisclaimerLinkUrl() { return disclaimerLinkUrl; }
    public void setDisclaimerLinkUrl(String disclaimerLinkUrl) { this.disclaimerLinkUrl = disclaimerLinkUrl; }
    public String getDisclaimerLinkLabel() { return disclaimerLinkLabel; }
    public void setDisclaimerLinkLabel(String disclaimerLinkLabel) { this.disclaimerLinkLabel = disclaimerLinkLabel; }

    public static HomeSettingsDto fromEntity(HomeSettings entity) {
        if (entity == null) return null;
        return HomeSettingsDto.builder()
                .id(entity.getId())
                .heroTitle(entity.getHeroTitle())
                .heroSubtitle(entity.getHeroSubtitle())
                .heroDescription(entity.getHeroDescription())
                .bannerImageUrl(entity.getBannerImageUrl())
                .videoUrl(entity.getVideoUrl())
                .aboutPreview(entity.getAboutPreview())
                .metaTitle(entity.getMetaTitle())
                .metaDescription(entity.getMetaDescription())
                .scrollLabel(entity.getScrollLabel())
                .footerDescription(entity.getFooterDescription() != null ? entity.getFooterDescription() : "Agência de modelos e gestão internacional de talentos. Representação exclusiva, editorial e comercial com inteligência e inovação.")
                .footerHubs(entity.getFooterHubs() != null ? entity.getFooterHubs() : "PARIS • MILAN • NEW YORK • SÃO PAULO")
                .footerPressBookingUrl(entity.getFooterPressBookingUrl() != null ? entity.getFooterPressBookingUrl() : "/contato")
                .footerApplyUrl(entity.getFooterApplyUrl() != null ? entity.getFooterApplyUrl() : "/apply")
                .disclaimerActive(entity.getDisclaimerActive() != null ? entity.getDisclaimerActive() : false)
                .disclaimerTitle(entity.getDisclaimerTitle() != null ? entity.getDisclaimerTitle() : "")
                .disclaimerText(entity.getDisclaimerText() != null ? entity.getDisclaimerText() : "")
                .disclaimerLinkUrl(entity.getDisclaimerLinkUrl() != null ? entity.getDisclaimerLinkUrl() : "")
                .disclaimerLinkLabel(entity.getDisclaimerLinkLabel() != null ? entity.getDisclaimerLinkLabel() : "")
                .build();
    }
}

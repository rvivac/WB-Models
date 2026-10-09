package com.wbscouting.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "home_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HomeSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "hero_title", length = 255, nullable = true)
    private String heroTitle;

    @Column(name = "hero_subtitle", length = 255, nullable = true)
    private String heroSubtitle;

    @Column(name = "hero_description", columnDefinition = "TEXT", nullable = true)
    private String heroDescription;

    @Column(name = "banner_image_url", length = 1000, nullable = true)
    private String bannerImageUrl;

    @Column(name = "video_url", length = 1000, nullable = true)
    private String videoUrl;

    @Column(name = "about_preview", columnDefinition = "TEXT", nullable = true)
    private String aboutPreview;

    @Column(name = "meta_title", length = 255, nullable = true)
    private String metaTitle;

    @Column(name = "meta_description", columnDefinition = "TEXT", nullable = true)
    private String metaDescription;

    @Column(name = "scroll_label", length = 100, nullable = true)
    private String scrollLabel;

    @Column(name = "footer_description", columnDefinition = "TEXT", nullable = true)
    private String footerDescription;

    @Column(name = "footer_hubs", length = 255, nullable = true)
    private String footerHubs;

    @Column(name = "footer_press_booking_url", length = 255, nullable = true)
    private String footerPressBookingUrl;

    @Column(name = "footer_apply_url", length = 255, nullable = true)
    private String footerApplyUrl;

    @Column(name = "disclaimer_active", nullable = true)
    private Boolean disclaimerActive;

    @Column(name = "disclaimer_title", length = 255, nullable = true)
    private String disclaimerTitle;

    @Column(name = "disclaimer_text", columnDefinition = "TEXT", nullable = true)
    private String disclaimerText;

    @Column(name = "disclaimer_link_url", length = 1000, nullable = true)
    private String disclaimerLinkUrl;

    @Column(name = "disclaimer_link_label", length = 100, nullable = true)
    private String disclaimerLinkLabel;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    // Explicit getters and setters
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
}

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

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}

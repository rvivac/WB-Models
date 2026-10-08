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
                .build();
    }
}

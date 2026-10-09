package com.wbscouting.api.controller.admin;

import com.wbscouting.api.dto.content.HomeSettingsDto;
import com.wbscouting.api.entity.HomeSettings;
import com.wbscouting.api.entity.SiteContent;
import com.wbscouting.api.repository.HomeSettingsRepository;
import com.wbscouting.api.repository.SiteContentRepository;
import com.wbscouting.api.security.audit.AuditAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping({"/api/v1/admin/home-settings", "/admin/home-settings"})
@PreAuthorize("hasAnyRole('WEBMASTER', 'SUPER_ADMIN', 'CONTENT_ADMIN', 'ADMIN')")
@RequiredArgsConstructor
public class AdminHomeSettingsController {

    private final HomeSettingsRepository homeSettingsRepository;
    private final SiteContentRepository siteContentRepository;

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<HomeSettingsDto> getHomeSettings() {
        Optional<HomeSettings> opt = homeSettingsRepository.findFirstByOrderByUpdatedAtDesc();
        HomeSettings settings = opt.orElseGet(() -> HomeSettings.builder()
                .heroTitle("EDITORIAL & HIGH FASHION SCOUTING")
                .heroSubtitle("Desenvolvimento integral e conexão estratégica de talentos com os polos internacionais da moda.")
                .heroDescription("Núcleo editorial dedicado ao scouting, preparação técnica e posicionamento global de modelos.")
                .bannerImageUrl("")
                .videoUrl("")
                .aboutPreview("A WB Agency conecta novos talentos às maiores agências e semanas de moda de Paris, Milão e Nova York.")
                .metaTitle("WB Agency | Editorial & High Fashion Scouting")
                .metaDescription("Agência de scouting e gestão de carreira de modelos para o mercado internacional da moda.")
                .scrollLabel("Explore o Elenco")
                .footerDescription("Agência de modelos e gestão internacional de talentos. Representação exclusiva, editorial e comercial com inteligência e inovação.")
                .footerHubs("PARIS • MILAN • NEW YORK • SÃO PAULO")
                .footerPressBookingUrl("/contato")
                .footerApplyUrl("/apply")
                .updatedAt(OffsetDateTime.now())
                .build());
        return ResponseEntity.ok(HomeSettingsDto.fromEntity(settings));
    }

    @PutMapping
    @Transactional
    @AuditAction(action = "UPDATE", resource = "HOME_SETTINGS", description = "Atualização de configurações da página principal")
    public ResponseEntity<HomeSettingsDto> updateHomeSettings(@RequestBody HomeSettingsDto dto) {
        log.info("Atualizando configurações da Home via admin: heroTitle='{}'", dto.getHeroTitle());

        Optional<HomeSettings> opt = homeSettingsRepository.findFirstByOrderByUpdatedAtDesc();
        HomeSettings entity = opt.orElseGet(HomeSettings::new);

        entity.setHeroTitle(dto.getHeroTitle());
        entity.setHeroSubtitle(dto.getHeroSubtitle());
        entity.setHeroDescription(dto.getHeroDescription());
        entity.setBannerImageUrl(dto.getBannerImageUrl());
        entity.setVideoUrl(dto.getVideoUrl());
        entity.setAboutPreview(dto.getAboutPreview());
        entity.setMetaTitle(dto.getMetaTitle());
        entity.setMetaDescription(dto.getMetaDescription());
        entity.setScrollLabel(dto.getScrollLabel());
        entity.setFooterDescription(dto.getFooterDescription());
        entity.setFooterHubs(dto.getFooterHubs());
        entity.setFooterPressBookingUrl(dto.getFooterPressBookingUrl());
        entity.setFooterApplyUrl(dto.getFooterApplyUrl());
        entity.setDisclaimerActive(dto.getDisclaimerActive() != null ? dto.getDisclaimerActive() : false);
        entity.setDisclaimerTitle(dto.getDisclaimerTitle() != null ? dto.getDisclaimerTitle() : "");
        entity.setDisclaimerText(dto.getDisclaimerText() != null ? dto.getDisclaimerText() : "");
        entity.setDisclaimerLinkUrl(dto.getDisclaimerLinkUrl() != null ? dto.getDisclaimerLinkUrl() : "");
        entity.setDisclaimerLinkLabel(dto.getDisclaimerLinkLabel() != null ? dto.getDisclaimerLinkLabel() : "");
        entity.setSplashDurationMs(dto.getSplashDurationMs() != null ? dto.getSplashDurationMs() : 1000);
        entity.setUpdatedAt(OffsetDateTime.now());

        HomeSettings saved = homeSettingsRepository.saveAndFlush(entity);

        // Sincroniza com site_content ("home") para retrocompatibilidade
        try {
            SiteContent sc = siteContentRepository.findBySectionKey("home")
                    .orElseGet(() -> SiteContent.builder().sectionKey("home").build());
            Map<String, Object> map = new HashMap<>();
            map.put("heroTitle", dto.getHeroTitle());
            map.put("heroSubtitle", dto.getHeroSubtitle());
            map.put("heroDescription", dto.getHeroDescription());
            map.put("scrollLabel", dto.getScrollLabel());
            map.put("metaTitle", dto.getMetaTitle());
            map.put("metaDescription", dto.getMetaDescription());
            map.put("footerDescription", dto.getFooterDescription());
            map.put("footerHubs", dto.getFooterHubs());
            map.put("footerPressBookingUrl", dto.getFooterPressBookingUrl());
            map.put("footerApplyUrl", dto.getFooterApplyUrl());
            map.put("disclaimerActive", dto.getDisclaimerActive() != null ? dto.getDisclaimerActive() : false);
            map.put("disclaimerTitle", dto.getDisclaimerTitle() != null ? dto.getDisclaimerTitle() : "");
            map.put("disclaimerText", dto.getDisclaimerText() != null ? dto.getDisclaimerText() : "");
            map.put("disclaimerLinkUrl", dto.getDisclaimerLinkUrl() != null ? dto.getDisclaimerLinkUrl() : "");
            map.put("disclaimerLinkLabel", dto.getDisclaimerLinkLabel() != null ? dto.getDisclaimerLinkLabel() : "");
            map.put("splashDurationMs", dto.getSplashDurationMs() != null ? dto.getSplashDurationMs() : 1000);
            sc.setPayloadPt(map);
            sc.setPayloadEn(map);
            sc.setUpdatedAt(OffsetDateTime.now());
            siteContentRepository.saveAndFlush(sc);
        } catch (Exception e) {
            log.warn("Erro não impeditivo ao sincronizar site_content: {}", e.getMessage());
        }

        return ResponseEntity.ok(HomeSettingsDto.fromEntity(saved));
    }
}

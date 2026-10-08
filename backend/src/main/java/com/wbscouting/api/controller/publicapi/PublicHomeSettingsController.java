package com.wbscouting.api.controller.publicapi;

import com.wbscouting.api.dto.content.HomeSettingsDto;
import com.wbscouting.api.entity.HomeSettings;
import com.wbscouting.api.repository.HomeSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping({"/api/v1/home-settings", "/home-settings"})
@RequiredArgsConstructor
public class PublicHomeSettingsController {

    private final HomeSettingsRepository homeSettingsRepository;

    @GetMapping
    @Transactional
    public ResponseEntity<HomeSettingsDto> getHomeSettings() {
        Optional<HomeSettings> opt = homeSettingsRepository.findFirstByOrderByUpdatedAtDesc();

        HomeSettings settings;
        if (opt.isPresent()) {
            settings = opt.get();
        } else {
            log.info("Inicializando configurações padrão da Home no banco de dados...");
            settings = HomeSettings.builder()
                    .heroTitle("EDITORIAL & HIGH FASHION SCOUTING")
                    .heroSubtitle("Desenvolvimento integral e conexão estratégica de talentos com os polos internacionais da moda.")
                    .heroDescription("Núcleo editorial dedicado ao scouting, preparação técnica e posicionamento global de modelos.")
                    .bannerImageUrl("https://stmytwsdlonpnirqiufq.supabase.co/storage/v1/object/public/site-assets/hero-banner.webp")
                    .videoUrl("")
                    .aboutPreview("A WB Agency conecta novos talentos às maiores agências e semanas de moda de Paris, Milão e Nova York.")
                    .metaTitle("WB Agency | Editorial & High Fashion Scouting")
                    .metaDescription("Agência de scouting e gestão de carreira de modelos para o mercado internacional da moda.")
                    .scrollLabel("Explore o Elenco")
                    .updatedAt(OffsetDateTime.now())
                    .build();
            settings = homeSettingsRepository.saveAndFlush(settings);
        }

        return ResponseEntity.ok(HomeSettingsDto.fromEntity(settings));
    }
}

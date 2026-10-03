package com.wbscouting.api.controller.admin;

import com.wbscouting.api.config.SupabaseProperties;
import com.wbscouting.api.dto.content.HomeContentDto;
import com.wbscouting.api.entity.Admin;
import com.wbscouting.api.entity.SiteContent;
import com.wbscouting.api.exception.FileSizeExceededException;
import com.wbscouting.api.exception.InvalidFileException;
import com.wbscouting.api.repository.SiteContentRepository;
import com.wbscouting.api.service.storage.StorageService;
import com.wbscouting.api.service.storage.SupabaseStorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import com.wbscouting.api.security.audit.AuditAction;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Slf4j
@RestController
@RequestMapping({"/api/v1/admin/institutional/home", "/admin/institutional/home"})
@PreAuthorize("hasAnyRole('WEBMASTER', 'SUPER_ADMIN', 'CONTENT_ADMIN', 'ADMIN')")
@RequiredArgsConstructor
public class AdminInstitutionalHomeController {

    private static final String SECTION_HOME = "home";

    private final SiteContentRepository siteContentRepository;
    private final StorageService storageService;
    private final SupabaseProperties supabaseProperties;

    @GetMapping
    public ResponseEntity<HomeContentDto> getHomeContent() {
        log.info("Consultando configuração vigente da primeira dobra da Home (Hero)");

        SiteContent content = siteContentRepository.findBySectionKey(SECTION_HOME)
                .orElseGet(this::createDefaultHomeContent);

        return ResponseEntity.ok(toDto(content));
    }

    @PutMapping
    @AuditAction(action = "UPDATE", resource = "INSTITUTIONAL_HOME", description = "Atualização de textos e metadados da Home")
    public ResponseEntity<HomeContentDto> updateHomeContent(
            @Valid @RequestBody HomeContentDto dto,
            Authentication authentication
    ) {
        log.info("Atualizando textos institucionais e metadados SEO da Home");

        SiteContent content = siteContentRepository.findBySectionKey(SECTION_HOME)
                .orElseGet(this::createDefaultHomeContent);

        UUID adminId = extractAdminId(authentication);
        if (adminId != null) {
            content.setUpdatedBy(adminId);
        }

        Map<String, Object> payloadPt = content.getPayloadPt() != null ? new HashMap<>(content.getPayloadPt()) : new HashMap<>();
        payloadPt.put("heroTitle", dto.getHeroTitle());
        payloadPt.put("heroSubtitle", dto.getHeroSubtitle());
        payloadPt.put("heroDescription", dto.getHeroDescription());
        payloadPt.put("scrollLabel", dto.getScrollLabel());
        payloadPt.put("metaTitle", dto.getMetaTitle());
        payloadPt.put("metaDescription", dto.getMetaDescription());
        content.setPayloadPt(payloadPt);

        Map<String, Object> payloadEn = content.getPayloadEn() != null ? new HashMap<>(content.getPayloadEn()) : new HashMap<>();
        payloadEn.put("heroTitle", dto.getHeroTitle());
        payloadEn.put("heroSubtitle", dto.getHeroSubtitle());
        payloadEn.put("heroDescription", dto.getHeroDescription());
        payloadEn.put("scrollLabel", dto.getScrollLabel());
        payloadEn.put("metaTitle", dto.getMetaTitle());
        payloadEn.put("metaDescription", dto.getMetaDescription());
        content.setPayloadEn(payloadEn);

        SiteContent saved = siteContentRepository.save(content);
        return ResponseEntity.ok(toDto(saved));
    }

    @PutMapping(value = "/video", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadHeroVideo(@RequestParam("file") MultipartFile file) {
        log.info("Recebendo upload de vídeo hero da Home. Arquivo: {}, Tamanho: {} bytes",
                file.getOriginalFilename(), file.getSize());

        validateFile(file, SupabaseStorageService.MAX_SIZE_SITE_ASSETS, SupabaseStorageService.ALLOWED_VIDEO_TYPES, "vídeo");

        String bucket = resolveBucketName();
        String extension = getFileExtension(file.getOriginalFilename(), ".mp4");
        String path = "institutional/home-hero" + extension;

        String uploadedPath = storageService.uploadFile(bucket, path, file);
        String publicUrl = storageService.getPublicUrl(bucket, uploadedPath);

        SiteContent content = siteContentRepository.findBySectionKey(SECTION_HOME)
                .orElseGet(this::createDefaultHomeContent);

        Map<String, Object> mediaUrls = content.getMediaUrls() != null ? new HashMap<>(content.getMediaUrls()) : new HashMap<>();
        mediaUrls.put("videoUrl", publicUrl);
        content.setMediaUrls(mediaUrls);
        siteContentRepository.save(content);

        return ResponseEntity.ok(Map.of("videoUrl", publicUrl));
    }

    @PutMapping(value = "/poster", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadHeroPoster(@RequestParam("file") MultipartFile file) {
        log.info("Recebendo upload de imagem poster fallback da Home. Arquivo: {}, Tamanho: {} bytes",
                file.getOriginalFilename(), file.getSize());

        validateFile(file, SupabaseStorageService.MAX_SIZE_SITE_ASSETS, SupabaseStorageService.ALLOWED_IMAGE_TYPES, "imagem de capa");

        String bucket = resolveBucketName();
        String extension = getFileExtension(file.getOriginalFilename(), ".webp");
        String path = "institutional/home-poster" + extension;

        String uploadedPath = storageService.uploadFile(bucket, path, file);
        String publicUrl = storageService.getPublicUrl(bucket, uploadedPath);

        SiteContent content = siteContentRepository.findBySectionKey(SECTION_HOME)
                .orElseGet(this::createDefaultHomeContent);

        Map<String, Object> mediaUrls = content.getMediaUrls() != null ? new HashMap<>(content.getMediaUrls()) : new HashMap<>();
        mediaUrls.put("posterUrl", publicUrl);
        content.setMediaUrls(mediaUrls);
        siteContentRepository.save(content);

        return ResponseEntity.ok(Map.of("posterUrl", publicUrl));
    }

    private HomeContentDto toDto(SiteContent content) {
        Map<String, Object> pt = content.getPayloadPt() != null ? content.getPayloadPt() : Collections.emptyMap();
        Map<String, Object> media = content.getMediaUrls() != null ? content.getMediaUrls() : Collections.emptyMap();

        return HomeContentDto.builder()
                .heroTitle(getString(pt, "heroTitle", "WB AGENCY"))
                .heroSubtitle(getString(pt, "heroSubtitle", "EDITORIAL & HIGH FASHION SCOUTING"))
                .heroDescription(getString(pt, "heroDescription", "Representação exclusiva, desenvolvimento de talentos e curadoria estética conectada aos maiores mercados globais."))
                .scrollLabel(getString(pt, "scrollLabel", "SCROLL"))
                .metaTitle(getString(pt, "metaTitle", "WB Agency | Scouting Internacional e Alta Moda"))
                .metaDescription(getString(pt, "metaDescription", "Agência de scouting e modelos com foco editorial."))
                .videoUrl(getString(media, "videoUrl", "assets/videos/wb-presentation.mp4"))
                .posterUrl(getString(media, "posterUrl", "assets/images/logo-wb-agency.jpeg"))
                .build();
    }

    private SiteContent createDefaultHomeContent() {
        Map<String, Object> payloadPt = new HashMap<>();
        payloadPt.put("heroTitle", "WB AGENCY");
        payloadPt.put("heroSubtitle", "EDITORIAL & HIGH FASHION SCOUTING");
        payloadPt.put("heroDescription", "Representação exclusiva, desenvolvimento de talentos e curadoria estética conectada aos maiores mercados globais.");
        payloadPt.put("scrollLabel", "SCROLL");
        payloadPt.put("metaTitle", "WB Agency | Scouting Internacional e Alta Moda");
        payloadPt.put("metaDescription", "Agência de scouting e modelos com foco editorial.");

        Map<String, Object> mediaUrls = new HashMap<>();
        mediaUrls.put("videoUrl", "assets/videos/wb-presentation.mp4");
        mediaUrls.put("posterUrl", "assets/images/logo-wb-agency.jpeg");

        SiteContent content = SiteContent.builder()
                .sectionKey(SECTION_HOME)
                .payloadPt(payloadPt)
                .payloadEn(payloadPt)
                .mediaUrls(mediaUrls)
                .build();

        return siteContentRepository.save(content);
    }

    private void validateFile(MultipartFile file, long maxSize, Set<String> allowedTypes, String fileTypeLabel) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("O arquivo de " + fileTypeLabel + " é obrigatório e não pode estar vazio.");
        }
        if (file.getSize() > maxSize) {
            double mbSent = file.getSize() / (1024.0 * 1024.0);
            double mbMax = maxSize / (1024.0 * 1024.0);
            throw new FileSizeExceededException(String.format("O arquivo excede o limite máximo permitido de %.0f MB. Tamanho enviado: %.2f MB", mbMax, mbSent));
        }
        String contentType = file.getContentType();
        if (contentType == null || !allowedTypes.contains(contentType.toLowerCase())) {
            throw new InvalidFileException(String.format("Tipo de arquivo '%s' não suportado para %s. Permitidos: %s",
                    contentType, fileTypeLabel, allowedTypes));
        }
    }

    private String getFileExtension(String filename, String defaultExt) {
        if (!StringUtils.hasText(filename)) return defaultExt;
        int lastDot = filename.lastIndexOf('.');
        if (lastDot >= 0 && lastDot < filename.length() - 1) {
            return filename.substring(lastDot);
        }
        return defaultExt;
    }

    private String resolveBucketName() {
        return supabaseProperties.resolveBucketSiteAssets();
    }

    private String getString(Map<String, Object> map, String key, String defaultValue) {
        Object val = map.get(key);
        return val != null ? val.toString() : defaultValue;
    }

    private UUID extractAdminId(Authentication authentication) {
        Authentication auth = authentication != null ? authentication : SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Admin admin) {
            return admin.getId();
        }
        return null;
    }
}

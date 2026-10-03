package com.wbscouting.api.service.content;

import com.wbscouting.api.config.SupabaseProperties;
import com.wbscouting.api.dto.content.AssetUploadResponseDto;
import com.wbscouting.api.dto.content.SiteContentAdminDto;
import com.wbscouting.api.dto.content.SiteContentPublicDto;
import com.wbscouting.api.dto.content.SiteContentUpdateRequestDto;
import com.wbscouting.api.entity.SiteContent;
import com.wbscouting.api.exception.FileSizeExceededException;
import com.wbscouting.api.exception.InvalidFileException;
import com.wbscouting.api.exception.ResourceNotFoundException;
import com.wbscouting.api.repository.SiteContentRepository;
import com.wbscouting.api.service.storage.StorageService;
import com.wbscouting.api.service.storage.SupabaseStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SiteContentServiceImpl implements SiteContentService {

    private final SiteContentRepository siteContentRepository;
    private final StorageService storageService;
    private final SupabaseProperties supabaseProperties;

    @Override
    @Transactional(readOnly = true)
    public SiteContentPublicDto getPublicContent(String sectionKey, String lang) {
        log.info("Consultando conteúdo público para sectionKey='{}', lang='{}'", sectionKey, lang);

        SiteContent content = siteContentRepository.findBySectionKey(sectionKey)
                .orElseGet(() -> {
                    if ("ABOUT_MANIFESTO".equalsIgnoreCase(sectionKey)) {
                        return createDefaultAboutManifesto();
                    }
                    throw new ResourceNotFoundException("Conteúdo da seção não encontrado: " + sectionKey);
                });

        String resolvedLang = resolveLanguage(lang);
        Map<String, Object> resolvedPayload = resolvePayloadByLanguage(content, resolvedLang);

        return SiteContentPublicDto.builder()
                .sectionKey(content.getSectionKey())
                .payload(resolvedPayload)
                .mediaUrls(content.getMediaUrls())
                .lang(resolvedLang)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Map<String, Object>> getAllPublicContent(String lang) {
        String resolvedLang = resolveLanguage(lang);
        log.info("Consultando mapa consolidado de conteúdos públicos para lang='{}'", resolvedLang);

        List<SiteContent> contents = siteContentRepository.findAll();
        Map<String, Map<String, Object>> consolidated = new LinkedHashMap<>();

        for (SiteContent content : contents) {
            Map<String, Object> payload = resolvePayloadByLanguage(content, resolvedLang);
            consolidated.put(content.getSectionKey(), payload != null ? payload : Collections.emptyMap());
        }

        return consolidated;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SiteContentAdminDto> getAllAdminContent() {
        log.info("Listando todas as seções e payloads de conteúdos para painel CMS");

        return siteContentRepository.findAll().stream()
                .map(this::toAdminDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SiteContentAdminDto updateContent(String sectionKey, SiteContentUpdateRequestDto dto, UUID adminId) {
        log.info("Executando upsert de conteúdo institucional para sectionKey='{}', adminId={}", sectionKey, adminId);

        SiteContent content = siteContentRepository.findBySectionKey(sectionKey)
                .orElseGet(() -> SiteContent.builder()
                        .sectionKey(sectionKey)
                        .build());

        content.setPayloadPt(dto.getPayloadPt());
        content.setPayloadEn(dto.getPayloadEn());
        if (dto.getMediaUrls() != null) {
            content.setMediaUrls(dto.getMediaUrls());
        }
        content.setUpdatedBy(adminId);

        SiteContent saved = siteContentRepository.save(content);
        log.info("Conteúdo institucional da seção '{}' atualizado com sucesso.", sectionKey);

        return toAdminDto(saved);
    }

    @Override
    public AssetUploadResponseDto uploadAsset(MultipartFile file, String folder) {
        log.info("Iniciando upload de ativo institucional. Nome='{}', Pasta='{}'",
                file != null ? file.getOriginalFilename() : "null", folder);

        String bucket = resolveBucketName();
        validateAssetFile(file, bucket);

        String targetFolder = StringUtils.hasText(folder) ? folder.trim() : "assets";
        String sanitizedFilename = sanitizeFilename(file.getOriginalFilename());
        String storagePath = String.format("%s/%s-%s", targetFolder, UUID.randomUUID(), sanitizedFilename);

        storageService.uploadFile(bucket, storagePath, file);
        String publicUrl = storageService.getPublicUrl(bucket, storagePath);

        log.info("Upload de ativo institucional concluído com sucesso: URL={}", publicUrl);

        return AssetUploadResponseDto.builder()
                .fileUrl(publicUrl)
                .storagePath(storagePath)
                .fileType(file.getContentType())
                .fileSizeBytes(file.getSize())
                .build();
    }

    private String resolveLanguage(String lang) {
        if (StringUtils.hasText(lang) && "en".equalsIgnoreCase(lang.trim())) {
            return "en";
        }
        return "pt";
    }

    private Map<String, Object> resolvePayloadByLanguage(SiteContent content, String lang) {
        if ("en".equalsIgnoreCase(lang)) {
            if (content.getPayloadEn() != null && !content.getPayloadEn().isEmpty()) {
                return content.getPayloadEn();
            }
            // Fallback para PT
            return content.getPayloadPt();
        }

        // Padrão PT
        if (content.getPayloadPt() != null && !content.getPayloadPt().isEmpty()) {
            return content.getPayloadPt();
        }
        return content.getPayloadEn();
    }

    private SiteContentAdminDto toAdminDto(SiteContent content) {
        return SiteContentAdminDto.builder()
                .id(content.getId())
                .sectionKey(content.getSectionKey())
                .payloadPt(content.getPayloadPt())
                .payloadEn(content.getPayloadEn())
                .mediaUrls(content.getMediaUrls())
                .createdAt(content.getCreatedAt())
                .updatedAt(content.getUpdatedAt())
                .updatedBy(content.getUpdatedBy())
                .build();
    }

    private String sanitizeFilename(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "file";
        }
        return filename.trim().replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String resolveBucketName() {
        return supabaseProperties.resolveBucketSiteAssets();
    }

    private void validateAssetFile(MultipartFile file, String bucket) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("O arquivo para upload é obrigatório e não pode estar vazio.");
        }
        long maxSize = SupabaseStorageService.MAX_SIZE_SITE_ASSETS;
        if (file.getSize() > maxSize) {
            long maxMb = maxSize / (1024 * 1024);
            throw new FileSizeExceededException(
                    String.format("O arquivo enviado (%.2f MB) excede o limite máximo permitido de %d MB para o bucket '%s'.",
                            file.getSize() / (1024.0 * 1024.0), maxMb, bucket));
        }
        String contentType = file.getContentType();
        boolean isImage = SupabaseStorageService.ALLOWED_IMAGE_TYPES.contains(contentType);
        boolean isVideo = SupabaseStorageService.ALLOWED_VIDEO_TYPES.contains(contentType);
        if (!isImage && !isVideo) {
            throw new InvalidFileException(
                    String.format("Formato de arquivo '%s' não suportado para o bucket '%s'. Tipos permitidos: imagens (%s) e vídeos (%s).",
                            contentType, bucket,
                            String.join(", ", SupabaseStorageService.ALLOWED_IMAGE_TYPES),
                            String.join(", ", SupabaseStorageService.ALLOWED_VIDEO_TYPES)));
        }
    }

    private SiteContent createDefaultAboutManifesto() {
        Map<String, Object> pt = new HashMap<>();
        pt.put("headline", "A Nova Estética do Scouting Global");
        pt.put("quote", "A beleza contemporânea nasce da singularidade e precisão.");
        pt.put("body", "A WB Agency consolidou-se como um núcleo editorial focado no desenvolvimento integral de modelos para os principais mercados da moda internacional. Nossa metodologia rejeita a padronização e prioriza a identidade visual autêntica, conectando talentos a marcas com relevância estética global.");

        Map<String, Object> en = new HashMap<>();
        en.put("headline", "The New Aesthetic of Global Scouting");
        en.put("quote", "Contemporary beauty stems from uniqueness and precision.");
        en.put("body", "WB Agency has established itself as an editorial powerhouse dedicated to the comprehensive development of models for premier global fashion markets. Our scouting methodology moves beyond mass standards to foster authentic personal identity, positioning talents at the intersection of high fashion and international relevance.");

        return SiteContent.builder()
                .sectionKey("ABOUT_MANIFESTO")
                .payloadPt(pt)
                .payloadEn(en)
                .build();
    }
}

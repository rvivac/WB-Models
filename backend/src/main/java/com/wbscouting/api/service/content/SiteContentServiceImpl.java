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

    public static final String DEFAULT_BUCKET_SITE_ASSETS = "site-assets";
    public static final long MAX_IMAGE_SIZE_BYTES = 10L * 1024 * 1024; // 10 MB
    public static final long MAX_VIDEO_SIZE_BYTES = 25L * 1024 * 1024; // 25 MB

    public static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    public static final Set<String> ALLOWED_VIDEO_TYPES = Set.of(
            "video/mp4",
            "video/webm"
    );

    private final SiteContentRepository siteContentRepository;
    private final StorageService storageService;
    private final SupabaseProperties supabaseProperties;

    @Override
    @Transactional(readOnly = true)
    public SiteContentPublicDto getPublicContent(String sectionKey, String lang) {
        log.info("Consultando conteúdo público para sectionKey='{}', lang='{}'", sectionKey, lang);

        SiteContent content = siteContentRepository.findBySectionKey(sectionKey)
                .orElseThrow(() -> new ResourceNotFoundException("Conteúdo da seção não encontrado: " + sectionKey));

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

        validateAssetFile(file);

        String bucket = resolveBucketName();
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

    private void validateAssetFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("O arquivo enviado não pode ser nulo ou vazio.");
        }

        String rawContentType = file.getContentType();
        if (!StringUtils.hasText(rawContentType)) {
            throw new InvalidFileException("O tipo de conteúdo (Content-Type) do arquivo não foi identificado.");
        }

        String contentType = rawContentType.toLowerCase().trim();
        long fileSize = file.getSize();

        if (ALLOWED_IMAGE_TYPES.contains(contentType)) {
            if (fileSize > MAX_IMAGE_SIZE_BYTES) {
                throw new FileSizeExceededException(String.format(
                        "O arquivo de imagem excede o limite máximo permitido de 10 MB. Tamanho enviado: %.2f MB",
                        fileSize / (1024.0 * 1024.0)));
            }
        } else if (ALLOWED_VIDEO_TYPES.contains(contentType)) {
            if (fileSize > MAX_VIDEO_SIZE_BYTES) {
                throw new FileSizeExceededException(String.format(
                        "O arquivo de vídeo excede o limite máximo permitido de 25 MB. Tamanho enviado: %.2f MB",
                        fileSize / (1024.0 * 1024.0)));
            }
        } else {
            throw new InvalidFileException(String.format(
                    "Tipo de arquivo '%s' não suportado para o bucket 'site-assets'. Permitidos: JPEG, PNG, WEBP, MP4, WEBM.",
                    contentType));
        }
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
        if (supabaseProperties != null && supabaseProperties.getBuckets() != null
                && StringUtils.hasText(supabaseProperties.getBuckets().getSiteAssets())) {
            return supabaseProperties.getBuckets().getSiteAssets();
        }
        return DEFAULT_BUCKET_SITE_ASSETS;
    }
}

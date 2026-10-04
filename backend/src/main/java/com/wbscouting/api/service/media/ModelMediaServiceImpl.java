package com.wbscouting.api.service.media;

import com.wbscouting.api.config.SupabaseProperties;
import com.wbscouting.api.dto.media.MediaOrderItemDto;
import com.wbscouting.api.dto.media.MediaReorderRequestDto;
import com.wbscouting.api.dto.media.MediaUploadResponseDto;
import com.wbscouting.api.entity.Model;
import com.wbscouting.api.entity.ModelMedia;
import com.wbscouting.api.enums.MediaType;
import com.wbscouting.api.exception.ResourceNotFoundException;
import com.wbscouting.api.repository.ModelMediaRepository;
import com.wbscouting.api.repository.ModelRepository;
import com.wbscouting.api.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModelMediaServiceImpl implements ModelMediaService {

    private final ModelRepository modelRepository;
    private final ModelMediaRepository modelMediaRepository;
    private final StorageService storageService;
    private final SupabaseProperties supabaseProperties;

    @Override
    @Transactional
    public MediaUploadResponseDto uploadMedia(UUID modelId, MediaType mediaType, boolean isCover, MultipartFile file) {
        log.info("Iniciando upload de mídia para modelId='{}', mediaType='{}', isCover='{}'", modelId, mediaType, isCover);

        Model model = modelRepository.findById(modelId)
                .orElseThrow(() -> new ResourceNotFoundException("Modelo", "id", modelId));

        if (isCover && mediaType != MediaType.BOOK) {
            throw new IllegalArgumentException("Apenas mídias do tipo BOOK podem ser marcadas como capa.");
        }

        String bucket = supabaseProperties.resolveBucketModelsMedia();

        // Se a nova mídia for do tipo COMPOSITE, substitui o composite anterior se houver
        if (mediaType == MediaType.COMPOSITE) {
            Optional<ModelMedia> existingComposite = modelMediaRepository.findByModelIdAndMediaTypeAndIsActiveTrue(modelId, MediaType.COMPOSITE);
            if (existingComposite.isPresent()) {
                ModelMedia oldComp = existingComposite.get();
                try {
                    storageService.deleteFile(bucket, oldComp.getFilePath());
                } catch (Exception e) {
                    log.warn("Falha ao remover arquivo do composite anterior do storage: {}", e.getMessage());
                }
                modelMediaRepository.delete(oldComp);
                modelMediaRepository.flush();
            }
        }

        // Se marcada como capa, reseta a capa atual atomicamente
        if (isCover) {
            modelMediaRepository.findByModelIdAndIsCoverTrue(modelId).ifPresent(oldCover -> {
                oldCover.setIsCover(false);
                modelMediaRepository.save(oldCover);
            });
        }

        // Cálculo da próxima ordem de exibição
        Integer nextOrder = modelMediaRepository.findNextDisplayOrder(modelId, mediaType);
        if (nextOrder == null) {
            nextOrder = 1;
        }

        // Padrão de caminho: {modelId}/{mediaType.toLowerCase()}/{uuid}-{sanitized-original-filename}
        String sanitizedFilename = sanitizeFilename(file != null ? file.getOriginalFilename() : null);
        String storagePath = String.format("%s/%s/%s-%s",
                modelId,
                mediaType.name().toLowerCase(),
                UUID.randomUUID(),
                sanitizedFilename);

        // Upload físico no bucket Supabase Storage
        String uploadedPath = storageService.uploadFile(bucket, storagePath, file);
        String rawPublicUrl = storageService.getPublicUrl(bucket, uploadedPath);

        // ============================================================
        // PROTECAO INCONDICIONAL: GARANTE QUE A URL SALVA NO BANCO É VÁLIDA!
        // Se o storage retornar URL incompleta (ex: termina com "/public/models-media/" sem path)
        // o helper resolvePublicUrlFromFields remonta a URL correta a partir de bucket + uploadedPath.
        // ============================================================
        String safePublicUrl = storageService.resolvePublicUrlFromFields(bucket, uploadedPath, rawPublicUrl);
        if (!java.util.Objects.equals(rawPublicUrl, safePublicUrl)) {
            log.warn("[uploadMedia] Fallback URL ativado. modelId={}, mediaType={}, rawURL={}, safeURL={}",
                    modelId, mediaType,
                    org.springframework.util.StringUtils.truncate(rawPublicUrl == null ? "null" : rawPublicUrl, 80),
                    org.springframework.util.StringUtils.truncate(safePublicUrl == null ? "null" : safePublicUrl, 120));
        }

        // Bust cache -> garante que a foto NOVA apareça INSTANTANEAMENTE no site público,
        // evitando cache HTTP 404 do browser/Supabase por 1h. (vence a cada hora)
        String bustCache = "?v=" + (System.currentTimeMillis() / 3_600_000L);
        String displayUrl = safePublicUrl + bustCache;

        // Persistência do registro JPA (grava URL SEGURA, garantida válida)
        ModelMedia media = ModelMedia.builder()
                .model(model)
                .mediaType(mediaType)
                .fileUrl(safePublicUrl)
                .filePath(uploadedPath)
                .displayOrder(nextOrder)
                .isCover(isCover)
                .isActive(true)
                .build();

        ModelMedia savedMedia = modelMediaRepository.save(media);

        // Se for capa, sincroniza a foto principal do modelo (também com URL segura + bust cache)
        if (isCover) {
            model.setPrimaryPhotoUrl(displayUrl);
            modelRepository.save(model);
        }

        log.info("Mídia cadastrada com sucesso: id='{}', modelId='{}', url='{}'", savedMedia.getId(), modelId, safePublicUrl);
        return toDto(savedMedia);
    }

    @Override
    @Transactional
    public void deleteMedia(UUID modelId, UUID mediaId) {
        log.info("Iniciando exclusão de mídia id='{}' do modelo id='{}'", mediaId, modelId);

        ModelMedia media = modelMediaRepository.findByIdAndModelId(mediaId, modelId)
                .orElseThrow(() -> new ResourceNotFoundException("Mídia", "id", mediaId));

        String bucket = supabaseProperties.resolveBucketModelsMedia();
        String filePath = media.getFilePath();

        // 1. Excluir arquivo físico no Supabase Storage
        storageService.deleteFile(bucket, filePath);

        boolean wasCover = Boolean.TRUE.equals(media.getIsCover());

        // 2. Deletar registro na tabela model_media
        modelMediaRepository.delete(media);
        modelMediaRepository.flush();

        // 3. Se a mídia deletada for a capa (is_cover = true), promove a mais antiga remanescente do tipo BOOK
        if (wasCover) {
            Model model = media.getModel();
            Optional<ModelMedia> oldestBookOpt = modelMediaRepository.findFirstByModelIdAndMediaTypeOrderByCreatedAtAsc(modelId, MediaType.BOOK);
            if (oldestBookOpt.isPresent()) {
                ModelMedia promotedCover = oldestBookOpt.get();
                promotedCover.setIsCover(true);
                modelMediaRepository.save(promotedCover);
                model.setPrimaryPhotoUrl(promotedCover.getFileUrl());
                log.info("Mídia id='{}' promovida automaticamente a nova capa do modelo id='{}'", promotedCover.getId(), modelId);
            } else {
                model.setPrimaryPhotoUrl(null);
                log.info("Nenhuma mídia BOOK remanescente para capa do modelo id='{}'", modelId);
            }
            modelRepository.save(model);
        }

        log.info("Mídia id='{}' excluída com sucesso.", mediaId);
    }

    @Override
    @Transactional
    public void reorderMedia(UUID modelId, MediaReorderRequestDto reorderDto) {
        log.info("Reordenando mídias do modelo id='{}' com {} itens", modelId,
                reorderDto != null && reorderDto.getItems() != null ? reorderDto.getItems().size() : 0);

        if (!modelRepository.existsById(modelId)) {
            throw new ResourceNotFoundException("Modelo", "id", modelId);
        }

        if (reorderDto == null || reorderDto.getItems() == null || reorderDto.getItems().isEmpty()) {
            return;
        }

        for (MediaOrderItemDto item : reorderDto.getItems()) {
            ModelMedia media = modelMediaRepository.findByIdAndModelId(item.getMediaId(), modelId)
                    .orElseThrow(() -> new ResourceNotFoundException("Mídia", "id", item.getMediaId()));
            media.setDisplayOrder(item.getDisplayOrder());
            modelMediaRepository.save(media);
        }

        log.info("Reordenação de mídias concluída com sucesso para o modelo id='{}'", modelId);
    }

    @Override
    @Transactional
    public void setCoverMedia(UUID modelId, UUID mediaId) {
        log.info("Definindo mídia id='{}' como capa do modelo id='{}'", mediaId, modelId);

        Model model = modelRepository.findById(modelId)
                .orElseThrow(() -> new ResourceNotFoundException("Modelo", "id", modelId));

        ModelMedia targetMedia = modelMediaRepository.findByIdAndModelId(mediaId, modelId)
                .orElseThrow(() -> new ResourceNotFoundException("Mídia", "id", mediaId));

        if (targetMedia.getMediaType() != MediaType.BOOK) {
            throw new IllegalArgumentException("Apenas mídias do tipo BOOK podem ser marcadas como capa.");
        }

        // Desmarca a capa anterior se houver
        modelMediaRepository.findByModelIdAndIsCoverTrue(modelId).ifPresent(oldCover -> {
            if (!oldCover.getId().equals(mediaId)) {
                oldCover.setIsCover(false);
                modelMediaRepository.save(oldCover);
            }
        });

        // Define a nova capa
        targetMedia.setIsCover(true);
        modelMediaRepository.save(targetMedia);

        // Atualiza a foto principal do modelo
        model.setPrimaryPhotoUrl(targetMedia.getFileUrl());
        modelRepository.save(model);

        log.info("Mídia id='{}' definida como capa com sucesso para o modelo id='{}'", mediaId, modelId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MediaUploadResponseDto> listModelMedia(UUID modelId) {
        if (!modelRepository.existsById(modelId)) {
            throw new ResourceNotFoundException("Modelo", "id", modelId);
        }

        return modelMediaRepository.findByModelIdOrderByMediaTypeAscDisplayOrderAsc(modelId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "image.jpg";
        }
        String name = filename.replace("\\", "/");
        int lastSlash = name.lastIndexOf('/');
        if (lastSlash >= 0) {
            name = name.substring(lastSlash + 1);
        }
        String sanitized = name.trim().replaceAll("[^a-zA-Z0-9._-]", "-").replaceAll("-+", "-");
        return sanitized.isEmpty() ? "image.jpg" : sanitized;
    }

    @Override
    @Transactional(readOnly = true)
    public com.wbscouting.api.dto.media.ModelCompositeResponseDto getComposite(UUID modelId) {
        if (!modelRepository.existsById(modelId)) {
            throw new ResourceNotFoundException("Modelo", "id", modelId);
        }

        return modelMediaRepository.findByModelIdAndMediaTypeAndIsActiveTrue(modelId, MediaType.COMPOSITE)
                .map(comp -> toCompositeDto(comp, null, null))
                .orElse(null);
    }

    @Override
    @Transactional
    public com.wbscouting.api.dto.media.ModelCompositeResponseDto uploadOrReplaceComposite(UUID modelId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("O arquivo do composite não pode estar vazio.");
        }
        if (file.getSize() > 25 * 1024 * 1024) {
            throw new IllegalArgumentException("O arquivo do composite excede o limite máximo permitido de 25 MB.");
        }

        String contentType = file.getContentType();
        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        boolean isPdf = (contentType != null && contentType.equalsIgnoreCase("application/pdf")) || originalFilename.endsWith(".pdf");
        boolean isImage = (contentType != null && (contentType.equalsIgnoreCase("image/jpeg") || contentType.equalsIgnoreCase("image/png") || contentType.equalsIgnoreCase("image/webp")))
                || originalFilename.endsWith(".jpg") || originalFilename.endsWith(".jpeg") || originalFilename.endsWith(".png") || originalFilename.endsWith(".webp");

        if (!isPdf && !isImage) {
            throw new IllegalArgumentException("Formato inválido. Os formatos permitidos para o composite são: PDF, JPG, PNG e WEBP.");
        }

        Model model = modelRepository.findById(modelId)
                .orElseThrow(() -> new ResourceNotFoundException("Modelo", "id", modelId));

        String bucket = supabaseProperties.resolveBucketModelsMedia();

        // Substituição atômica: remove composite anterior do storage e banco se houver
        Optional<ModelMedia> existingComposite = modelMediaRepository.findByModelIdAndMediaTypeAndIsActiveTrue(modelId, MediaType.COMPOSITE);
        if (existingComposite.isPresent()) {
            ModelMedia oldComp = existingComposite.get();
            try {
                storageService.deleteFile(bucket, oldComp.getFilePath());
            } catch (Exception e) {
                log.warn("Falha ao remover arquivo do composite anterior do storage: {}", e.getMessage());
            }
            modelMediaRepository.delete(oldComp);
            modelMediaRepository.flush();
        }

        String originalName = sanitizeFilename(file.getOriginalFilename());
        String filename = "models/" + modelId + "/composite/" + UUID.randomUUID() + "-" + originalName;
        String publicUrl = storageService.uploadFile(bucket, filename, file);

        ModelMedia media = ModelMedia.builder()
                .model(model)
                .mediaType(MediaType.COMPOSITE)
                .fileUrl(publicUrl)
                .filePath(filename)
                .displayOrder(1)
                .isCover(false)
                .isActive(true)
                .build();

        ModelMedia saved = modelMediaRepository.save(media);
        return toCompositeDto(saved, originalName, file.getSize());
    }

    @Override
    @Transactional
    public void deleteComposite(UUID modelId) {
        if (!modelRepository.existsById(modelId)) {
            throw new ResourceNotFoundException("Modelo", "id", modelId);
        }

        modelMediaRepository.findByModelIdAndMediaTypeAndIsActiveTrue(modelId, MediaType.COMPOSITE)
                .ifPresent(comp -> {
                    try {
                        storageService.deleteFile(supabaseProperties.getBuckets().getModelsMedia(), comp.getFilePath());
                    } catch (Exception e) {
                        log.warn("Falha ao remover arquivo do composite do storage: {}", e.getMessage());
                    }
                    modelMediaRepository.delete(comp);
                });
    }

    private com.wbscouting.api.dto.media.ModelCompositeResponseDto toCompositeDto(ModelMedia media, String originalFilename, Long sizeBytes) {
        String name = originalFilename;
        if (name == null || name.isBlank()) {
            name = media.getFilePath() != null
                    ? media.getFilePath().substring(media.getFilePath().lastIndexOf('/') + 1)
                    : "composite";
            if (name.matches("^[0-9a-fA-F\\-]{36}-.+")) {
                name = name.substring(37);
            }
        }
        String type = name.toLowerCase().endsWith(".pdf") ? "PDF" : "IMAGE";
        return com.wbscouting.api.dto.media.ModelCompositeResponseDto.builder()
                .id(media.getId())
                .fileUrl(media.getFileUrl())
                .fileName(name)
                .fileType(type)
                .fileSizeBytes(sizeBytes != null ? sizeBytes : 0L)
                .updatedAt(media.getUpdatedAt() != null ? media.getUpdatedAt() : media.getCreatedAt())
                .build();
    }

    private MediaUploadResponseDto toDto(ModelMedia media) {
        // ============================================================
        // PROTECAO CONSISTENCIA: ADMIN === PUBLICO
        // Perfil publico usava storageService.resolvePublicUrlFromFields
        // para remontar URLs incompletas.
        // Admin NAO usava. Por isso foto nova Eve aparecia no publico
        // e NAO aparecia no painel.
        // Aplicamos exatamente a mesma logica aqui.
        // ============================================================
        String bucket = supabaseProperties.resolveBucketModelsMedia();
        String rawUrl = media.getFileUrl();
        String filePath = media.getFilePath();
        String safeUrl = storageService.resolvePublicUrlFromFields(bucket, filePath, rawUrl);

        // Bust cache de 1 hora (forca navegador carregar midia nova, evita HTTP 404 cache antigo)
        if (safeUrl != null && !safeUrl.isBlank() && !safeUrl.contains("?v=")) {
            safeUrl = safeUrl + "?v=" + (System.currentTimeMillis() / 3_600_000L);
        }

        return MediaUploadResponseDto.builder()
                .id(media.getId())
                .modelId(media.getModel().getId())
                .mediaType(media.getMediaType())
                .fileUrl(safeUrl)
                .filePath(media.getFilePath())
                .displayOrder(media.getDisplayOrder())
                .isCover(media.getIsCover())
                .isActive(Boolean.TRUE.equals(media.getIsActive()))
                .createdAt(media.getCreatedAt())
                .build();
    }
}

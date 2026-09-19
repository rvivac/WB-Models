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

        String bucket = supabaseProperties.getBuckets().getModelsMedia();

        // Se a nova mídia for do tipo COMPOSITE, substitui o composite anterior se houver
        if (mediaType == MediaType.COMPOSITE) {
            Optional<ModelMedia> existingComposite = modelMediaRepository.findByModelIdAndMediaTypeAndIsActiveTrue(modelId, MediaType.COMPOSITE);
            if (existingComposite.isPresent()) {
                ModelMedia oldComp = existingComposite.get();
                try {
                    storageService.deleteFile(bucket, oldComp.getStoragePath());
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
        String publicUrl = storageService.getPublicUrl(bucket, uploadedPath);

        // Persistência do registro JPA
        ModelMedia media = ModelMedia.builder()
                .model(model)
                .mediaType(mediaType)
                .fileUrl(publicUrl)
                .storagePath(uploadedPath)
                .displayOrder(nextOrder)
                .isCover(isCover)
                .isActive(true)
                .build();

        ModelMedia savedMedia = modelMediaRepository.save(media);

        // Se for capa, sincroniza a foto principal do modelo
        if (isCover) {
            model.setPrimaryPhotoUrl(publicUrl);
            modelRepository.save(model);
        }

        log.info("Mídia cadastrada com sucesso: id='{}', modelId='{}', url='{}'", savedMedia.getId(), modelId, publicUrl);
        return toDto(savedMedia);
    }

    @Override
    @Transactional
    public void deleteMedia(UUID modelId, UUID mediaId) {
        log.info("Iniciando exclusão de mídia id='{}' do modelo id='{}'", mediaId, modelId);

        ModelMedia media = modelMediaRepository.findByIdAndModelId(mediaId, modelId)
                .orElseThrow(() -> new ResourceNotFoundException("Mídia", "id", mediaId));

        String bucket = supabaseProperties.getBuckets().getModelsMedia();
        String storagePath = media.getStoragePath();

        // 1. Excluir arquivo físico no Supabase Storage
        storageService.deleteFile(bucket, storagePath);

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

    private MediaUploadResponseDto toDto(ModelMedia media) {
        return MediaUploadResponseDto.builder()
                .id(media.getId())
                .modelId(media.getModel().getId())
                .mediaType(media.getMediaType())
                .fileUrl(media.getFileUrl())
                .storagePath(media.getStoragePath())
                .displayOrder(media.getDisplayOrder())
                .isCover(media.getIsCover())
                .createdAt(media.getCreatedAt())
                .build();
    }
}

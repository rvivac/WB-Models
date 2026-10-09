package com.wbscouting.api.service.publicapi;

import com.wbscouting.api.dto.publicapi.ModelCardPublicDto;
import com.wbscouting.api.dto.publicapi.ModelDetailPublicDto;
import com.wbscouting.api.dto.publicapi.PageResponseDto;
import com.wbscouting.api.entity.Model;
import com.wbscouting.api.entity.ModelMedia;
import com.wbscouting.api.enums.GenderType;
import com.wbscouting.api.enums.MediaType;
import com.wbscouting.api.repository.ModelRepository;
import com.wbscouting.api.repository.specification.ModelSpecification;
import com.wbscouting.api.service.storage.SupabaseStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublicModelServiceImpl implements PublicModelService {

    private final ModelRepository modelRepository;
    private final PublicModelDetailService publicModelDetailService;
    private final SupabaseStorageService storageService;

    // Helper: resolve URL PUBLICA de uma ModelMedia usando file_path se file_url incompleto
    private String resolveMediaUrlPublic(ModelMedia media) {
        if (media == null) return null;
        if (storageService == null || storageService.getProperties() == null) {
            return media.getFileUrl();
        }
        return storageService.resolvePublicUrlFromFields(
                storageService.getProperties().resolveBucketModelsMedia(),
                media.getFilePath(),
                media.getFileUrl());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ModelCardPublicDto> getFeaturedModels() {
        log.info("Buscando modelos em destaque para a home pública");

        List<Model> models = modelRepository.findFeaturedHomeModels();
        return models.stream()
                .map(this::toCardDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDto<ModelCardPublicDto> listModels(GenderType gender, Boolean isStar, int page, int size) {
        int validatedPage = Math.max(page, 0);
        int validatedSize = (size <= 0) ? 24 : Math.min(size, 100);

        log.info("Listando casting público: gender={}, isStar={}, page={}, size={}", gender, isStar, validatedPage, validatedSize);

        Pageable pageable = PageRequest.of(validatedPage, validatedSize, Sort.by(Sort.Order.asc("stageName")));
        Specification<Model> spec = ModelSpecification.filter(gender, isStar, true, null);

        Page<Model> modelPage = modelRepository.findAll(spec, pageable);
        Page<ModelCardPublicDto> dtoPage = modelPage.map(this::toCardDto);

        return PageResponseDto.from(dtoPage);
    }

    @Override
    @Transactional(readOnly = true)
    public ModelDetailPublicDto getModelDetail(UUID id) {
        return publicModelDetailService.getModelDetail(id);
    }

    private ModelCardPublicDto toCardDto(Model model) {
        return ModelCardPublicDto.builder()
                .id(model.getId())
                .stageName(model.getStageName())
                .gender(model.getGender())
                .coverImageUrl(resolveCoverImageUrl(model))
                .heightCm(model.getHeightCm())
                .isStar(model.getIsStar())
                .build();
    }

    private String resolveCoverImageUrl(Model model) {
        // 1. Foto primaria do cadastro (validando URL completa + FALLBACK se incompleta!)
        String primary = model.getPrimaryPhotoUrl();
        if (StringUtils.hasText(primary)) {
            if (storageService == null || storageService.getProperties() == null) {
                return primary;
            }
            String modelsMediaBucket = storageService.getProperties().resolveBucketModelsMedia();
            // a) URL parece OK (possui caminho do arquivo apos o bucket) -> retorna direto
            boolean pareceCompleta = primary.contains(modelsMediaBucket + "/")
                    && primary.lastIndexOf(modelsMediaBucket + "/") + modelsMediaBucket.length() + 1 < primary.length()
                    && !primary.endsWith("/");
            if (pareceCompleta) return primary;

            // b) URL incompleta ou suspeita? TENTA FALLBACK: remontar usando filePath de qualquer modelMedia
            // (usa o helper resolvePublicUrlFromFields criado ontem, nunca mais da HTTP 400)
            String filePathGuess = null;
            if (model.getMedia() != null && !model.getMedia().isEmpty()) {
                for (ModelMedia m : model.getMedia()) {
                    if (Boolean.TRUE.equals(m.getIsActive()) && Boolean.TRUE.equals(m.getIsCover())) {
                        filePathGuess = m.getFilePath();
                        break;
                    }
                }
                if (filePathGuess == null) {
                    // Se nenhuma capa marcada, usa primeira BOOK ou primeira ativa
                    for (ModelMedia m : model.getMedia()) {
                        if (Boolean.TRUE.equals(m.getIsActive()) && m.getMediaType() == MediaType.BOOK) {
                            filePathGuess = m.getFilePath(); break;
                        }
                    }
                    if (filePathGuess == null) {
                        for (ModelMedia m : model.getMedia()) {
                            if (Boolean.TRUE.equals(m.getIsActive())) { filePathGuess = m.getFilePath(); break; }
                        }
                    }
                }
            }
            String remontada = storageService.resolvePublicUrlFromFields(modelsMediaBucket, filePathGuess, primary);
            if (StringUtils.hasText(remontada) && !remontada.endsWith("/") && remontada.length() > 40) {
                return remontada;
            }
        }

        if (model.getMedia() != null && !model.getMedia().isEmpty()) {
            // 2. isCover=true
            for (ModelMedia m : model.getMedia()) {
                if (Boolean.TRUE.equals(m.getIsActive()) && Boolean.TRUE.equals(m.getIsCover())) {
                    String url = resolveMediaUrlPublic(m);
                    if (StringUtils.hasText(url)) return url;
                }
            }
            // 3. Primeira BOOK
            for (ModelMedia m : model.getMedia()) {
                if (Boolean.TRUE.equals(m.getIsActive()) && m.getMediaType() == MediaType.BOOK) {
                    String url = resolveMediaUrlPublic(m);
                    if (StringUtils.hasText(url)) return url;
                }
            }
            // 4. Primeira ativa (qualquer)
            for (ModelMedia m : model.getMedia()) {
                if (Boolean.TRUE.equals(m.getIsActive())) {
                    String url = resolveMediaUrlPublic(m);
                    if (StringUtils.hasText(url)) return url;
                }
            }
        }
        return null;
    }
}

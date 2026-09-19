package com.wbscouting.api.service.publicapi;

import com.wbscouting.api.dto.publicapi.ModelCardPublicDto;
import com.wbscouting.api.dto.publicapi.PageResponseDto;
import com.wbscouting.api.entity.Model;
import com.wbscouting.api.entity.ModelMedia;
import com.wbscouting.api.enums.GenderType;
import com.wbscouting.api.enums.MediaType;
import com.wbscouting.api.repository.ModelMediaRepository;
import com.wbscouting.api.repository.ModelRepository;
import com.wbscouting.api.specification.PublicModelSpecification;
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

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublicModelCatalogServiceImpl implements PublicModelCatalogService {

    public static final int DEFAULT_PAGE_SIZE = 24;
    public static final int MAX_PAGE_SIZE = 48;

    private final ModelRepository modelRepository;
    private final ModelMediaRepository modelMediaRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponseDto<ModelCardPublicDto> listModels(
            GenderType gender,
            Boolean isStar,
            String search,
            int page,
            int size,
            String sort
    ) {
        int validatedPage = Math.max(page, 0);
        int validatedSize = (size <= 0) ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);

        Sort parsedSort = parseSort(sort);
        Pageable pageable = PageRequest.of(validatedPage, validatedSize, parsedSort);

        log.info("Executando consulta pública do catálogo: gender={}, isStar={}, search='{}', page={}, size={}, sort={}",
                gender, isStar, search, validatedPage, validatedSize, parsedSort);

        Specification<Model> spec = PublicModelSpecification.filter(gender, isStar, search);

        Page<Model> modelPage = modelRepository.findAll(spec, pageable);

        if (modelPage.isEmpty()) {
            return PageResponseDto.<ModelCardPublicDto>builder()
                    .content(Collections.emptyList())
                    .pageNumber(modelPage.getNumber())
                    .pageSize(modelPage.getSize())
                    .totalElements(modelPage.getTotalElements())
                    .totalPages(modelPage.getTotalPages())
                    .isLast(modelPage.isLast())
                    .build();
        }

        List<Model> models = modelPage.getContent();
        List<UUID> modelIds = models.stream().map(Model::getId).toList();

        // Prevenção estrita de N+1: Carrega todas as mídias ativas dos modelos da página em uma única consulta SQL
        List<ModelMedia> activeMedia = modelMediaRepository.findByModelIdInAndIsActiveTrueOrderByDisplayOrderAsc(modelIds);
        Map<UUID, List<ModelMedia>> mediaByModelId = activeMedia.stream()
                .collect(Collectors.groupingBy(m -> m.getModel().getId()));

        Page<ModelCardPublicDto> dtoPage = modelPage.map(model -> {
            List<ModelMedia> modelMedia = mediaByModelId.getOrDefault(model.getId(), Collections.emptyList());
            String coverImageUrl = resolveCoverImageUrl(model, modelMedia);

            return ModelCardPublicDto.builder()
                    .id(model.getId())
                    .stageName(model.getStageName())
                    .gender(model.getGender())
                    .coverImageUrl(coverImageUrl)
                    .heightCm(model.getHeightCm())
                    .city(model.getCity())
                    .isStar(model.getIsStar())
                    .build();
        });

        return PageResponseDto.from(dtoPage);
    }

    private String resolveCoverImageUrl(Model model, List<ModelMedia> modelMedia) {
        // 1. Foto explicitamente marcada como capa (is_cover = true)
        for (ModelMedia m : modelMedia) {
            if (Boolean.TRUE.equals(m.getIsCover()) && StringUtils.hasText(m.getFileUrl())) {
                return m.getFileUrl();
            }
        }

        // 2. Primeira foto do tipo BOOK baseada no display_order
        for (ModelMedia m : modelMedia) {
            if (m.getMediaType() == MediaType.BOOK && StringUtils.hasText(m.getFileUrl())) {
                return m.getFileUrl();
            }
        }

        // 3. Primeira foto ativa baseada no display_order
        for (ModelMedia m : modelMedia) {
            if (StringUtils.hasText(m.getFileUrl())) {
                return m.getFileUrl();
            }
        }

        // 4. Fallback para foto primária do cadastro
        if (StringUtils.hasText(model.getPrimaryPhotoUrl())) {
            return model.getPrimaryPhotoUrl();
        }

        return null;
    }

    private Sort parseSort(String sort) {
        if (!StringUtils.hasText(sort)) {
            return Sort.by(Sort.Order.asc("stageName"));
        }

        String[] parts = sort.split(",");
        String property = parts[0].trim();
        Sort.Direction direction = (parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim()))
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        return Sort.by(direction, property);
    }
}

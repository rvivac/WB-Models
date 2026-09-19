package com.wbscouting.api.service.publicapi;

import com.wbscouting.api.dto.publicapi.MediaPublicDto;
import com.wbscouting.api.dto.publicapi.ModelCardPublicDto;
import com.wbscouting.api.dto.publicapi.ModelDetailPublicDto;
import com.wbscouting.api.dto.publicapi.PageResponseDto;
import com.wbscouting.api.entity.Model;
import com.wbscouting.api.entity.ModelMedia;
import com.wbscouting.api.enums.GenderType;
import com.wbscouting.api.enums.MediaType;
import com.wbscouting.api.exception.ResourceNotFoundException;
import com.wbscouting.api.repository.ModelMediaRepository;
import com.wbscouting.api.repository.ModelRepository;
import com.wbscouting.api.repository.specification.ModelSpecification;
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

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublicModelServiceImpl implements PublicModelService {

    private final ModelRepository modelRepository;
    private final ModelMediaRepository modelMediaRepository;
    private final PublicModelDetailService publicModelDetailService;

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
                .city(model.getCity())
                .isStar(model.getIsStar())
                .build();
    }

    private String resolveCoverImageUrl(Model model) {
        if (StringUtils.hasText(model.getPrimaryPhotoUrl())) {
            return model.getPrimaryPhotoUrl();
        }

        if (model.getMedia() != null && !model.getMedia().isEmpty()) {
            for (ModelMedia m : model.getMedia()) {
                if (Boolean.TRUE.equals(m.getIsActive()) && Boolean.TRUE.equals(m.getIsCover()) && StringUtils.hasText(m.getFileUrl())) {
                    return m.getFileUrl();
                }
            }
            for (ModelMedia m : model.getMedia()) {
                if (Boolean.TRUE.equals(m.getIsActive()) && m.getMediaType() == MediaType.BOOK && StringUtils.hasText(m.getFileUrl())) {
                    return m.getFileUrl();
                }
            }
            for (ModelMedia m : model.getMedia()) {
                if (Boolean.TRUE.equals(m.getIsActive()) && StringUtils.hasText(m.getFileUrl())) {
                    return m.getFileUrl();
                }
            }
        }
        return null;
    }
}

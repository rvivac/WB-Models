package com.wbscouting.api.service;

import com.wbscouting.api.dto.ModelDTO;
import com.wbscouting.api.entity.Model;
import com.wbscouting.api.enums.GenderType;
import com.wbscouting.api.exception.ResourceNotFoundException;
import com.wbscouting.api.repository.ModelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service("legacyModelService")
@RequiredArgsConstructor
public class ModelService {

    private final ModelRepository modelRepository;

    @Transactional(readOnly = true)
    public Page<ModelDTO.SummaryResponse> listModels(GenderType gender, Boolean isStar, Pageable pageable) {
        Page<Model> models;

        if (gender != null) {
            models = modelRepository.findByGenderAndIsActiveTrue(gender, pageable);
        } else if (Boolean.TRUE.equals(isStar)) {
            models = modelRepository.findByIsStarTrueAndIsActiveTrue(pageable);
        } else {
            models = modelRepository.findByIsActiveTrue(pageable);
        }

        return models.map(this::mapToSummary);
    }

    @Transactional(readOnly = true)
    public List<ModelDTO.SummaryResponse> getFeaturedHomeModels() {
        return modelRepository.findFeaturedHomeModels()
                .stream()
                .map(this::mapToSummary)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ModelDTO.DetailResponse getModelById(UUID id) {
        Model model = modelRepository.findById(id)
                .filter(Model::getIsActive)
                .orElseThrow(() -> new ResourceNotFoundException("Modelo", "id", id));

        return mapToDetail(model);
    }

    private ModelDTO.SummaryResponse mapToSummary(Model model) {
        return ModelDTO.SummaryResponse.builder()
                .id(model.getId())
                .stageName(model.getStageName())
                .gender(model.getGender())
                .isStar(model.getIsStar())
                .primaryPhotoUrl(model.getPrimaryPhotoUrl())
                .heightCm(model.getHeightCm())
                .dressSize(model.getDressSize())
                .shoeSize(model.getShoeSize())
                .city(model.getCity())
                .build();
    }

    private ModelDTO.DetailResponse mapToDetail(Model model) {
        List<ModelDTO.MediaResponse> mediaResponses = model.getMedia()
                .stream()
                .filter(m -> Boolean.TRUE.equals(m.getIsActive()))
                .map(m -> ModelDTO.MediaResponse.builder()
                        .id(m.getId())
                        .mediaType(m.getMediaType())
                        .fileUrl(m.getFileUrl())
                        .displayOrder(m.getDisplayOrder())
                        .build())
                .collect(Collectors.toList());

        return ModelDTO.DetailResponse.builder()
                .id(model.getId())
                .stageName(model.getStageName())
                .gender(model.getGender())
                .isStar(model.getIsStar())
                .isFeaturedHome(model.getIsFeaturedHome())
                .primaryPhotoUrl(model.getPrimaryPhotoUrl())
                .instagramUrl(model.getInstagramUrl())
                .birthDate(model.getBirthDate())
                .heightCm(model.getHeightCm())
                .city(model.getCity())
                .nationality(model.getNationality())
                .dressSize(model.getDressSize())
                .shoeSize(model.getShoeSize())
                .bustChestCm(model.getBustChestCm())
                .waistCm(model.getWaistCm())
                .hipsCm(model.getHipsCm())
                .hairColor(model.getHairColor())
                .eyesColor(model.getEyesColor())
                .media(mediaResponses)
                .build();
    }
}

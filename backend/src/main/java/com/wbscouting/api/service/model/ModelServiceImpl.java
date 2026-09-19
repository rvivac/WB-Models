package com.wbscouting.api.service.model;

import com.wbscouting.api.dto.ModelDTO;
import com.wbscouting.api.dto.model.*;
import com.wbscouting.api.entity.Model;
import com.wbscouting.api.enums.GenderType;
import com.wbscouting.api.exception.ResourceNotFoundException;
import com.wbscouting.api.repository.ModelRepository;
import com.wbscouting.api.repository.specification.ModelSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModelServiceImpl implements ModelService {

    private final ModelRepository modelRepository;

    @Override
    @Transactional
    public ModelAdminResponseDto createModel(ModelCreateRequestDto dto) {
        log.info("Cadastrando novo modelo artístico: {}", dto.getStageName());

        Model model = Model.builder()
                .stageName(dto.getStageName())
                .gender(dto.getGender())
                .isStar(Boolean.TRUE.equals(dto.getIsStar()))
                .isFeaturedHome(Boolean.TRUE.equals(dto.getIsFeaturedHome()))
                .featuredOrder(dto.getFeaturedOrder())
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .primaryPhotoUrl(dto.getPrimaryPhotoUrl())
                .instagramUrl(dto.getInstagramUrl())
                .birthDate(dto.getBirthDate())
                .heightCm(dto.getHeightCm())
                .city(dto.getCity())
                .nationality(dto.getNationality())
                .dressSize(dto.getDressSize())
                .shoeSize(dto.getShoeSize())
                .bustChestCm(dto.getBustChestCm())
                .waistCm(dto.getWaistCm())
                .hipsCm(dto.getHipsCm())
                .hairColor(dto.getHairColor())
                .eyesColor(dto.getEyesColor())
                .build();

        Model savedModel = modelRepository.save(model);
        log.info("Modelo cadastrado com sucesso com ID: {}", savedModel.getId());
        return mapToAdminResponse(savedModel);
    }

    @Override
    @Transactional
    public ModelAdminResponseDto updateModel(UUID id, ModelUpdateRequestDto dto) {
        log.info("Atualizando dados do modelo com ID: {}", id);

        Model model = modelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Modelo", "id", id));

        model.setStageName(dto.getStageName());
        model.setGender(dto.getGender());
        if (dto.getIsStar() != null) model.setIsStar(dto.getIsStar());
        if (dto.getIsFeaturedHome() != null) model.setIsFeaturedHome(dto.getIsFeaturedHome());
        model.setFeaturedOrder(dto.getFeaturedOrder());
        if (dto.getIsActive() != null) model.setIsActive(dto.getIsActive());
        model.setPrimaryPhotoUrl(dto.getPrimaryPhotoUrl());
        model.setInstagramUrl(dto.getInstagramUrl());
        model.setBirthDate(dto.getBirthDate());
        model.setHeightCm(dto.getHeightCm());
        model.setCity(dto.getCity());
        model.setNationality(dto.getNationality());
        model.setDressSize(dto.getDressSize());
        model.setShoeSize(dto.getShoeSize());
        model.setBustChestCm(dto.getBustChestCm());
        model.setWaistCm(dto.getWaistCm());
        model.setHipsCm(dto.getHipsCm());
        model.setHairColor(dto.getHairColor());
        model.setEyesColor(dto.getEyesColor());

        Model updatedModel = modelRepository.save(model);
        log.info("Modelo ID: {} atualizado com sucesso", updatedModel.getId());
        return mapToAdminResponse(updatedModel);
    }

    @Override
    @Transactional
    public ModelAdminResponseDto updateStatus(UUID id, ModelStatusPatchDto dto) {
        log.info("Atualizando status de ativação do modelo ID: {} para {}", id, dto.getIsActive());

        Model model = modelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Modelo", "id", id));

        model.setIsActive(dto.getIsActive());
        Model saved = modelRepository.save(model);
        return mapToAdminResponse(saved);
    }

    @Override
    @Transactional
    public ModelAdminResponseDto updateStar(UUID id, ModelStarPatchDto dto) {
        log.info("Atualizando status Star do modelo ID: {} para {}", id, dto.getIsStar());

        Model model = modelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Modelo", "id", id));

        model.setIsStar(dto.getIsStar());
        Model saved = modelRepository.save(model);
        return mapToAdminResponse(saved);
    }

    @Override
    @Transactional
    public ModelAdminResponseDto updateFeatured(UUID id, ModelFeaturedPatchDto dto) {
        log.info("Atualizando destaque na home do modelo ID: {} (isFeaturedHome={}, featuredOrder={})",
                id, dto.getIsFeaturedHome(), dto.getFeaturedOrder());

        Model model = modelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Modelo", "id", id));

        model.setIsFeaturedHome(dto.getIsFeaturedHome());
        model.setFeaturedOrder(dto.getFeaturedOrder());
        Model saved = modelRepository.save(model);
        return mapToAdminResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ModelAdminResponseDto> listAdminModels(
            GenderType gender, Boolean isStar, Boolean isActive, String search, Pageable pageable) {

        Specification<Model> spec = ModelSpecification.filter(gender, isStar, isActive, search);
        return modelRepository.findAll(spec, pageable).map(this::mapToAdminResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ModelAdminResponseDto getAdminModelById(UUID id) {
        Model model = modelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Modelo", "id", id));
        return mapToAdminResponse(model);
    }

    @Override
    @Transactional
    public void deleteModel(UUID id) {
        log.info("Excluindo modelo com ID: {}", id);

        Model model = modelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Modelo", "id", id));

        modelRepository.delete(model);
        log.info("Modelo ID: {} excluído com sucesso", id);
    }

    // Métodos Públicos de Catálogo (Portal / Site Institucional)

    @Override
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

    @Override
    @Transactional(readOnly = true)
    public List<ModelDTO.SummaryResponse> getFeaturedHomeModels() {
        return modelRepository.findFeaturedHomeModels()
                .stream()
                .map(this::mapToSummary)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ModelDTO.DetailResponse getModelById(UUID id) {
        Model model = modelRepository.findById(id)
                .filter(Model::getIsActive)
                .orElseThrow(() -> new ResourceNotFoundException("Modelo", "id", id));

        return mapToDetail(model);
    }

    private ModelAdminResponseDto mapToAdminResponse(Model model) {
        return ModelAdminResponseDto.builder()
                .id(model.getId())
                .stageName(model.getStageName())
                .gender(model.getGender())
                .isStar(model.getIsStar())
                .isFeaturedHome(model.getIsFeaturedHome())
                .featuredOrder(model.getFeaturedOrder())
                .isActive(model.getIsActive())
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
                .createdAt(model.getCreatedAt())
                .updatedAt(model.getUpdatedAt())
                .build();
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

package com.wbscouting.api.service.model;

import com.wbscouting.api.dto.model.FeaturedModelOrderItemDto;
import com.wbscouting.api.dto.model.FeaturedModelResponseDto;
import com.wbscouting.api.dto.model.FeaturedModelsReorderRequestDto;
import com.wbscouting.api.entity.Model;
import com.wbscouting.api.enums.GenderType;
import com.wbscouting.api.exception.ResourceNotFoundException;
import com.wbscouting.api.repository.ModelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeaturedModelServiceImpl implements FeaturedModelService {

    private final ModelRepository modelRepository;

    @Override
    @Transactional(readOnly = true)
    public List<FeaturedModelResponseDto> getFeaturedHomeModels() {
        log.debug("Buscando modelos em destaque na Home");
        List<Model> models = modelRepository.findByIsActiveTrueAndIsFeaturedHomeTrueOrderByFeaturedOrderAscStageNameAsc();
        return models.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<FeaturedModelResponseDto> updateFeaturedHomeModels(FeaturedModelsReorderRequestDto request) {
        if (request == null || request.getItems() == null) {
            throw new IllegalArgumentException("A lista de itens de destaque não pode ser nula.");
        }

        List<FeaturedModelOrderItemDto> items = request.getItems();
        if (items.size() < 4 || items.size() > 8) {
            throw new IllegalArgumentException("A vitrine da Home deve conter entre 4 e 8 modelos (recebido: " + items.size() + ").");
        }

        List<UUID> newModelIds = items.stream()
                .map(FeaturedModelOrderItemDto::getModelId)
                .toList();

        Set<UUID> uniqueIds = new HashSet<>(newModelIds);
        if (uniqueIds.size() != newModelIds.size()) {
            throw new IllegalArgumentException("A lista de modelos em destaque não pode conter IDs duplicados.");
        }

        log.info("Iniciando atualização atômica da vitrine da Home com {} modelos", items.size());

        // 1. Desmarcar modelos atualmente destacados que não estão na nova lista
        List<Model> currentlyFeatured = modelRepository.findByIsActiveTrueAndIsFeaturedHomeTrueOrderByFeaturedOrderAscStageNameAsc();
        for (Model current : currentlyFeatured) {
            if (!uniqueIds.contains(current.getId())) {
                current.setIsFeaturedHome(false);
                current.setFeaturedOrder(null);
                modelRepository.save(current);
                log.debug("Modelo id='{}' ({}) removido dos destaques da Home", current.getId(), current.getStageName());
            }
        }

        // 2. Atualizar ou promover os modelos da nova lista com a devida ordem
        for (FeaturedModelOrderItemDto item : items) {
            Model model = modelRepository.findById(item.getModelId())
                    .orElseThrow(() -> new ResourceNotFoundException("Modelo", "id", item.getModelId()));

            if (!Boolean.TRUE.equals(model.getIsActive())) {
                throw new IllegalArgumentException("Apenas modelos com status ativo podem ser colocados em destaque na Home: " + model.getStageName());
            }

            model.setIsFeaturedHome(true);
            model.setFeaturedOrder(item.getDisplayOrder());
            modelRepository.save(model);
            log.debug("Modelo id='{}' ({}) atualizado para ordem {}", model.getId(), model.getStageName(), item.getDisplayOrder());
        }

        modelRepository.flush();
        log.info("Vitrine da Home atualizada com sucesso.");

        return getFeaturedHomeModels();
    }

    private FeaturedModelResponseDto toDto(Model model) {
        String category = "FASHION";
        if (model.getGender() == GenderType.MALE) {
            category = "COMMERCIAL";
        }

        return FeaturedModelResponseDto.builder()
                .id(model.getId())
                .artisticName(model.getStageName())
                .stageName(model.getStageName())
                .category(category)
                .height(model.getHeightCm() != null ? model.getHeightCm() : 175)
                .heightCm(model.getHeightCm())
                .isStar(Boolean.TRUE.equals(model.getIsStar()))
                .coverPhotoUrl(model.getPrimaryPhotoUrl())
                .primaryPhotoUrl(model.getPrimaryPhotoUrl())
                .displayOrder(model.getFeaturedOrder())
                .featuredOrder(model.getFeaturedOrder())
                .build();
    }
}

package com.wbscouting.api.service.model;

import com.wbscouting.api.dto.model.FeaturedModelOrderItemDto;
import com.wbscouting.api.dto.model.FeaturedModelResponseDto;
import com.wbscouting.api.dto.model.FeaturedModelsReorderRequestDto;
import com.wbscouting.api.entity.FeaturedModel;
import com.wbscouting.api.entity.Model;
import com.wbscouting.api.enums.GenderType;
import com.wbscouting.api.exception.ResourceNotFoundException;
import com.wbscouting.api.repository.FeaturedModelRepository;
import com.wbscouting.api.repository.ModelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeaturedModelServiceImpl implements FeaturedModelService {

    private final ModelRepository modelRepository;
    private final FeaturedModelRepository featuredModelRepository;

    @Override
    @Transactional(readOnly = true)
    public List<FeaturedModelResponseDto> getFeaturedHomeModels() {
        log.debug("Buscando modelos em destaque na Home a partir de featured_models e models");

        List<FeaturedModel> featuredEntries = featuredModelRepository.findAllByOrderByDisplayOrderAsc();
        if (!featuredEntries.isEmpty()) {
            List<FeaturedModelResponseDto> result = new ArrayList<>();
            for (FeaturedModel entry : featuredEntries) {
                Optional<Model> mOpt = modelRepository.findById(entry.getModelId());
                if (mOpt.isPresent() && Boolean.TRUE.equals(mOpt.get().getIsActive())) {
                    FeaturedModelResponseDto dto = toDto(mOpt.get());
                    dto.setDisplayOrder(entry.getDisplayOrder());
                    dto.setFeaturedOrder(entry.getDisplayOrder());
                    result.add(dto);
                }
            }
            if (!result.isEmpty()) {
                return result;
            }
        }

        // Fallback para coluna is_featured_home da tabela models
        List<Model> models = modelRepository.findByIsActiveTrueAndIsFeaturedHomeTrueOrderByFeaturedOrderAscStageNameAsc();
        return models.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<FeaturedModelResponseDto> updateFeaturedHomeModels(FeaturedModelsReorderRequestDto request) {
        List<FeaturedModelOrderItemDto> items = (request != null && request.getItems() != null)
                ? request.getItems()
                : Collections.emptyList();

        List<UUID> newModelIds = items.stream()
                .map(FeaturedModelOrderItemDto::getModelId)
                .toList();

        Set<UUID> uniqueIds = new HashSet<>(newModelIds);
        if (uniqueIds.size() != newModelIds.size()) {
            throw new IllegalArgumentException("A lista de modelos em destaque não pode conter IDs duplicados.");
        }

        log.info("Iniciando atualização da vitrine da Home com {} modelos (sem validação de quantidade mínima)", items.size());

        // 1. Limpar tabela featured_models
        featuredModelRepository.deleteAll();

        // 2. Desmarcar modelos atualmente destacados na tabela models
        List<Model> currentlyFeatured = modelRepository.findByIsActiveTrueAndIsFeaturedHomeTrueOrderByFeaturedOrderAscStageNameAsc();
        for (Model current : currentlyFeatured) {
            if (!uniqueIds.contains(current.getId())) {
                current.setIsFeaturedHome(false);
                current.setFeaturedOrder(null);
                modelRepository.save(current);
            }
        }

        // 3. Inserir em featured_models e atualizar flags em models
        List<FeaturedModel> newFeaturedEntries = new ArrayList<>();
        int orderCounter = 1;
        for (FeaturedModelOrderItemDto item : items) {
            Model model = modelRepository.findById(item.getModelId())
                    .orElseThrow(() -> new ResourceNotFoundException("Modelo", "id", item.getModelId()));

            int order = item.getDisplayOrder() != null ? item.getDisplayOrder() : orderCounter;

            model.setIsFeaturedHome(true);
            model.setFeaturedOrder(order);
            modelRepository.save(model);

            FeaturedModel entry = FeaturedModel.builder()
                    .modelId(model.getId())
                    .displayOrder(order)
                    .createdAt(OffsetDateTime.now())
                    .build();
            newFeaturedEntries.add(entry);
            orderCounter++;
        }

        featuredModelRepository.saveAllAndFlush(newFeaturedEntries);
        modelRepository.flush();

        log.info("Vitrine da Home atualizada com sucesso em featured_models e models.");
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

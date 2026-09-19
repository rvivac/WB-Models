package com.wbscouting.api.service.model;

import com.wbscouting.api.dto.ModelDTO;
import com.wbscouting.api.dto.model.*;
import com.wbscouting.api.enums.GenderType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface ModelService {

    ModelAdminResponseDto createModel(ModelCreateRequestDto dto);

    ModelAdminResponseDto updateModel(UUID id, ModelUpdateRequestDto dto);

    ModelAdminResponseDto updateStatus(UUID id, ModelStatusPatchDto dto);

    ModelAdminResponseDto updateStar(UUID id, ModelStarPatchDto dto);

    ModelAdminResponseDto updateFeatured(UUID id, ModelFeaturedPatchDto dto);

    Page<ModelAdminResponseDto> listAdminModels(GenderType gender, Boolean isStar, Boolean isActive, String search, Pageable pageable);

    ModelAdminResponseDto getAdminModelById(UUID id);

    void deleteModel(UUID id);

    Page<ModelDTO.SummaryResponse> listModels(GenderType gender, Boolean isStar, Pageable pageable);

    List<ModelDTO.SummaryResponse> getFeaturedHomeModels();

    ModelDTO.DetailResponse getModelById(UUID id);
}

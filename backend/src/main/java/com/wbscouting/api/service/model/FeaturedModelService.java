package com.wbscouting.api.service.model;

import com.wbscouting.api.dto.model.FeaturedModelResponseDto;
import com.wbscouting.api.dto.model.FeaturedModelsReorderRequestDto;

import java.util.List;

public interface FeaturedModelService {

    List<FeaturedModelResponseDto> getFeaturedHomeModels();

    List<FeaturedModelResponseDto> updateFeaturedHomeModels(FeaturedModelsReorderRequestDto request);
}

package com.wbscouting.api.service.publicapi;

import com.wbscouting.api.dto.publicapi.ModelCardPublicDto;
import com.wbscouting.api.dto.publicapi.ModelDetailPublicDto;
import com.wbscouting.api.dto.publicapi.PageResponseDto;
import com.wbscouting.api.enums.GenderType;

import java.util.List;
import java.util.UUID;

public interface PublicModelService {

    List<ModelCardPublicDto> getFeaturedModels();

    PageResponseDto<ModelCardPublicDto> listModels(GenderType gender, Boolean isStar, int page, int size);

    ModelDetailPublicDto getModelDetail(UUID id);
}

package com.wbscouting.api.service.publicapi;

import com.wbscouting.api.dto.publicapi.ModelCardPublicDto;
import com.wbscouting.api.dto.publicapi.PageResponseDto;
import com.wbscouting.api.enums.GenderType;

public interface PublicModelCatalogService {

    PageResponseDto<ModelCardPublicDto> listModels(GenderType gender, Boolean isStar, String search, int page, int size, String sort);
}

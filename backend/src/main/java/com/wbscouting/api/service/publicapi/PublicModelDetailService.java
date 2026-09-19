package com.wbscouting.api.service.publicapi;

import com.wbscouting.api.dto.publicapi.ModelDetailPublicDto;

import java.util.UUID;

public interface PublicModelDetailService {

    ModelDetailPublicDto getModelDetail(UUID id);
}

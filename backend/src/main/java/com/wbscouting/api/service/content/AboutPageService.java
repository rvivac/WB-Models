package com.wbscouting.api.service.content;

import com.wbscouting.api.dto.AboutPageDto;

public interface AboutPageService {
    AboutPageDto getPublicAboutPage();
    AboutPageDto getPublicAboutPage(String lang);
    com.wbscouting.api.dto.AboutPageResponseDto getPublicAboutPageResponse();
    com.wbscouting.api.dto.AboutPageResponseDto getPublicAboutPageResponse(String lang);
    AboutPageDto getAdminAboutPage();
    AboutPageDto updateAboutPage(AboutPageDto dto);
}

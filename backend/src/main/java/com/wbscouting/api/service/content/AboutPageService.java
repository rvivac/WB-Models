package com.wbscouting.api.service.content;

import com.wbscouting.api.dto.AboutPageDto;

public interface AboutPageService {
    AboutPageDto getPublicAboutPage();
    AboutPageDto getAdminAboutPage();
    AboutPageDto updateAboutPage(AboutPageDto dto);
}

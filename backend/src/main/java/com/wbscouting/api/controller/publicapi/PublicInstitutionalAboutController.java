package com.wbscouting.api.controller.publicapi;

import com.wbscouting.api.dto.AboutPageDto;
import com.wbscouting.api.service.content.AboutPageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
@Slf4j
@RestController
@RequiredArgsConstructor
public class PublicInstitutionalAboutController {

    private final AboutPageService aboutPageService;

    @GetMapping({"/api/v1/public/institutional/about", "/public/institutional/about"})
    public ResponseEntity<com.wbscouting.api.dto.AboutPageResponseDto> getPublicAboutPage(
            @org.springframework.web.bind.annotation.RequestParam(value = "lang", required = false) String lang,
            @org.springframework.web.bind.annotation.RequestHeader(value = "Accept-Language", required = false) String acceptLanguage
    ) {
        String resolvedLang = lang;
        if ((resolvedLang == null || resolvedLang.isBlank()) && acceptLanguage != null && acceptLanguage.toLowerCase().startsWith("en")) {
            resolvedLang = "en";
        }
        log.info("Acesso público às configurações e textos da página Sobre Nós (lang={})", resolvedLang);
        return ResponseEntity.ok(aboutPageService.getPublicAboutPageResponse(resolvedLang));
    }
}

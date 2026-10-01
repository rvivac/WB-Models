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
    public ResponseEntity<AboutPageDto> getPublicAboutPage() {
        log.info("Acesso público às configurações e textos da página Sobre Nós");
        return ResponseEntity.ok(aboutPageService.getPublicAboutPage());
    }
}

package com.wbscouting.api.controller.admin;

import com.wbscouting.api.dto.AboutPageDto;
import com.wbscouting.api.service.content.AboutPageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@PreAuthorize("hasAnyRole('WEBMASTER', 'SUPER_ADMIN', 'CONTENT_ADMIN', 'ADMIN')")
@RequiredArgsConstructor
public class AdminInstitutionalAboutController {

    private final AboutPageService aboutPageService;

    @GetMapping({"/api/v1/admin/institutional/about", "/admin/institutional/about"})
    public ResponseEntity<AboutPageDto> getAboutPageSettings() {
        log.info("Admin consultando dados e textos da página institucional Sobre Nós");
        return ResponseEntity.ok(aboutPageService.getAdminAboutPage());
    }

    @PutMapping({"/api/v1/admin/institutional/about", "/admin/institutional/about"})
    public ResponseEntity<AboutPageDto> updateAboutPageSettings(@Valid @RequestBody AboutPageDto dto) {
        log.info("Admin atualizando textos da página institucional Sobre Nós");
        return ResponseEntity.ok(aboutPageService.updateAboutPage(dto));
    }
}

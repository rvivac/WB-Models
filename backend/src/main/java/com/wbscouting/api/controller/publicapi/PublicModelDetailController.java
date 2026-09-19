package com.wbscouting.api.controller.publicapi;

import com.wbscouting.api.dto.publicapi.ModelDetailPublicDto;
import com.wbscouting.api.service.publicapi.PublicModelDetailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping({"/public/models", "/api/v1/public/models"})
@RequiredArgsConstructor
public class PublicModelDetailController {

    private final PublicModelDetailService publicModelDetailService;

    @GetMapping("/{id}")
    public ResponseEntity<ModelDetailPublicDto> getModelDetail(@PathVariable UUID id) {
        log.info("Requisição pública de detalhes de modelo ID: {}", id);

        ModelDetailPublicDto detail = publicModelDetailService.getModelDetail(id);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(180, TimeUnit.SECONDS).cachePublic())
                .body(detail);
    }
}

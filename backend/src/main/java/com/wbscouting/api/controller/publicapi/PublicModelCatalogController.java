package com.wbscouting.api.controller.publicapi;

import com.wbscouting.api.dto.publicapi.ModelCardPublicDto;
import com.wbscouting.api.dto.publicapi.PageResponseDto;
import com.wbscouting.api.enums.GenderType;
import com.wbscouting.api.service.publicapi.PublicModelCatalogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping({"/public/models", "/api/v1/public/models"})
@RequiredArgsConstructor
public class PublicModelCatalogController {

    private final PublicModelCatalogService publicModelCatalogService;

    @GetMapping
    public ResponseEntity<PageResponseDto<ModelCardPublicDto>> listModels(
            @RequestParam(required = false) GenderType gender,
            @RequestParam(required = false) Boolean isStar,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size,
            @RequestParam(defaultValue = "stageName,asc") String sort
    ) {
        log.info("Recebida requisição pública de catálogo de modelos: gender={}, isStar={}, search='{}', page={}, size={}, sort='{}'",
                gender, isStar, search, page, size, sort);

        PageResponseDto<ModelCardPublicDto> response = publicModelCatalogService.listModels(
                gender, isStar, search, page, size, sort
        );

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(120, TimeUnit.SECONDS).cachePublic())
                .body(response);
    }
}

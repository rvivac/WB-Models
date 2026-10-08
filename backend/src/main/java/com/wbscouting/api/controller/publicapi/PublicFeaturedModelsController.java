package com.wbscouting.api.controller.publicapi;

import com.wbscouting.api.dto.model.FeaturedModelResponseDto;
import com.wbscouting.api.dto.publicapi.ModelCardPublicDto;
import com.wbscouting.api.service.model.FeaturedModelService;
import com.wbscouting.api.service.publicapi.PublicModelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping({"/api/v1/featured-models", "/featured-models", "/api/v1/public/featured-models", "/public/featured-models"})
@RequiredArgsConstructor
public class PublicFeaturedModelsController {

    private final FeaturedModelService featuredModelService;
    private final PublicModelService publicModelService;

    @GetMapping
    public ResponseEntity<List<FeaturedModelResponseDto>> getFeaturedModels() {
        List<FeaturedModelResponseDto> featured = featuredModelService.getFeaturedHomeModels();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(60, TimeUnit.SECONDS).cachePublic())
                .body(featured);
    }
}

package com.wbscouting.api.controller.admin;

import com.wbscouting.api.dto.model.FeaturedModelResponseDto;
import com.wbscouting.api.dto.model.FeaturedModelsReorderRequestDto;
import com.wbscouting.api.service.model.FeaturedModelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/featured-models")
@RequiredArgsConstructor
public class AdminFeaturedModelsController {

    private final FeaturedModelService featuredModelService;

    @GetMapping
    public ResponseEntity<List<FeaturedModelResponseDto>> getFeaturedHomeModels() {
        return ResponseEntity.ok(featuredModelService.getFeaturedHomeModels());
    }

    @PutMapping
    public ResponseEntity<List<FeaturedModelResponseDto>> updateFeaturedHomeModels(
            @Valid @RequestBody FeaturedModelsReorderRequestDto request) {
        return ResponseEntity.ok(featuredModelService.updateFeaturedHomeModels(request));
    }
}

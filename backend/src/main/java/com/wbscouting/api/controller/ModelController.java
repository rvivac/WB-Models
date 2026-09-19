package com.wbscouting.api.controller;

import com.wbscouting.api.dto.ModelDTO;
import com.wbscouting.api.enums.GenderType;
import com.wbscouting.api.service.ModelService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/models")
@RequiredArgsConstructor
public class ModelController {

    private final ModelService modelService;

    @GetMapping
    public ResponseEntity<Page<ModelDTO.SummaryResponse>> listModels(
            @RequestParam(required = false) GenderType gender,
            @RequestParam(required = false) Boolean isStar,
            @PageableDefault(size = 12, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(modelService.listModels(gender, isStar, pageable));
    }

    @GetMapping("/featured-home")
    public ResponseEntity<List<ModelDTO.SummaryResponse>> getFeaturedHomeModels() {
        return ResponseEntity.ok(modelService.getFeaturedHomeModels());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ModelDTO.DetailResponse> getModelById(@PathVariable UUID id) {
        return ResponseEntity.ok(modelService.getModelById(id));
    }
}

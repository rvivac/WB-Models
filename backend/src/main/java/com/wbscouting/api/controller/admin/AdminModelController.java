package com.wbscouting.api.controller.admin;

import com.wbscouting.api.dto.model.*;
import com.wbscouting.api.enums.GenderType;
import com.wbscouting.api.service.model.ModelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/admin/models")
@RequiredArgsConstructor
public class AdminModelController {

    private final ModelService modelService;

    @PostMapping
    public ResponseEntity<ModelAdminResponseDto> createModel(@Valid @RequestBody ModelCreateRequestDto request) {
        ModelAdminResponseDto createdModel = modelService.createModel(request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdModel.getId())
                .toUri();

        return ResponseEntity.created(location).body(createdModel);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ModelAdminResponseDto> updateModel(
            @PathVariable UUID id,
            @Valid @RequestBody ModelUpdateRequestDto request) {
        return ResponseEntity.ok(modelService.updateModel(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ModelAdminResponseDto> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody ModelStatusPatchDto request) {
        return ResponseEntity.ok(modelService.updateStatus(id, request));
    }

    @PatchMapping("/{id}/star")
    public ResponseEntity<ModelAdminResponseDto> updateStar(
            @PathVariable UUID id,
            @Valid @RequestBody ModelStarPatchDto request) {
        return ResponseEntity.ok(modelService.updateStar(id, request));
    }

    @PatchMapping("/{id}/featured")
    public ResponseEntity<ModelAdminResponseDto> updateFeatured(
            @PathVariable UUID id,
            @Valid @RequestBody ModelFeaturedPatchDto request) {
        return ResponseEntity.ok(modelService.updateFeatured(id, request));
    }

    @GetMapping
    public ResponseEntity<Page<ModelAdminResponseDto>> listAdminModels(
            @RequestParam(required = false) GenderType gender,
            @RequestParam(required = false) Boolean isStar,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(modelService.listAdminModels(gender, isStar, isActive, search, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ModelAdminResponseDto> getAdminModelById(@PathVariable UUID id) {
        return ResponseEntity.ok(modelService.getAdminModelById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteModel(@PathVariable UUID id) {
        modelService.deleteModel(id);
        return ResponseEntity.noContent().build();
    }
}

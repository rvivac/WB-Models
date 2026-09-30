package com.wbscouting.api.controller.admin;

import com.wbscouting.api.dto.media.ModelCompositeResponseDto;
import com.wbscouting.api.service.media.ModelMediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/admin/models/{modelId}/composite")
@RequiredArgsConstructor
public class AdminModelCompositeController {

    private final ModelMediaService modelMediaService;

    @GetMapping
    public ResponseEntity<ModelCompositeResponseDto> getComposite(@PathVariable UUID modelId) {
        ModelCompositeResponseDto composite = modelMediaService.getComposite(modelId);
        return ResponseEntity.ok(composite);
    }

    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ModelCompositeResponseDto> uploadOrReplaceComposite(
            @PathVariable UUID modelId,
            @RequestParam("file") MultipartFile file) {
        ModelCompositeResponseDto response = modelMediaService.uploadOrReplaceComposite(modelId, file);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteComposite(@PathVariable UUID modelId) {
        modelMediaService.deleteComposite(modelId);
        return ResponseEntity.noContent().build();
    }
}

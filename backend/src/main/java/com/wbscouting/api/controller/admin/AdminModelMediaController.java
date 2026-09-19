package com.wbscouting.api.controller.admin;

import com.wbscouting.api.dto.media.MediaReorderRequestDto;
import com.wbscouting.api.dto.media.MediaUploadResponseDto;
import com.wbscouting.api.enums.MediaType;
import com.wbscouting.api.service.media.ModelMediaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/models/{modelId}/media")
@RequiredArgsConstructor
public class AdminModelMediaController {

    private final ModelMediaService modelMediaService;

    @PostMapping(consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MediaUploadResponseDto> uploadMedia(
            @PathVariable UUID modelId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("mediaType") MediaType mediaType,
            @RequestParam(value = "isCover", required = false, defaultValue = "false") boolean isCover) {

        MediaUploadResponseDto response = modelMediaService.uploadMedia(modelId, mediaType, isCover, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping(value = "/reorder", consumes = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> reorderMedia(
            @PathVariable UUID modelId,
            @Valid @RequestBody MediaReorderRequestDto reorderDto) {

        modelMediaService.reorderMedia(modelId, reorderDto);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{mediaId}/cover")
    public ResponseEntity<Void> setCoverMedia(
            @PathVariable UUID modelId,
            @PathVariable UUID mediaId) {

        modelMediaService.setCoverMedia(modelId, mediaId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{mediaId}")
    public ResponseEntity<Void> deleteMedia(
            @PathVariable UUID modelId,
            @PathVariable UUID mediaId) {

        modelMediaService.deleteMedia(modelId, mediaId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<MediaUploadResponseDto>> listMedia(@PathVariable UUID modelId) {
        List<MediaUploadResponseDto> mediaList = modelMediaService.listModelMedia(modelId);
        return ResponseEntity.ok(mediaList);
    }
}

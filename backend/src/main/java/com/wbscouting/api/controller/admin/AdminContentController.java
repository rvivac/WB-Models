package com.wbscouting.api.controller.admin;

import com.wbscouting.api.dto.content.AssetUploadResponseDto;
import com.wbscouting.api.dto.content.SiteContentAdminDto;
import com.wbscouting.api.dto.content.SiteContentUpdateRequestDto;
import com.wbscouting.api.entity.Admin;
import com.wbscouting.api.service.content.SiteContentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/admin/content", "/admin/content"})
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CONTENT_ADMIN')")
@RequiredArgsConstructor
public class AdminContentController {

    private final SiteContentService siteContentService;

    @GetMapping
    public ResponseEntity<List<SiteContentAdminDto>> getAllAdminContent() {
        List<SiteContentAdminDto> contents = siteContentService.getAllAdminContent();
        return ResponseEntity.ok(contents);
    }

    @PutMapping("/{sectionKey}")
    public ResponseEntity<SiteContentAdminDto> updateContent(
            @PathVariable String sectionKey,
            @Valid @RequestBody SiteContentUpdateRequestDto request,
            Authentication authentication) {

        UUID adminId = null;
        Authentication auth = authentication != null ? authentication : SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Admin admin) {
            adminId = admin.getId();
        }

        SiteContentAdminDto updated = siteContentService.updateContent(sectionKey, request, adminId);
        return ResponseEntity.ok(updated);
    }

    @PostMapping(value = "/assets/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AssetUploadResponseDto> uploadAsset(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", required = false, defaultValue = "assets") String folder) {

        AssetUploadResponseDto response = siteContentService.uploadAsset(file, folder);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}

package com.wbscouting.api.controller.admin;

import com.wbscouting.api.dto.admin.candidate.CandidateDetailAdminDto;
import com.wbscouting.api.dto.admin.candidate.CandidateListItemAdminDto;
import com.wbscouting.api.dto.admin.candidate.CandidateNotesUpdateDto;
import com.wbscouting.api.dto.admin.candidate.CandidateStatusUpdateDto;
import com.wbscouting.api.dto.common.PageResponseDto;
import com.wbscouting.api.enums.CandidateStatus;
import com.wbscouting.api.service.candidate.AdminCandidateQueryService;
import com.wbscouting.api.service.candidate.AdminCandidateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/admin/candidates", "/admin/candidates"})
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CONTENT_ADMIN')")
@RequiredArgsConstructor
public class AdminCandidateController {

    private final AdminCandidateQueryService adminCandidateQueryService;
    private final AdminCandidateService adminCandidateService;

    @GetMapping
    public ResponseEntity<PageResponseDto<CandidateListItemAdminDto>> listCandidates(
            @RequestParam(required = false) CandidateStatus status,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean isMinor,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageResponseDto<CandidateListItemAdminDto> result = adminCandidateQueryService.listCandidates(
                status, gender, search, isMinor, page, size
        );
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CandidateDetailAdminDto> getCandidateDetail(@PathVariable UUID id) {
        CandidateDetailAdminDto detail = adminCandidateQueryService.getCandidateDetail(id);
        return ResponseEntity.ok(detail);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<CandidateDetailAdminDto> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody CandidateStatusUpdateDto request) {

        CandidateDetailAdminDto updated = adminCandidateService.updateStatus(id, request);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{id}/notes")
    public ResponseEntity<CandidateDetailAdminDto> updateNotes(
            @PathVariable UUID id,
            @Valid @RequestBody CandidateNotesUpdateDto request) {

        CandidateDetailAdminDto updated = adminCandidateService.updateNotes(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCandidate(@PathVariable UUID id) {
        adminCandidateQueryService.deleteCandidate(id);
        return ResponseEntity.noContent().build();
    }
}

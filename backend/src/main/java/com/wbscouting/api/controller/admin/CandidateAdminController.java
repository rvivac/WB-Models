package com.wbscouting.api.controller.admin;

import com.wbscouting.api.dto.CandidateSubmissionResponseDto;
import com.wbscouting.api.dto.submission.CandidateStatusUpdateDto;
import com.wbscouting.api.entity.Admin;
import com.wbscouting.api.entity.CandidateSubmission;
import com.wbscouting.api.enums.SubmissionGender;
import com.wbscouting.api.enums.SubmissionStatus;
import com.wbscouting.api.service.submission.CandidateSubmissionAdminService;
import com.wbscouting.api.specification.CandidateSubmissionSpecification;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping({"/api/v1/admin/submissions", "/admin/submissions"})
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CONTENT_ADMIN', 'ADMIN')")
@RequiredArgsConstructor
public class CandidateAdminController {

    private final CandidateSubmissionAdminService adminService;

    @GetMapping
    public ResponseEntity<Page<CandidateSubmissionResponseDto>> listSubmissions(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) SubmissionStatus status,
            @RequestParam(required = false) SubmissionGender gender,
            @RequestParam(required = false) BigDecimal minHeight,
            @RequestParam(required = false) BigDecimal maxHeight,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        log.info("Consulta administrativa de candidaturas. Status: {}, Search: {}, Pageable: {}", status, search, pageable);

        Specification<CandidateSubmission> spec = CandidateSubmissionSpecification.filter(
                search, status, gender, minHeight, maxHeight, startDate, endDate
        );

        Page<CandidateSubmissionResponseDto> result = adminService.listSubmissions(spec, pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CandidateSubmissionResponseDto> getSubmissionById(@PathVariable UUID id) {
        log.info("Obtenção de dossiê da candidatura ID: {}", id);
        CandidateSubmissionResponseDto submission = adminService.getSubmissionById(id);
        return ResponseEntity.ok(submission);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<CandidateSubmissionResponseDto> updateSubmissionStatus(
            @PathVariable UUID id,
            @Valid @RequestBody CandidateStatusUpdateDto updateDto,
            Authentication authentication
    ) {
        String reviewerName = "Administrador";
        Authentication auth = authentication != null ? authentication : SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            if (auth.getPrincipal() instanceof Admin admin) {
                reviewerName = admin.getName() != null ? admin.getName() : admin.getEmail();
            } else if (auth.getName() != null) {
                reviewerName = auth.getName();
            }
        }

        log.info("Atualização de status da candidatura ID: {} para {} pelo revisor {}", id, updateDto.getStatus(), reviewerName);
        CandidateSubmissionResponseDto updated = adminService.updateSubmissionStatus(id, updateDto, reviewerName);
        return ResponseEntity.ok(updated);
    }
}

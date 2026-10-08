package com.wbscouting.api.controller.admin;

import com.wbscouting.api.config.SupabaseProperties;
import com.wbscouting.api.dto.admin.candidate.CandidateApplicationSummaryDto;
import com.wbscouting.api.dto.admin.candidate.CandidateDecisionRequestDto;
import com.wbscouting.api.dto.admin.candidate.CandidateDetailResponseDto;
import com.wbscouting.api.entity.Admin;
import com.wbscouting.api.entity.Candidate;
import com.wbscouting.api.entity.CandidateSubmission;
import com.wbscouting.api.enums.CandidateStatus;
import com.wbscouting.api.enums.SubmissionGender;
import com.wbscouting.api.enums.SubmissionStatus;
import com.wbscouting.api.exception.ResourceNotFoundException;
import com.wbscouting.api.repository.CandidatePhotoRepository;
import com.wbscouting.api.repository.CandidateRepository;
import com.wbscouting.api.repository.CandidateSubmissionRepository;
import com.wbscouting.api.service.candidate.AdminCandidateService;
import com.wbscouting.api.service.storage.StorageService;
import com.wbscouting.api.service.submission.CandidateSubmissionAdminService;
import com.wbscouting.api.specification.AdminCandidateSpecification;
import com.wbscouting.api.specification.CandidateSubmissionSpecification;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import com.wbscouting.api.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.wbscouting.api.security.audit.AuditAction;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping({"/api/v1/admin/applications", "/admin/applications"})
@PreAuthorize("hasAnyRole('WEBMASTER', 'SUPER_ADMIN', 'CONTENT_ADMIN', 'ADMIN')")
@RequiredArgsConstructor
public class AdminApplicationController {

    private final CandidateRepository candidateRepository;
    private final CandidatePhotoRepository candidatePhotoRepository;
    private final AdminCandidateService adminCandidateService;
    private final CandidateSubmissionRepository submissionRepository;
    private final CandidateSubmissionAdminService candidateSubmissionAdminService;
    private final StorageService storageService;
    private final SupabaseProperties supabaseProperties;

    private String resolveBucketBaseUrl() {
        try {
            String bucket = supabaseProperties.resolveBucketCandidates();
            return storageService.getPublicUrl(bucket, "");
        } catch (Exception e) {
            return "https://stmytwsdlonpnirqiufq.supabase.co/storage/v1/object/public/candidates-uploads";
        }
    }

    @Transactional(readOnly = true)
    @GetMapping
    public ResponseEntity<Page<CandidateApplicationSummaryDto>> listApplications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) SubmissionStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) SubmissionGender gender,
            @RequestParam(required = false) Boolean isMinor,
            @RequestParam(required = false, defaultValue = "false") Boolean includePromoted,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection
    ) {
        log.info("Consulta administrativa tabular de candidaturas (tabela candidates). Status: {}, Search: {}, isMinor: {}, Page: {}, Size: {}",
                status, search, isMinor, page, size);

        Sort.Direction direction = "ASC".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String mappedSortBy = switch (sortBy) {
            case "height" -> "heightCm";
            case "fullName" -> "fullName";
            case "city" -> "city";
            case "age" -> "age";
            case "status" -> "status";
            default -> "createdAt";
        };

        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(direction, mappedSortBy));

        CandidateStatus candidateStatus = null;
        if (status != null) {
            try { candidateStatus = CandidateStatus.valueOf(status.name()); } catch (Exception ignored) {}
        }
        String candidateGender = (gender != null) ? gender.name() : null;

        Specification<Candidate> candidateSpec = AdminCandidateSpecification.filter(candidateStatus, search, candidateGender, isMinor);
        Page<Candidate> candidatePage = candidateRepository.findAll(candidateSpec, pageable);

        String bucketUrl = resolveBucketBaseUrl();
        if (candidatePage.hasContent() || candidateRepository.count() > 0) {
            Page<CandidateApplicationSummaryDto> result = candidatePage
                    .map(c -> CandidateApplicationSummaryDto.fromCandidate(c, bucketUrl));
            return ResponseEntity.ok(result);
        }

        // Fallback para banco legado (candidate_submissions) caso a tabela candidates esteja vazia
        Specification<CandidateSubmission> legacySpec = CandidateSubmissionSpecification.filter(
                search, status, gender, isMinor, null, null, null, null, includePromoted
        );
        Page<CandidateApplicationSummaryDto> legacyResult = submissionRepository.findAll(legacySpec, pageable)
                .map(CandidateApplicationSummaryDto::fromEntity);
        return ResponseEntity.ok(legacyResult);
    }

    @Transactional(readOnly = true)
    @GetMapping("/counts")
    public ResponseEntity<Map<String, Long>> getCounts() {
        long pending = candidateRepository.countByStatus(CandidateStatus.PENDING);
        long approved = candidateRepository.countByStatus(CandidateStatus.APPROVED);
        long rejected = candidateRepository.countByStatus(CandidateStatus.REJECTED);
        long archived = candidateRepository.countByStatus(CandidateStatus.ARCHIVED);
        long total = pending + approved + rejected;

        if (pending == 0 && approved == 0 && rejected == 0 && archived == 0 && submissionRepository.count() > 0) {
            pending = submissionRepository.countByStatusAndConvertedToModelIdIsNull(SubmissionStatus.PENDING);
            approved = submissionRepository.countByStatusAndConvertedToModelIdIsNull(SubmissionStatus.APPROVED);
            rejected = submissionRepository.countByStatusAndConvertedToModelIdIsNull(SubmissionStatus.REJECTED);
            archived = submissionRepository.countByStatus(SubmissionStatus.ARCHIVED);
            total = submissionRepository.countByConvertedToModelIdIsNull();
        }

        return ResponseEntity.ok(Map.of(
                "pending", pending,
                "approved", approved,
                "rejected", rejected,
                "archived", archived,
                "total", total
        ));
    }

    @Transactional(readOnly = true)
    @GetMapping("/{id}")
    public ResponseEntity<CandidateDetailResponseDto> getApplicationById(@PathVariable UUID id) {
        log.info("Buscando detalhes da candidatura ID: {}", id);
        String bucketUrl = resolveBucketBaseUrl();
        Optional<Candidate> opt = candidateRepository.findWithPhotosById(id);
        if (opt.isPresent()) {
            return ResponseEntity.ok(CandidateDetailResponseDto.fromCandidate(opt.get(), bucketUrl));
        }

        CandidateSubmission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidatura", "id", id));
        return ResponseEntity.ok(CandidateDetailResponseDto.fromEntity(submission));
    }

    @Transactional
    @PatchMapping("/{id}/decision")
    @AuditAction(action = "DECISION", resource = "SCOUTING_CANDIDATE", description = "Triagem de candidatura de modelo")
    public ResponseEntity<CandidateDetailResponseDto> updateDecision(
            @PathVariable UUID id,
            @Valid @RequestBody CandidateDecisionRequestDto decisionDto,
            Authentication authentication
    ) {
        log.info("Decisão de triagem para candidatura ID: {}, Status: {}", id, decisionDto.getStatus());
        String bucketUrl = resolveBucketBaseUrl();
        Optional<Candidate> opt = candidateRepository.findWithPhotosById(id);
        if (opt.isPresent()) {
            Candidate candidate = opt.get();
            try {
                candidate.setStatus(CandidateStatus.valueOf(decisionDto.getStatus().name()));
            } catch (Exception ignored) {}
            if (decisionDto.getInternalNotes() != null) {
                candidate.setInternalNotes(decisionDto.getInternalNotes());
            }
            candidate.setUpdatedAt(OffsetDateTime.now());
            Candidate saved = candidateRepository.save(candidate);
            return ResponseEntity.ok(CandidateDetailResponseDto.fromCandidate(saved, bucketUrl));
        }

        CandidateSubmission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidatura", "id", id));

        submission.setStatus(decisionDto.getStatus());
        if (decisionDto.getInternalNotes() != null) {
            submission.setFeedbackNotes(decisionDto.getInternalNotes());
        }

        String reviewerName = extractReviewerName(authentication);
        submission.setReviewedBy(reviewerName);
        submission.setReviewedAt(OffsetDateTime.now());

        CandidateSubmission saved = submissionRepository.save(submission);
        return ResponseEntity.ok(CandidateDetailResponseDto.fromEntity(saved));
    }

    @PostMapping({"/{id}/promote", "/{id}/promote-to-model"})
    @AuditAction(action = "PROMOTE", resource = "SCOUTING_CANDIDATE", description = "Promoção de candidato para elenco de modelos")
    public ResponseEntity<ApiResponse<Map<String, Object>>> promoteCandidateToModel(
            @PathVariable("id") UUID id,
            @RequestParam(required = false, defaultValue = "true") Boolean activateImmediately,
            Authentication authentication
    ) {
        String reviewerName = extractReviewerName(authentication);
        log.info("Ação operacional de promoção para modelo da candidatura ID: {} por {}", id, reviewerName);
        UUID modelId = adminCandidateService.promoteToModel(id, activateImmediately);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Candidato promovido a modelo com sucesso.", Map.of("modelId", modelId)));
    }

    @DeleteMapping("/{id}")
    @AuditAction(action = "ARCHIVE", resource = "SCOUTING_CANDIDATE", description = "Mover candidatura para Arquivo Morto")
    public ResponseEntity<Void> deleteApplication(@PathVariable UUID id) {
        log.info("Iniciando arquivamento (soft delete) da candidatura ID: {}", id);
        adminCandidateService.deleteCandidate(id);
        log.info("Candidatura ID: {} movida para Arquivo Morto com sucesso.", id);
        return ResponseEntity.noContent().build();
    }

    private void purgeCandidateFiles(CandidateSubmission submission) {
        String candidatesBucket = supabaseProperties.getBuckets().getCandidatesUploads();
        if (candidatesBucket == null || candidatesBucket.isBlank()) {
            candidatesBucket = "candidates-uploads";
        }

        deleteStorageFile(candidatesBucket, submission.getFacePhotoUrl());
        deleteStorageFile(candidatesBucket, submission.getProfilePhotoUrl());
        deleteStorageFile(candidatesBucket, submission.getFullBodyPhotoUrl());

        // Também garantir purga se estiver em wb-media-assets
        deleteStorageFile("wb-media-assets", submission.getFacePhotoUrl());
        deleteStorageFile("wb-media-assets", submission.getProfilePhotoUrl());
        deleteStorageFile("wb-media-assets", submission.getFullBodyPhotoUrl());
    }

    private void deleteStorageFile(String bucket, String urlOrPath) {
        if (urlOrPath == null || urlOrPath.isBlank()) return;
        try {
            String relativePath = extractRelativePath(urlOrPath, bucket);
            if (relativePath != null && !relativePath.isBlank()) {
                storageService.deleteFile(bucket, relativePath);
            }
        } catch (Exception e) {
            log.warn("Exclusão de arquivo no bucket '{}' (URL/Path: {}) ignorada ou não encontrado: {}",
                    bucket, urlOrPath, e.getMessage());
        }
    }

    private String extractRelativePath(String urlOrPath, String bucket) {
        if (urlOrPath == null || urlOrPath.isBlank()) return null;

        String path = urlOrPath;
        // Se for URL com query params, remove
        int queryIdx = path.indexOf('?');
        if (queryIdx >= 0) {
            path = path.substring(0, queryIdx);
        }

        // Se contiver /{bucket}/
        String bucketMarker = "/" + bucket + "/";
        int bucketIdx = path.indexOf(bucketMarker);
        if (bucketIdx >= 0) {
            return path.substring(bucketIdx + bucketMarker.length());
        }

        // Se contiver "submissions/"
        int subIdx = path.indexOf("submissions/");
        if (subIdx >= 0) {
            return path.substring(subIdx);
        }

        // Se for caminho http genérico
        if (path.startsWith("http://") || path.startsWith("https://")) {
            int lastSlash = path.lastIndexOf('/');
            if (lastSlash >= 0) {
                return path.substring(lastSlash + 1);
            }
        }

        return path.startsWith("/") ? path.substring(1) : path;
    }

    private String extractReviewerName(Authentication authentication) {
        Authentication auth = authentication != null ? authentication : SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            if (auth.getPrincipal() instanceof Admin admin) {
                return admin.getName() != null ? admin.getName() : admin.getEmail();
            } else if (auth.getName() != null) {
                return auth.getName();
            }
        }
        return "Booker / Scouting Desk";
    }
}

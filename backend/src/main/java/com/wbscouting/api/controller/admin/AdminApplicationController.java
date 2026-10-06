package com.wbscouting.api.controller.admin;

import com.wbscouting.api.config.SupabaseProperties;
import com.wbscouting.api.dto.admin.candidate.CandidateApplicationSummaryDto;
import com.wbscouting.api.dto.admin.candidate.CandidateDecisionRequestDto;
import com.wbscouting.api.dto.admin.candidate.CandidateDetailResponseDto;
import com.wbscouting.api.entity.Admin;
import com.wbscouting.api.entity.CandidateSubmission;
import com.wbscouting.api.enums.SubmissionGender;
import com.wbscouting.api.enums.SubmissionStatus;
import com.wbscouting.api.exception.ResourceNotFoundException;
import com.wbscouting.api.repository.CandidateSubmissionRepository;
import com.wbscouting.api.service.storage.StorageService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import com.wbscouting.api.dto.ApiResponse;
import com.wbscouting.api.dto.model.ModelResponseDto;
import com.wbscouting.api.service.submission.CandidateSubmissionAdminService;
// TASK CIRURGICA #1: Import CORRIGIDO (anteriormente estava apontando para service.submission - package errado)
import com.wbscouting.api.specification.CandidateSubmissionSpecification;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.wbscouting.api.security.audit.AuditAction;
import org.springframework.web.bind.annotation.*;

// ============================================================
// Logger MANUAL (lombok @Slf4j NAO gera field no MavenWrapper 3.6.3 Render)
// ============================================================
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

// @Slf4j comentado: substituido pelo logger static abaixo (evita erro cannot find symbol variable log)
// @Slf4j
@RestController
@RequestMapping({"/api/v1/admin/applications", "/admin/applications"})
@PreAuthorize("hasAnyRole('WEBMASTER', 'SUPER_ADMIN', 'CONTENT_ADMIN', 'ADMIN')")
@RequiredArgsConstructor
public class AdminApplicationController {

    // 🔥 Logger manual (substitui @Slf4j - annotation processor do Lombok nao funciona no mvnw 3.6.3)
    private static final Logger log = LoggerFactory.getLogger(AdminApplicationController.class);

    private final CandidateSubmissionRepository submissionRepository;
    private final CandidateSubmissionAdminService candidateSubmissionAdminService;
    private final StorageService storageService;
    private final SupabaseProperties supabaseProperties;

    @GetMapping
    public ResponseEntity<Page<CandidateApplicationSummaryDto>> listApplications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) SubmissionStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) SubmissionGender gender,
            @RequestParam(required = false) Boolean isMinor,
            // 🆕 REGRA 1 e 3: Por PADRAO = false (apenas fichas ainda NAO promovidas para Casting ficam no Scouting Desk).
            // Permite filtro historico ?includePromoted=true se quiser ver todos (incluindo os ja promovidos de dados antigos).
            @RequestParam(required = false, defaultValue = "false") Boolean includePromoted,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection
    ) {
        log.info("Consulta administrativa tabular de candidaturas. Status: {}, Search: {}, isMinor: {}, includePromoted={}, Page: {}, Size: {}",
                status, search, isMinor, includePromoted, page, size);

        Sort.Direction direction = "ASC".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String mappedSortBy = switch (sortBy) {
            case "height" -> "height";
            case "fullName" -> "fullName";
            case "city" -> "city";
            case "age" -> "age";
            case "status" -> "status";
            default -> "createdAt";
        };

        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(direction, mappedSortBy));

        // 🆕 REGRA 1 e 3: Se includePromoted=false (DEFAULT), oculta quaisquer registros antigos que JA TIVEREM sido
        //    promovidos (convertedToModelId != null). Os status APPROVED e REJECTED normais (sem promotion) CONTINUAM
        //    aparecendo (permanentes na tabela scouting desk por tempo indeterminado).
        Specification<CandidateSubmission> spec = CandidateSubmissionSpecification.filter(
                search, status, gender, isMinor, null, null, null, null, includePromoted
        );

        Page<CandidateApplicationSummaryDto> result = submissionRepository.findAll(spec, pageable)
                .map(CandidateApplicationSummaryDto::fromEntity);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/counts")
    public ResponseEntity<Map<String, Long>> getCounts() {
        // 🆕 REGRA 3: counts da aba principal consideram apenas os NAO promovidos (ainda no scouting desk).
        long pending = submissionRepository.countByStatusAndConvertedToModelIdIsNull(SubmissionStatus.PENDING);
        long approved = submissionRepository.countByStatusAndConvertedToModelIdIsNull(SubmissionStatus.APPROVED);
        long rejected = submissionRepository.countByStatusAndConvertedToModelIdIsNull(SubmissionStatus.REJECTED);
        long total = submissionRepository.countByConvertedToModelIdIsNull();

        return ResponseEntity.ok(Map.of(
                "pending", pending,
                "approved", approved,
                "rejected", rejected,
                "total", total
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CandidateDetailResponseDto> getApplicationById(@PathVariable UUID id) {
        log.info("Buscando detalhes da candidatura ID: {}", id);
        CandidateSubmission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidatura", "id", id));

        return ResponseEntity.ok(CandidateDetailResponseDto.fromEntity(submission));
    }

    @PatchMapping("/{id}/decision")
    @AuditAction(action = "DECISION", resource = "SCOUTING_CANDIDATE", description = "Triagem de candidatura de modelo")
    public ResponseEntity<CandidateDetailResponseDto> updateDecision(
            @PathVariable UUID id,
            @Valid @RequestBody CandidateDecisionRequestDto decisionDto,
            Authentication authentication
    ) {
        log.info("Decisão de triagem para candidatura ID: {}, Status: {}", id, decisionDto.getStatus());
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

    @PostMapping("/{id}/promote")
    @AuditAction(action = "PROMOTE", resource = "SCOUTING_CANDIDATE", description = "Promoção de candidato para elenco de modelos")
    public ResponseEntity<ApiResponse<ModelResponseDto>> promoteCandidateToModel(
            @PathVariable("id") UUID id,
            @RequestParam(required = false, defaultValue = "true") Boolean activateImmediately,
            Authentication authentication
    ) {
        String reviewerName = extractReviewerName(authentication);
        log.info("Ação operacional de promoção para modelo da candidatura ID: {} por {}", id, reviewerName);
        ModelResponseDto createdModel = candidateSubmissionAdminService.promoteCandidateToModel(id, reviewerName, activateImmediately);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Candidato promovido a modelo com sucesso.", createdModel));
    }

    @DeleteMapping("/{id}")
    @AuditAction(action = "DELETE", resource = "SCOUTING_CANDIDATE", description = "Exclusão permanente de candidatura (Purge)")
    public ResponseEntity<Void> deleteApplication(@PathVariable UUID id) {
        log.info("Iniciando exclusão permanente defensiva (Purge LGPD) da candidatura ID: {}", id);
        CandidateSubmission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidatura", "id", id));

        // Purge dos binários no Supabase Storage
        purgeCandidateFiles(submission);

        submissionRepository.delete(submission);
        log.info("Candidatura ID: {} e arquivos associados purgados com sucesso do banco e storage.", id);
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

package com.wbscouting.api.service.submission;

import com.wbscouting.api.dto.CandidateSubmissionResponseDto;
import com.wbscouting.api.dto.submission.CandidateStatusUpdateDto;
import com.wbscouting.api.entity.CandidateSubmission;
import com.wbscouting.api.enums.SubmissionStatus;
import com.wbscouting.api.event.CandidateApprovedEvent;
import com.wbscouting.api.exception.ResourceNotFoundException;
import com.wbscouting.api.repository.CandidateSubmissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CandidateSubmissionAdminServiceImpl implements CandidateSubmissionAdminService {

    private final CandidateSubmissionRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(readOnly = true)
    public Page<CandidateSubmissionResponseDto> listSubmissions(Specification<CandidateSubmission> spec, Pageable pageable) {
        log.debug("Listando candidaturas com paginação e filtros dinâmicos. Pageable: {}", pageable);
        return repository.findAll(spec, pageable)
                .map(CandidateSubmissionResponseDto::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public CandidateSubmissionResponseDto getSubmissionById(UUID id) {
        log.debug("Buscando detalhes da candidatura ID: {}", id);
        CandidateSubmission submission = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidatura não encontrada com ID: " + id));
        return CandidateSubmissionResponseDto.fromEntity(submission);
    }

    @Override
    @Transactional
    public CandidateSubmissionResponseDto updateSubmissionStatus(UUID id, CandidateStatusUpdateDto updateDto, String reviewer) {
        log.info("Atualizando status da candidatura ID: {} para {} por {}", id, updateDto.getStatus(), reviewer);

        CandidateSubmission submission = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidatura não encontrada com ID: " + id));

        submission.setStatus(updateDto.getStatus());
        submission.setReviewedBy(StringUtils.hasText(reviewer) ? reviewer : "Sistema");
        submission.setReviewedAt(OffsetDateTime.now());

        if (StringUtils.hasText(updateDto.getFeedbackNotes())) {
            submission.setFeedbackNotes(updateDto.getFeedbackNotes().trim());
        }

        CandidateSubmission saved = repository.save(submission);

        if (saved.getStatus() == SubmissionStatus.APPROVED) {
            log.info("Disparando CandidateApprovedEvent para a candidatura ID: {}", saved.getId());
            eventPublisher.publishEvent(new CandidateApprovedEvent(this, saved, reviewer));
        }

        return CandidateSubmissionResponseDto.fromEntity(saved);
    }
}

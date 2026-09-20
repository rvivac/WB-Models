package com.wbscouting.api.service.submission;

import com.wbscouting.api.dto.CandidateSubmissionResponseDto;
import com.wbscouting.api.dto.submission.CandidateStatusUpdateDto;
import com.wbscouting.api.entity.CandidateSubmission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public interface CandidateSubmissionAdminService {

    Page<CandidateSubmissionResponseDto> listSubmissions(Specification<CandidateSubmission> spec, Pageable pageable);

    CandidateSubmissionResponseDto getSubmissionById(UUID id);

    CandidateSubmissionResponseDto updateSubmissionStatus(UUID id, CandidateStatusUpdateDto updateDto, String reviewer);
}

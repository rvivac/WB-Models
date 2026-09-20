package com.wbscouting.api.repository;

import com.wbscouting.api.entity.CandidateSubmission;
import com.wbscouting.api.enums.SubmissionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CandidateSubmissionRepository extends JpaRepository<CandidateSubmission, UUID>, JpaSpecificationExecutor<CandidateSubmission> {

    Optional<CandidateSubmission> findByProtocol(String protocol);

    Page<CandidateSubmission> findByStatus(SubmissionStatus status, Pageable pageable);

    boolean existsByEmailAndStatus(String email, SubmissionStatus status);
}

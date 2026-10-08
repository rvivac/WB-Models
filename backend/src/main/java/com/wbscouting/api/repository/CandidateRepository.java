package com.wbscouting.api.repository;

import com.wbscouting.api.entity.Candidate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CandidateRepository extends JpaRepository<Candidate, UUID>, JpaSpecificationExecutor<Candidate> {

    Page<Candidate> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByStatus(com.wbscouting.api.enums.CandidateStatus status);

    @EntityGraph(attributePaths = {"photos"})
    Optional<Candidate> findWithPhotosById(UUID id);

    Optional<Candidate> findByProtocol(String protocol);

    @Override
    @EntityGraph(attributePaths = {"photos"})
    Page<Candidate> findAll(org.springframework.data.jpa.domain.Specification<Candidate> spec, Pageable pageable);
}

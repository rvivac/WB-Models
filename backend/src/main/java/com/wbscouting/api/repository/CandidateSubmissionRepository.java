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

    /**
     * Verificação pré-persistência de unicidade do protocolo.
     * Usado por ProtocolGeneratorService no laço anti-colisão.
     */
    boolean existsByProtocol(String protocol);

    Page<CandidateSubmission> findByStatus(SubmissionStatus status, Pageable pageable);

    long countByStatus(SubmissionStatus status);

    boolean existsByEmailAndStatus(String email, SubmissionStatus status);

    // 🆕 REGRAS 1/3: apenas registros AINDA no Scouting Desk (nao movidos para Casting) contam na grid e cards da Home Admin
    long countByStatusAndConvertedToModelIdIsNull(SubmissionStatus status);
    long countByConvertedToModelIdIsNull();
    long countByStatusAndConvertedToModelIdIsNotNull(SubmissionStatus status);
    long countByConvertedToModelIdIsNotNull();

    // 🆕 REGRA 2 validacao antes de apagar: confirma se o id existe
    boolean existsByIdAndConvertedToModelIdIsNotNull(UUID id);
}

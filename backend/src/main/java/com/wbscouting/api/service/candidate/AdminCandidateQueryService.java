package com.wbscouting.api.service.candidate;

import com.wbscouting.api.dto.admin.candidate.CandidateDetailAdminDto;
import com.wbscouting.api.dto.admin.candidate.CandidateListItemAdminDto;
import com.wbscouting.api.dto.common.PageResponseDto;
import com.wbscouting.api.enums.CandidateStatus;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AdminCandidateQueryService {

    /**
     * Consulta paginada com ordenação padrão por data de submissão decrescente (createdAt DESC).
     * Retorna PageResponseDto<CandidateListItemAdminDto> sem gerar signed URLs para máxima performance.
     */
    PageResponseDto<CandidateListItemAdminDto> listCandidates(
            CandidateStatus status,
            String gender,
            String search,
            Boolean isMinor,
            int page,
            int size
    );

    PageResponseDto<CandidateListItemAdminDto> listCandidates(
            CandidateStatus status,
            String gender,
            String search,
            Boolean isMinor,
            Pageable pageable
    );

    /**
     * Retorna a visão analítica completa da candidatura por ID com galeria de fotos
     * acompanhadas de Signed URLs temporárias (expiração de 900 segundos).
     */
    CandidateDetailAdminDto getCandidateDetail(UUID id);

    /**
     * Rotina de Double-Delete com integridade referencial:
     * Expurga os arquivos físicos no Supabase Storage e deleta o registro relacional no PostgreSQL.
     */
    void deleteCandidate(UUID id);
}

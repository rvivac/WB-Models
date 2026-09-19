package com.wbscouting.api.service.candidate;

import com.wbscouting.api.dto.admin.candidate.CandidateDetailAdminDto;
import com.wbscouting.api.dto.admin.candidate.CandidateListItemAdminDto;
import com.wbscouting.api.dto.admin.candidate.CandidateNotesUpdateDto;
import com.wbscouting.api.dto.admin.candidate.CandidateStatusUpdateDto;
import com.wbscouting.api.enums.CandidateStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AdminCandidateService {

    /**
     * Lista candidaturas de forma paginada com suporte a filtros dinâmicos.
     * Projeção simplificada sem geração de signed URLs na listagem massiva.
     */
    Page<CandidateListItemAdminDto> listCandidates(
            CandidateStatus status,
            String search,
            String gender,
            Boolean isMinor,
            Pageable pageable
    );

    /**
     * Retorna os detalhes completos de uma candidatura com as respectivas fotos
     * acompanhadas de URLs assinadas temporárias (Signed URLs) para visualização.
     */
    CandidateDetailAdminDto getCandidateDetail(UUID id);

    /**
     * Atualiza o status de triagem da candidatura.
     */
    CandidateDetailAdminDto updateStatus(UUID id, CandidateStatusUpdateDto dto);

    /**
     * Atualiza as anotações internas da agência sobre o candidato.
     */
    CandidateDetailAdminDto updateNotes(UUID id, CandidateNotesUpdateDto dto);

    /**
     * Realiza a exclusão segura da candidatura (remoção física de arquivos no Storage
     * e remoção relacional no banco de dados).
     */
    void deleteCandidate(UUID id);
}

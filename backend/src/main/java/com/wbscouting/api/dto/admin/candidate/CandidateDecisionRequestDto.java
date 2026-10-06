package com.wbscouting.api.dto.admin.candidate;

import com.wbscouting.api.enums.SubmissionStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// TASK testCompile Render Fix #1: Anotacoes Lombok completas exigidas
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateDecisionRequestDto {

    @NotNull(message = "O status da decisão é obrigatório (APPROVED ou REJECTED)")
    private SubmissionStatus status;

    private String internalNotes;

    // 🔥 Fallback manual para MavenWrapper 3.6.3 (annotation processor do Lombok pode nao rodar)
    // Construtor AllArgs fallback (redundante com @AllArgsConstructor, mas seguro)
    // Se @AllArgsConstructor gerar: nao tem duplicate pois o do Lombok substitui o manual? Wait:
    // Para NAO ter duplicata com @AllArgsConstructor, comentamos abaixo e mantemos apenas o builder manual fallback.
    // @AllArgsConstructor gera exatamente este construtor: 2 params (status, internalNotes).

    // Builder manual fallback (independente de @Builder gerar ou nao, .builder() existe)
    public static CandidateDecisionRequestDtoBuilder manualBuilder() { return new CandidateDecisionRequestDtoBuilder(); }
    public static class CandidateDecisionRequestDtoBuilder {
        private final CandidateDecisionRequestDto dto = new CandidateDecisionRequestDto();
        public CandidateDecisionRequestDtoBuilder status(SubmissionStatus s) { dto.setStatus(s); return this; }
        public CandidateDecisionRequestDtoBuilder internalNotes(String n) { dto.setInternalNotes(n); return this; }
        public CandidateDecisionRequestDto build() { return dto; }
    }

    public SubmissionStatus getStatus() { return status; }
    public void setStatus(SubmissionStatus status) { this.status = status; }
    public String getInternalNotes() { return internalNotes; }
    public void setInternalNotes(String internalNotes) { this.internalNotes = internalNotes; }
}
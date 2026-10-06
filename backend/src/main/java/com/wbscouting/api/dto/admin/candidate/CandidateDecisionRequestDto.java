package com.wbscouting.api.dto.admin.candidate;

import com.wbscouting.api.enums.SubmissionStatus;
import jakarta.validation.constraints.NotNull;

public class CandidateDecisionRequestDto {

    @NotNull(message = "O status da decisão é obrigatório (APPROVED ou REJECTED)")
    private SubmissionStatus status;

    private String internalNotes;

    public CandidateDecisionRequestDto() {}

    public SubmissionStatus getStatus() { return status; }
    public void setStatus(SubmissionStatus status) { this.status = status; }
    public String getInternalNotes() { return internalNotes; }
    public void setInternalNotes(String internalNotes) { this.internalNotes = internalNotes; }
}
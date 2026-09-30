package com.wbscouting.api.dto.admin.candidate;

import com.wbscouting.api.enums.SubmissionStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateDecisionRequestDto {

    @NotNull(message = "O status da decisão é obrigatório (APPROVED ou REJECTED)")
    private SubmissionStatus status;

    private String internalNotes;
}

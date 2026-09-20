package com.wbscouting.api.dto.submission;

import com.wbscouting.api.enums.SubmissionStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateStatusUpdateDto {

    @NotNull(message = "O novo status da candidatura é obrigatório")
    private SubmissionStatus status;

    @Size(max = 500, message = "As notas de feedback interno não podem exceder 500 caracteres")
    private String feedbackNotes;
}

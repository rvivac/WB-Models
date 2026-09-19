package com.wbscouting.api.dto.admin.candidate;

import com.wbscouting.api.enums.CandidateStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateStatusUpdateDto {

    @NotNull(message = "O status da candidatura é obrigatório.")
    private CandidateStatus status;
}

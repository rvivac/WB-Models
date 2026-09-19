package com.wbscouting.api.dto.admin.candidate;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateNotesUpdateDto {

    @JsonProperty("notes")
    @JsonAlias({"notes", "internalNotes"})
    private String notes;

    public String getInternalNotes() {
        return notes;
    }
}

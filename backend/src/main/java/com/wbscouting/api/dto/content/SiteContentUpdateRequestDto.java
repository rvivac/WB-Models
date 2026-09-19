package com.wbscouting.api.dto.content;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SiteContentUpdateRequestDto {

    @NotNull(message = "O payload em português (payloadPt) é obrigatório.")
    private Map<String, Object> payloadPt;

    @NotNull(message = "O payload em inglês (payloadEn) é obrigatório.")
    private Map<String, Object> payloadEn;

    private Map<String, Object> mediaUrls;
}

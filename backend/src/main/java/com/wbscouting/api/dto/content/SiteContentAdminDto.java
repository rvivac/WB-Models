package com.wbscouting.api.dto.content;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SiteContentAdminDto {

    private UUID id;
    private String sectionKey;
    private Map<String, Object> payloadPt;
    private Map<String, Object> payloadEn;
    private Map<String, Object> mediaUrls;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private UUID updatedBy;
}

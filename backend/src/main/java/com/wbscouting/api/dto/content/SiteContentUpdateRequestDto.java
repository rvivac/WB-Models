package com.wbscouting.api.dto.content;

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

    private Map<String, Object> payloadPt;
    private Map<String, Object> payloadEn;
    private Map<String, Object> mediaUrls;
}

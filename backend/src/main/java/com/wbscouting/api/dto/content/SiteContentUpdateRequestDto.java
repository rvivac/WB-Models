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

    // Campos planos opcionais para interoperabilidade direta
    private String headline;
    private String quote;
    private String sectionTitle;
    private String body;

    public Map<String, Object> getResolvedPayloadPt() {
        if (payloadPt != null && !payloadPt.isEmpty()) {
            return payloadPt;
        }
        Map<String, Object> map = new java.util.HashMap<>();
        if (headline != null) map.put("headline", headline);
        if (quote != null) map.put("quote", quote);
        if (sectionTitle != null) map.put("sectionTitle", sectionTitle);
        if (body != null) map.put("body", body);
        return map;
    }
}

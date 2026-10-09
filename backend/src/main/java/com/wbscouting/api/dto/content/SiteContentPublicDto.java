package com.wbscouting.api.dto.content;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SiteContentPublicDto {

    private String sectionKey;
    private Map<String, Object> payload;
    private Map<String, Object> mediaUrls;
    private String lang;

    public String getContent() {
        if (payload != null && payload.containsKey("content") && payload.get("content") != null) {
            return payload.get("content").toString();
        }
        if (payload != null && payload.containsKey("body") && payload.get("body") != null) {
            return payload.get("body").toString();
        }
        return null;
    }
}

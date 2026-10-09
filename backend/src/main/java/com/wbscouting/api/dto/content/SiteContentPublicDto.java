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
    private String content;

    public String getContent() {
        if (content != null && !content.isBlank()) {
            return content;
        }
        if (payload != null) {
            if (payload.containsKey("content") && payload.get("content") != null) {
                return payload.get("content").toString();
            }
            if (payload.containsKey("body") && payload.get("body") != null) {
                return payload.get("body").toString();
            }
            if (payload.containsKey("text") && payload.get("text") != null) {
                return payload.get("text").toString();
            }
        }
        return content != null ? content : "";
    }
    public void setContent(String content) {
        this.content = content;
    }
}

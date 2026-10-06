package com.wbscouting.api.dto.content;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SectionSummaryDto {
    private String sectionKey;
    private String title;
    private String description;

    // 🔥 GETTERS EXPLICITOS fallback (Lombok @Data nao processa no mvnw 3.6.3)
    public String getSectionKey() { return sectionKey; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }

    // 🔥 SETTERS EXPLICITOS
    public void setSectionKey(String sectionKey) { this.sectionKey = sectionKey; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }

    // 🔥 BUILDER MANUAL fallback (alias builder() = assinatura Lombok)
    public static SectionSummaryDtoBuilder builder() { return new SectionSummaryDtoBuilder(); }
    public static class SectionSummaryDtoBuilder {
        private final SectionSummaryDto s = new SectionSummaryDto();
        public SectionSummaryDtoBuilder sectionKey(String v) { s.setSectionKey(v); return this; }
        public SectionSummaryDtoBuilder title(String v) { s.setTitle(v); return this; }
        public SectionSummaryDtoBuilder description(String v) { s.setDescription(v); return this; }
        public SectionSummaryDto build() { return s; }
    }
}

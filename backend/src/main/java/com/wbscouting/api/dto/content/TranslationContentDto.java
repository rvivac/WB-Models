package com.wbscouting.api.dto.content;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranslationContentDto {
    private String headline;
    private String quote;
    private String sectionTitle;
    private String body;
    private String content;

    // 🔥 GETTERS EXPLICITOS fallback (Lombok @Data nao processa no mvnw 3.6.3)
    public String getHeadline() { return headline; }
    public String getQuote() { return quote; }
    public String getSectionTitle() { return sectionTitle; }
    public String getBody() { return body; }
    public String getContent() { return content != null ? content : body; }

    // 🔥 SETTERS EXPLICITOS
    public void setHeadline(String headline) { this.headline = headline; }
    public void setQuote(String quote) { this.quote = quote; }
    public void setSectionTitle(String sectionTitle) { this.sectionTitle = sectionTitle; }
    public void setBody(String body) { this.body = body; }
    public void setContent(String content) { this.content = content; }

    // 🔥 BUILDER MANUAL fallback (alias builder() = assinatura Lombok)
    public static TranslationContentDtoBuilder builder() { return new TranslationContentDtoBuilder(); }
    public static class TranslationContentDtoBuilder {
        private final TranslationContentDto t = new TranslationContentDto();
        public TranslationContentDtoBuilder headline(String v) { t.setHeadline(v); return this; }
        public TranslationContentDtoBuilder quote(String v) { t.setQuote(v); return this; }
        public TranslationContentDtoBuilder sectionTitle(String v) { t.setSectionTitle(v); return this; }
        public TranslationContentDtoBuilder body(String v) { t.setBody(v); return this; }
        public TranslationContentDtoBuilder content(String v) { t.setContent(v); return this; }
        public TranslationContentDto build() { return t; }
    }
}

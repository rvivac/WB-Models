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
    private String body;

    // 🔥 GETTERS EXPLICITOS fallback (Lombok @Data nao processa no mvnw 3.6.3)
    public String getHeadline() { return headline; }
    public String getQuote() { return quote; }
    public String getBody() { return body; }

    // 🔥 SETTERS EXPLICITOS
    public void setHeadline(String headline) { this.headline = headline; }
    public void setQuote(String quote) { this.quote = quote; }
    public void setBody(String body) { this.body = body; }

    // 🔥 BUILDER MANUAL fallback (alias builder() = assinatura Lombok)
    public static TranslationContentDtoBuilder builder() { return new TranslationContentDtoBuilder(); }
    public static class TranslationContentDtoBuilder {
        private final TranslationContentDto t = new TranslationContentDto();
        public TranslationContentDtoBuilder headline(String v) { t.setHeadline(v); return this; }
        public TranslationContentDtoBuilder quote(String v) { t.setQuote(v); return this; }
        public TranslationContentDtoBuilder body(String v) { t.setBody(v); return this; }
        public TranslationContentDto build() { return t; }
    }
}

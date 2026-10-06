package com.wbscouting.api.dto.content;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranslationUpdateRequestDto {
    private TranslationContentDto pt;
    private TranslationContentDto en;
    private BilingualTranslationsDto translations;

    // 🔥 GETTERS EXPLICITOS fallback (Lombok @Data nao processa no mvnw 3.6.3)
    public TranslationContentDto getPt() { return pt; }
    public TranslationContentDto getEn() { return en; }
    public BilingualTranslationsDto getTranslations() { return translations; }

    // 🔥 SETTERS EXPLICITOS
    public void setPt(TranslationContentDto pt) { this.pt = pt; }
    public void setEn(TranslationContentDto en) { this.en = en; }
    public void setTranslations(BilingualTranslationsDto translations) { this.translations = translations; }

    // 🔥 BUILDER MANUAL fallback (alias builder() = assinatura Lombok)
    public static TranslationUpdateRequestDtoBuilder builder() { return new TranslationUpdateRequestDtoBuilder(); }
    public static class TranslationUpdateRequestDtoBuilder {
        private final TranslationUpdateRequestDto r = new TranslationUpdateRequestDto();
        public TranslationUpdateRequestDtoBuilder pt(TranslationContentDto v) { r.setPt(v); return this; }
        public TranslationUpdateRequestDtoBuilder en(TranslationContentDto v) { r.setEn(v); return this; }
        public TranslationUpdateRequestDtoBuilder translations(BilingualTranslationsDto v) { r.setTranslations(v); return this; }
        public TranslationUpdateRequestDto build() { return r; }
    }

    public TranslationContentDto getResolvedPt() {
        if (pt != null) {
            return pt;
        }
        if (translations != null && translations.getPt() != null) {
            return translations.getPt();
        }
        return new TranslationContentDto();
    }

    public TranslationContentDto getResolvedEn() {
        if (en != null) {
            return en;
        }
        if (translations != null && translations.getEn() != null) {
            return translations.getEn();
        }
        return new TranslationContentDto();
    }
}

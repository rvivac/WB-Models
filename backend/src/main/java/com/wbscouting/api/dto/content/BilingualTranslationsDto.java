package com.wbscouting.api.dto.content;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BilingualTranslationsDto {
    private TranslationContentDto pt;
    private TranslationContentDto en;

    // 🔥 GETTERS EXPLICITOS fallback (Lombok @Data nao processa no mvnw 3.6.3)
    public TranslationContentDto getPt() { return pt; }
    public TranslationContentDto getEn() { return en; }

    // 🔥 SETTERS EXPLICITOS
    public void setPt(TranslationContentDto pt) { this.pt = pt; }
    public void setEn(TranslationContentDto en) { this.en = en; }

    // 🔥 BUILDER MANUAL fallback (alias builder() = assinatura Lombok)
    public static BilingualTranslationsDtoBuilder builder() { return new BilingualTranslationsDtoBuilder(); }
    public static class BilingualTranslationsDtoBuilder {
        private final BilingualTranslationsDto b = new BilingualTranslationsDto();
        public BilingualTranslationsDtoBuilder pt(TranslationContentDto v) { b.setPt(v); return this; }
        public BilingualTranslationsDtoBuilder en(TranslationContentDto v) { b.setEn(v); return this; }
        public BilingualTranslationsDto build() { return b; }
    }
}

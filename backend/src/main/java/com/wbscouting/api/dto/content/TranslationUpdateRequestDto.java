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

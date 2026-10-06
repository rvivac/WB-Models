package com.wbscouting.api.dto.content;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

// TASK: Anotacoes Lombok completas (@Data @Builder @NoArgsConstructor @AllArgsConstructor) mantidas.
// Getters/setters e builder manual adicionados fallback para MavenWrapper 3.6.3 (annotation processor nao roda).
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SectionTranslationResponseDto {
    private String sectionKey;
    private String title;
    private OffsetDateTime lastUpdated;
    private BilingualTranslationsDto translations;

    // 🔥 GETTERS EXPLICITOS fallback
    public String getSectionKey() { return sectionKey; }
    public String getTitle() { return title; }
    public OffsetDateTime getLastUpdated() { return lastUpdated; }
    public BilingualTranslationsDto getTranslations() { return translations; }

    // 🔥 SETTERS EXPLICITOS
    public void setSectionKey(String sectionKey) { this.sectionKey = sectionKey; }
    public void setTitle(String title) { this.title = title; }
    public void setLastUpdated(OffsetDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
    public void setTranslations(BilingualTranslationsDto translations) { this.translations = translations; }

    // 🔥 BUILDER MANUAL fallback (alias builder() = assinatura Lombok)
    public static SectionTranslationResponseDtoBuilder builder() { return new SectionTranslationResponseDtoBuilder(); }
    public static class SectionTranslationResponseDtoBuilder {
        private final SectionTranslationResponseDto s = new SectionTranslationResponseDto();
        public SectionTranslationResponseDtoBuilder sectionKey(String v) { s.setSectionKey(v); return this; }
        public SectionTranslationResponseDtoBuilder title(String v) { s.setTitle(v); return this; }
        public SectionTranslationResponseDtoBuilder lastUpdated(OffsetDateTime v) { s.setLastUpdated(v); return this; }
        public SectionTranslationResponseDtoBuilder translations(BilingualTranslationsDto v) { s.setTranslations(v); return this; }
        public SectionTranslationResponseDto build() { return s; }
    }
}

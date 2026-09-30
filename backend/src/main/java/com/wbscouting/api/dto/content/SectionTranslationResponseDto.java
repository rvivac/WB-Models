package com.wbscouting.api.dto.content;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SectionTranslationResponseDto {
    private String sectionKey;
    private String title;
    private OffsetDateTime lastUpdated;
    private BilingualTranslationsDto translations;
}

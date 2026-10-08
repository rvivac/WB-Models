package com.wbscouting.api.dto.content;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TranslationBulkUpdateDto {

    private String locale; // e.g. "pt", "en"

    private String key;

    private String value;

    private Map<String, String> translations;
}

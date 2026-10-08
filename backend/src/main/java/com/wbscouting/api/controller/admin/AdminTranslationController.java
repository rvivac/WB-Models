package com.wbscouting.api.controller.admin;

import com.wbscouting.api.dto.content.TranslationBulkUpdateDto;
import com.wbscouting.api.entity.Translation;
import com.wbscouting.api.repository.TranslationRepository;
import com.wbscouting.api.security.audit.AuditAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.*;

@Slf4j
@RestController
@RequestMapping({"/api/v1/admin/translations", "/admin/translations"})
@PreAuthorize("hasAnyRole('WEBMASTER', 'SUPER_ADMIN', 'CONTENT_ADMIN', 'ADMIN')")
@RequiredArgsConstructor
public class AdminTranslationController {

    private final TranslationRepository translationRepository;

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, String>> getTranslations(
            @RequestParam(value = "lang", defaultValue = "pt") String lang
    ) {
        String normalizedLocale = lang.trim().toLowerCase();
        List<Translation> list = translationRepository.findByLocale(normalizedLocale);

        Map<String, String> dict = new LinkedHashMap<>();
        for (Translation t : list) {
            dict.put(t.getKey(), t.getValue());
        }
        return ResponseEntity.ok(dict);
    }

    @PutMapping
    @Transactional
    @AuditAction(action = "UPDATE", resource = "TRANSLATIONS", description = "Atualização de dicionário de tradução")
    public ResponseEntity<Map<String, Object>> updateTranslations(
            @RequestBody TranslationBulkUpdateDto dto
    ) {
        String locale = (dto.getLocale() != null ? dto.getLocale() : "pt").trim().toLowerCase();
        log.info("Salvando traduções para o locale '{}'", locale);

        Map<String, String> itemsToSave = new LinkedHashMap<>();
        if (dto.getTranslations() != null) {
            itemsToSave.putAll(dto.getTranslations());
        }
        if (dto.getKey() != null && dto.getValue() != null) {
            itemsToSave.put(dto.getKey(), dto.getValue());
        }

        int updatedCount = 0;
        for (Map.Entry<String, String> entry : itemsToSave.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key == null || value == null) continue;

            Optional<Translation> existingOpt = translationRepository.findByLocaleAndKey(locale, key);
            Translation entity;
            if (existingOpt.isPresent()) {
                entity = existingOpt.get();
                entity.setValue(value);
                entity.setUpdatedAt(OffsetDateTime.now());
            } else {
                entity = Translation.builder()
                        .locale(locale)
                        .key(key)
                        .value(value)
                        .createdAt(OffsetDateTime.now())
                        .updatedAt(OffsetDateTime.now())
                        .build();
            }
            translationRepository.save(entity);
            updatedCount++;
        }
        translationRepository.flush();

        log.info("Total de {} chaves de tradução persistidas com sucesso no banco de dados.", updatedCount);
        return ResponseEntity.ok(Map.of(
                "locale", locale,
                "updatedCount", updatedCount,
                "status", "SUCCESS"
        ));
    }
}

package com.wbscouting.api.controller.publicapi;

import com.wbscouting.api.entity.Translation;
import com.wbscouting.api.repository.TranslationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping({"/api/v1/translations", "/translations"})
@RequiredArgsConstructor
public class PublicTranslationController {

    private final TranslationRepository translationRepository;

    @GetMapping
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
}

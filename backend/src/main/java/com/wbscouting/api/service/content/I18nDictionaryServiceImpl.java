package com.wbscouting.api.service.content;

import com.wbscouting.api.constant.ContentSectionKey;
import com.wbscouting.api.entity.SiteContent;
import com.wbscouting.api.repository.SiteContentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class I18nDictionaryServiceImpl implements I18nDictionaryService {

    private final SiteContentRepository siteContentRepository;

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getDictionary(String lang) {
        boolean isEn = "en".equalsIgnoreCase(lang);
        String targetLang = isEn ? "en" : "pt";
        log.debug("Gerando dicionário dinâmico i18n para idioma: {}", targetLang);

        Map<String, Object> dictionary = new LinkedHashMap<>();

        // Pré-inicializar com chaves canônicas vazias para garantir contrato do frontend
        for (String canonicalKey : ContentSectionKey.CANONICAL_KEYS) {
            dictionary.put(canonicalKey, new LinkedHashMap<String, Object>());
        }

        List<SiteContent> allContents = siteContentRepository.findAll();
        for (SiteContent content : allContents) {
            if (content.getSectionKey() == null) {
                continue;
            }

            Map<String, Object> resolvedPayload;
            if (isEn) {
                if (content.getPayloadEn() != null && !content.getPayloadEn().isEmpty()) {
                    resolvedPayload = content.getPayloadEn();
                } else {
                    resolvedPayload = content.getPayloadPt() != null ? content.getPayloadPt() : Map.of();
                }
            } else {
                resolvedPayload = content.getPayloadPt() != null ? content.getPayloadPt() : Map.of();
            }

            dictionary.put(content.getSectionKey(), resolvedPayload);
        }

        return dictionary;
    }
}

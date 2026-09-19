package com.wbscouting.api.service.content;

import com.wbscouting.api.constant.ContentSectionKey;
import com.wbscouting.api.entity.SiteContent;
import com.wbscouting.api.repository.SiteContentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class I18nDictionaryServiceImplTest {

    @Mock
    private SiteContentRepository siteContentRepository;

    @InjectMocks
    private I18nDictionaryServiceImpl i18nDictionaryService;

    @Test
    @DisplayName("Deve retornar todas as chaves canônicas com mapas vazios quando o banco estiver vazio")
    void getDictionary_EmptyDatabase_ReturnsCanonicalKeysWithEmptyMaps() {
        when(siteContentRepository.findAll()).thenReturn(List.of());

        Map<String, Object> result = i18nDictionaryService.getDictionary("pt");

        assertThat(result).isNotNull();
        for (String canonicalKey : ContentSectionKey.CANONICAL_KEYS) {
            assertThat(result).containsKey(canonicalKey);
        }
    }

    @Test
    @DisplayName("Deve retornar payloads em português quando lang for pt")
    void getDictionary_Pt_ReturnsAllSectionsInPt() {
        SiteContent hero = SiteContent.builder()
                .sectionKey(ContentSectionKey.HOME_HERO)
                .payloadPt(Map.of("title", "Título Hero PT"))
                .payloadEn(Map.of("title", "Hero Title EN"))
                .build();

        SiteContent about = SiteContent.builder()
                .sectionKey(ContentSectionKey.ABOUT_US)
                .payloadPt(Map.of("description", "Sobre nós PT"))
                .payloadEn(Map.of("description", "About us EN"))
                .build();

        when(siteContentRepository.findAll()).thenReturn(List.of(hero, about));

        Map<String, Object> result = i18nDictionaryService.getDictionary("pt");

        assertThat(result).isNotNull();
        assertThat(result.get(ContentSectionKey.HOME_HERO)).isEqualTo(Map.of("title", "Título Hero PT"));
        assertThat(result.get(ContentSectionKey.ABOUT_US)).isEqualTo(Map.of("description", "Sobre nós PT"));
    }

    @Test
    @DisplayName("Deve retornar payloads em inglês com fallback para português se EN estiver ausente")
    void getDictionary_En_ReturnsEnPayloadsWithFallbackToPt() {
        SiteContent hero = SiteContent.builder()
                .sectionKey(ContentSectionKey.HOME_HERO)
                .payloadPt(Map.of("title", "Título Hero PT"))
                .payloadEn(Map.of("title", "Hero Title EN"))
                .build();

        SiteContent terms = SiteContent.builder()
                .sectionKey(ContentSectionKey.TERMS_PRIVACY)
                .payloadPt(Map.of("terms", "Termos PT"))
                .payloadEn(Map.of()) // vazio, deve cair no fallback
                .build();

        when(siteContentRepository.findAll()).thenReturn(List.of(hero, terms));

        Map<String, Object> result = i18nDictionaryService.getDictionary("en");

        assertThat(result).isNotNull();
        assertThat(result.get(ContentSectionKey.HOME_HERO)).isEqualTo(Map.of("title", "Hero Title EN"));
        assertThat(result.get(ContentSectionKey.TERMS_PRIVACY)).isEqualTo(Map.of("terms", "Termos PT"));
    }
}

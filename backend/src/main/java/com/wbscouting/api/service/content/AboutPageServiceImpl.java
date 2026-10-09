package com.wbscouting.api.service.content;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wbscouting.api.dto.AboutPageDto;
import com.wbscouting.api.dto.AboutPillarDto;
import com.wbscouting.api.dto.AboutSeoDto;
import com.wbscouting.api.entity.InstitutionalSetting;
import com.wbscouting.api.repository.InstitutionalSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AboutPageServiceImpl implements AboutPageService {

    public static final String ABOUT_PAGE_KEY = "ABOUT_PAGE";

    public static final String DEFAULT_TITLE = "A Nova Estética do Scouting Global";
    public static final String DEFAULT_SUBTITLE = "MANIFESTO INSTITUCIONAL";
    public static final String DEFAULT_DESCRIPTION = "A WB Agency consolidou-se como um núcleo editorial focado no desenvolvimento integral de modelos para os principais mercados da moda internacional. Nossa metodologia rejeita a padronização e prioriza a identidade visual autêntica, conectando talentos a marcas com relevância estética global.";
    public static final String DEFAULT_HERO_QUOTE = "Acreditamos na autenticidade, na força da personalidade e na beleza singular de cada indivíduo.";
    public static final String DEFAULT_MANIFESTO_TITLE = "Nossa Filosofia";
    public static final String DEFAULT_MANIFESTO_TEXT = "Conectamos talentos às principais marcas com curadoria estratégica, visão de vanguarda e compromisso com o desenvolvimento humano e profissional em escala global.";
    public static final String DEFAULT_PILLARS_TITLE = "Nossos Pilares & Valores";

    private final InstitutionalSettingRepository institutionalSettingRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public AboutPageDto getPublicAboutPage() {
        return getAdminAboutPage();
    }

    @Override
    @Transactional(readOnly = true)
    public AboutPageDto getAdminAboutPage() {
        try {
            return institutionalSettingRepository.findBySettingKey(ABOUT_PAGE_KEY)
                    .map(this::mapEntityToDto)
                    .orElseGet(this::getDefaultAboutPage);
        } catch (Exception e) {
            log.warn("Erro ao buscar configurações da página Sobre Nós, ativando fallback padrão: {}", e.getMessage());
            return getDefaultAboutPage();
        }
    }

    @Override
    @Transactional
    public AboutPageDto updateAboutPage(AboutPageDto dto) {
        log.info("Atualizando configurações institucionais da página Sobre Nós ('{}')", ABOUT_PAGE_KEY);

        InstitutionalSetting setting = institutionalSettingRepository.findBySettingKey(ABOUT_PAGE_KEY)
                .orElseGet(() -> InstitutionalSetting.builder()
                        .settingKey(ABOUT_PAGE_KEY)
                        .build());

        setting.setTitle(dto.getTitle() != null && !dto.getTitle().isBlank() ? dto.getTitle().trim() : DEFAULT_TITLE);
        setting.setSubtitle(dto.getSubtitle() != null && !dto.getSubtitle().isBlank() ? dto.getSubtitle().trim() : DEFAULT_SUBTITLE);
        setting.setDescription(dto.getDescription() != null && !dto.getDescription().isBlank() ? dto.getDescription().trim() : DEFAULT_DESCRIPTION);

        Map<String, Object> contentMap = new LinkedHashMap<>();
        contentMap.put("heroQuote", dto.getHeroQuote() != null && !dto.getHeroQuote().isBlank() ? dto.getHeroQuote().trim() : DEFAULT_HERO_QUOTE);
        contentMap.put("manifestoTitle", dto.getManifestoTitle() != null && !dto.getManifestoTitle().isBlank() ? dto.getManifestoTitle().trim() : DEFAULT_MANIFESTO_TITLE);
        contentMap.put("manifestoText", dto.getManifestoText() != null && !dto.getManifestoText().isBlank() ? dto.getManifestoText().trim() : DEFAULT_MANIFESTO_TEXT);
        contentMap.put("pillarsTitle", dto.getPillarsTitle() != null && !dto.getPillarsTitle().isBlank() ? dto.getPillarsTitle().trim() : DEFAULT_PILLARS_TITLE);

        List<AboutPillarDto> pillars = dto.getPillars() != null && !dto.getPillars().isEmpty()
                ? dto.getPillars()
                : getDefaultPillars();
        contentMap.put("pillars", pillars);

        AboutSeoDto seo = dto.getSeo() != null ? dto.getSeo() : getDefaultSeo();
        contentMap.put("seo", seo);

        setting.setContentData(contentMap);
        setting.setContentJson(contentMap);

        InstitutionalSetting saved = institutionalSettingRepository.save(setting);
        log.info("Página Sobre Nós atualizada com sucesso");

        return mapEntityToDto(saved);
    }

    private AboutPageDto mapEntityToDto(InstitutionalSetting setting) {
        AboutPageDto.AboutPageDtoBuilder builder = AboutPageDto.builder()
                .title(setting.getTitle() != null && !setting.getTitle().isBlank() ? setting.getTitle() : DEFAULT_TITLE)
                .subtitle(setting.getSubtitle() != null && !setting.getSubtitle().isBlank() ? setting.getSubtitle() : DEFAULT_SUBTITLE)
                .description(setting.getDescription() != null && !setting.getDescription().isBlank() ? setting.getDescription() : DEFAULT_DESCRIPTION)
                .updatedAt(setting.getUpdatedAt());

        Map<String, Object> data = setting.getContentDataWithFallback();
        if (data != null && !data.isEmpty()) {
            builder.heroQuote(extractString(data, "heroQuote", "hero_quote", DEFAULT_HERO_QUOTE));
            builder.manifestoTitle(extractString(data, "manifestoTitle", "manifesto_title", DEFAULT_MANIFESTO_TITLE));
            builder.manifestoText(extractString(data, "manifestoText", "manifesto_text", DEFAULT_MANIFESTO_TEXT));
            builder.pillarsTitle(extractString(data, "pillarsTitle", "pillars_title", DEFAULT_PILLARS_TITLE));
            
            Object rawPillars = data.containsKey("pillars") ? data.get("pillars") : data.get("pilares");
            builder.pillars(extractPillars(rawPillars));
            builder.seo(extractSeo(data.get("seo")));
        } else {
            builder.heroQuote(DEFAULT_HERO_QUOTE)
                    .manifestoTitle(DEFAULT_MANIFESTO_TITLE)
                    .manifestoText(DEFAULT_MANIFESTO_TEXT)
                    .pillarsTitle(DEFAULT_PILLARS_TITLE)
                    .pillars(getDefaultPillars())
                    .seo(getDefaultSeo());
        }

        return builder.build();
    }

    private String extractString(Map<String, Object> map, String key, String fallbackKey, String defaultValue) {
        Object val = map.get(key);
        if (val == null && fallbackKey != null) {
            val = map.get(fallbackKey);
        }
        if (val instanceof String s && !s.isBlank()) {
            return s.trim();
        }
        return defaultValue;
    }

    private List<AboutPillarDto> extractPillars(Object rawPillars) {
        if (rawPillars == null) {
            return getDefaultPillars();
        }
        try {
            return objectMapper.convertValue(rawPillars, new TypeReference<List<AboutPillarDto>>() {});
        } catch (Exception e) {
            log.warn("Falha ao converter pilares da página Sobre Nós, usando padrão: {}", e.getMessage());
            return getDefaultPillars();
        }
    }

    private AboutSeoDto extractSeo(Object rawSeo) {
        if (rawSeo == null) {
            return getDefaultSeo();
        }
        try {
            return objectMapper.convertValue(rawSeo, AboutSeoDto.class);
        } catch (Exception e) {
            log.warn("Falha ao converter SEO da página Sobre Nós, usando padrão: {}", e.getMessage());
            return getDefaultSeo();
        }
    }

    public AboutPageDto getDefaultAboutPage() {
        return AboutPageDto.builder()
                .title(DEFAULT_TITLE)
                .subtitle(DEFAULT_SUBTITLE)
                .description(DEFAULT_DESCRIPTION)
                .heroQuote(DEFAULT_HERO_QUOTE)
                .manifestoTitle(DEFAULT_MANIFESTO_TITLE)
                .manifestoText(DEFAULT_MANIFESTO_TEXT)
                .pillarsTitle(DEFAULT_PILLARS_TITLE)
                .pillars(getDefaultPillars())
                .seo(getDefaultSeo())
                .build();
    }

    public List<AboutPillarDto> getDefaultPillars() {
        return List.of(
                AboutPillarDto.builder()
                        .order(1)
                        .titulo("Curadoria & Autenticidade")
                        .descricao("Descoberta e representação de perfis singulares com identidade própria e alto potencial editorial.")
                        .build(),
                AboutPillarDto.builder()
                        .order(2)
                        .titulo("Transparência & Ética")
                        .descricao("Relações comerciais claras e respeito irrestrito aos contratos, imagem e bem-estar de cada modelo.")
                        .build(),
                AboutPillarDto.builder()
                        .order(3)
                        .titulo("Desenvolvimento de Carreira")
                        .descricao("Orientação contínua, construção de portfólio de alto nível e preparação para passarelas e campanhas.")
                        .build(),
                AboutPillarDto.builder()
                        .order(4)
                        .titulo("Alcance & Conexões")
                        .descricao("Pontes estratégicas com as principais agências parceiras, diretores de casting e marcas mundiais.")
                        .build()
        );
    }

    public AboutSeoDto getDefaultSeo() {
        return AboutSeoDto.builder()
                .metaTitle("Sobre Nós | WB Agency - Scouting & Model Management")
                .metaDescription("Conheça a WB Agency, agência de modelos e scouting internacional focada na autenticidade, excelência editorial e gestão de carreiras globais.")
                .build();
    }
}

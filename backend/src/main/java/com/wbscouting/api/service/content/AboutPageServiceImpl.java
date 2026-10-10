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
    public static final String DEFAULT_SUBTITLE = "";
    public static final String DEFAULT_DESCRIPTION = "Conectamos talentos às principais marcas com curadoria estratégica, visão de vanguarda e compromisso com o desenvolvimento humano e profissional em escala global.";
    public static final String DEFAULT_HERO_QUOTE = "Acreditamos na autenticidade, na força da personalidade e na beleza singular de cada indivíduo.";
    public static final String DEFAULT_MANIFESTO_TITLE = "Nossa Filosofia";
    public static final String DEFAULT_MANIFESTO_TEXT = "Conectamos talentos às principais marcas com curadoria estratégica, visão de vanguarda e compromisso com o desenvolvimento humano e profissional em escala global.";
    public static final String DEFAULT_PILLARS_TITLE = "Nossos Pilares & Valores";

    private final InstitutionalSettingRepository institutionalSettingRepository;
    private final ObjectMapper objectMapper;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.wbscouting.api.repository.SiteContentRepository siteContentRepository;

    @Override
    @Transactional(readOnly = true)
    public AboutPageDto getPublicAboutPage() {
        return getPublicAboutPage("pt");
    }

    @Override
    @Transactional(readOnly = true)
    public AboutPageDto getPublicAboutPage(String lang) {
        AboutPageDto dto = getAdminAboutPage();
        applyAboutManifestoTranslations(dto, lang);
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public com.wbscouting.api.dto.AboutPageResponseDto getPublicAboutPageResponse() {
        return getPublicAboutPageResponse("pt");
    }

    @Override
    @Transactional(readOnly = true)
    public com.wbscouting.api.dto.AboutPageResponseDto getPublicAboutPageResponse(String lang) {
        String resolvedLang = (lang != null && !lang.isBlank()) ? lang : "pt";
        boolean isEn = resolvedLang.trim().toLowerCase().startsWith("en");

        String headline = null;
        String quote = null;
        String sectionTitle = null;
        String body = null;
        boolean foundInSiteContent = false;

        // 1. Busca prioritária direta em site_contents onde section_key = 'ABOUT_MANIFESTO'
        if (siteContentRepository != null) {
            try {
                var contentOpt = siteContentRepository.findBySectionKey("ABOUT_MANIFESTO");
                if (contentOpt.isPresent()) {
                    var content = contentOpt.get();
                    Map<String, Object> payload = isEn ? content.getPayloadEn() : content.getPayloadPt();
                    if (payload == null) {
                        payload = content.getPayloadPt();
                    }
                    if (payload != null) {
                        foundInSiteContent = true;
                        headline = (String) payload.getOrDefault("headline", "");
                        quote = (String) payload.getOrDefault("quote", "");
                        sectionTitle = (String) payload.getOrDefault("sectionTitle", "");
                        body = (String) payload.getOrDefault("body", payload.getOrDefault("content", ""));
                    }
                }
            } catch (Exception ex) {
                log.warn("Erro ao buscar ABOUT_MANIFESTO diretamente de site_contents: {}", ex.getMessage());
            }
        }

        // 2. Se não encontrado em site_contents, busca em institutional_settings (ABOUT_PAGE)
        if (!foundInSiteContent) {
            AboutPageDto adminDto = getAdminAboutPage();
            if (adminDto != null) {
                headline = adminDto.getHeadline();
                quote = adminDto.getQuote();
                sectionTitle = adminDto.getSectionTitle();
                body = adminDto.getBody();
            }
        }

        String finalHeadline = headline != null ? headline.trim() : "";
        String finalQuote = quote != null ? quote.trim() : "";
        String finalSectionTitle = sectionTitle != null ? sectionTitle.trim() : "";
        String finalBody = body != null ? body.trim() : "";

        return new com.wbscouting.api.dto.AboutPageResponseDto(
                finalHeadline,
                finalHeadline,
                finalQuote,
                finalQuote,
                finalSectionTitle,
                finalSectionTitle,
                finalBody,
                finalBody
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AboutPageDto getAdminAboutPage() {
        try {
            AboutPageDto dto = institutionalSettingRepository.findBySettingKey(ABOUT_PAGE_KEY)
                    .map(this::mapEntityToDto)
                    .orElseGet(this::getDefaultAboutPage);
            applyAboutManifestoTranslations(dto, "pt");
            return dto;
        } catch (Exception e) {
            log.warn("Erro ao buscar configurações da página Sobre Nós, ativando fallback padrão: {}", e.getMessage());
            AboutPageDto fallback = getDefaultAboutPage();
            applyAboutManifestoTranslations(fallback, "pt");
            return fallback;
        }
    }

    private void applyAboutManifestoTranslations(AboutPageDto dto, String lang) {
        if (dto == null) return;
        boolean isEn = lang != null && lang.trim().toLowerCase().startsWith("en");
        if (siteContentRepository != null) {
            try {
                siteContentRepository.findBySectionKey("ABOUT_MANIFESTO").ifPresent(content -> {
                    Map<String, Object> payload = isEn ? content.getPayloadEn() : content.getPayloadPt();
                    if (payload == null || payload.isEmpty()) {
                        payload = content.getPayloadPt();
                    }
                    if (payload != null && !payload.isEmpty()) {
                        String headline = (String) payload.get("headline");
                        String quote = (String) payload.get("quote");
                        String sectionTitle = (String) payload.get("sectionTitle");
                        String body = (String) payload.get("body");

                        if (headline != null) {
                            dto.setTitle(headline.trim());
                            dto.setPageTitle(headline.trim());
                            dto.setHeadline(headline.trim());
                        }
                        if (quote != null) {
                            dto.setHeroQuote(quote.trim());
                            dto.setQuote(quote.trim());
                        }
                        if (sectionTitle != null) {
                            dto.setSectionTitle(sectionTitle.trim());
                            dto.setManifestoTitle(sectionTitle.trim());
                        }
                        if (body != null) {
                            dto.setManifestoText(body.trim());
                            dto.setBodyText(body.trim());
                            dto.setBody(body.trim());
                            dto.setDescription(body.trim());
                        }
                    }
                });
            } catch (Exception ex) {
                log.warn("Erro ao aplicar traduções de ABOUT_MANIFESTO: {}", ex.getMessage());
            }
        }
        // Paridade dos campos do DTO
        if (dto.getPageTitle() == null) dto.setPageTitle(dto.getTitle());
        if (dto.getHeadline() == null) dto.setHeadline(dto.getTitle());
        if (dto.getQuote() == null) dto.setQuote(dto.getHeroQuote());
        if (dto.getSectionTitle() == null) {
            dto.setSectionTitle(dto.getManifestoTitle() != null ? dto.getManifestoTitle() : "");
        }
        if (dto.getManifestoTitle() == null) {
            dto.setManifestoTitle(dto.getSectionTitle());
        }
        if (dto.getBodyText() == null) dto.setBodyText(dto.getManifestoText() != null ? dto.getManifestoText() : dto.getDescription());
        if (dto.getBody() == null) dto.setBody(dto.getBodyText());
        if ("MANIFESTO INSTITUCIONAL".equalsIgnoreCase(dto.getSubtitle())) {
            dto.setSubtitle("");
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

        setting.setTitle(dto.getTitle() != null ? dto.getTitle().trim() : (dto.getHeadline() != null ? dto.getHeadline().trim() : ""));
        setting.setSubtitle(dto.getSubtitle() != null ? dto.getSubtitle().trim() : "");
        setting.setDescription(dto.getDescription() != null ? dto.getDescription().trim() : (dto.getBody() != null ? dto.getBody().trim() : ""));

        String resolvedSectionTitle = dto.getSectionTitle() != null
                ? dto.getSectionTitle().trim()
                : (dto.getManifestoTitle() != null ? dto.getManifestoTitle().trim() : "");

        Map<String, Object> contentMap = new LinkedHashMap<>();
        contentMap.put("heroQuote", dto.getHeroQuote() != null ? dto.getHeroQuote().trim() : (dto.getQuote() != null ? dto.getQuote().trim() : ""));
        contentMap.put("sectionTitle", resolvedSectionTitle);
        contentMap.put("manifestoTitle", resolvedSectionTitle);
        contentMap.put("manifestoText", dto.getManifestoText() != null ? dto.getManifestoText().trim() : (dto.getBody() != null ? dto.getBody().trim() : ""));
        contentMap.put("pillarsTitle", dto.getPillarsTitle() != null ? dto.getPillarsTitle().trim() : "");

        List<AboutPillarDto> pillars = dto.getPillars() != null
                ? dto.getPillars()
                : Collections.emptyList();
        contentMap.put("pillars", pillars);

        AboutSeoDto seo = dto.getSeo() != null ? dto.getSeo() : getDefaultSeo();
        contentMap.put("seo", seo);

        setting.setContentData(contentMap);
        setting.setContentJson(contentMap);

        InstitutionalSetting saved = institutionalSettingRepository.save(setting);
        log.info("Página Sobre Nós atualizada com sucesso");

        if (siteContentRepository != null) {
            try {
                com.wbscouting.api.entity.SiteContent content = siteContentRepository.findBySectionKey("ABOUT_MANIFESTO")
                        .orElseGet(() -> com.wbscouting.api.entity.SiteContent.builder()
                                .sectionKey("ABOUT_MANIFESTO")
                                .build());
                Map<String, Object> pt = content.getPayloadPt() != null ? new HashMap<>(content.getPayloadPt()) : new HashMap<>();
                pt.put("headline", setting.getTitle());
                pt.put("quote", dto.getHeroQuote() != null ? dto.getHeroQuote() : (dto.getQuote() != null ? dto.getQuote() : ""));
                pt.put("sectionTitle", resolvedSectionTitle);
                pt.put("body", dto.getManifestoText() != null ? dto.getManifestoText() : (dto.getBody() != null ? dto.getBody() : ""));
                content.setPayloadPt(pt);
                siteContentRepository.save(content);
                log.info("Sincronização com siteContentRepository (ABOUT_MANIFESTO) concluída");
            } catch (Exception ex) {
                log.warn("Erro ao sincronizar siteContentRepository na atualização da página Sobre: {}", ex.getMessage());
            }
        }

        return mapEntityToDto(saved);
    }

    private AboutPageDto mapEntityToDto(InstitutionalSetting setting) {
        String titleVal = setting.getTitle() != null ? setting.getTitle() : "";
        AboutPageDto.AboutPageDtoBuilder builder = AboutPageDto.builder()
                .title(titleVal)
                .pageTitle(titleVal)
                .headline(titleVal)
                .subtitle(setting.getSubtitle() != null ? setting.getSubtitle() : "")
                .description(setting.getDescription() != null ? setting.getDescription() : "")
                .updatedAt(setting.getUpdatedAt());

        Map<String, Object> data = setting.getContentDataWithFallback();
        if (data != null && !data.isEmpty()) {
            String quoteVal = extractString(data, "heroQuote", "hero_quote", DEFAULT_HERO_QUOTE);
            String bodyVal = extractString(data, "manifestoText", "manifesto_text", DEFAULT_MANIFESTO_TEXT);
            String sectionTitleVal = extractString(data, "sectionTitle", "manifestoTitle", DEFAULT_MANIFESTO_TITLE);
            builder.heroQuote(quoteVal);
            builder.quote(quoteVal);
            builder.sectionTitle(sectionTitleVal);
            builder.manifestoTitle(sectionTitleVal);
            builder.manifestoText(bodyVal);
            builder.bodyText(bodyVal);
            builder.body(bodyVal);
            builder.pillarsTitle(extractString(data, "pillarsTitle", "pillars_title", DEFAULT_PILLARS_TITLE));
            
            Object rawPillars = data.containsKey("pillars") ? data.get("pillars") : data.get("pilares");
            builder.pillars(extractPillars(rawPillars));
            builder.seo(extractSeo(data.get("seo")));
        } else {
            builder.heroQuote("")
                    .quote("")
                    .sectionTitle("")
                    .manifestoTitle("")
                    .manifestoText("")
                    .bodyText("")
                    .body("")
                    .pillarsTitle("")
                    .pillars(Collections.emptyList())
                    .seo(getDefaultSeo());
        }

        return builder.build();
    }

    private String extractString(Map<String, Object> map, String key, String fallbackKey, String defaultValue) {
        if (map.containsKey(key)) {
            Object val = map.get(key);
            return val != null ? val.toString().trim() : "";
        }
        if (fallbackKey != null && map.containsKey(fallbackKey)) {
            Object val = map.get(fallbackKey);
            return val != null ? val.toString().trim() : "";
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
                .pageTitle(DEFAULT_TITLE)
                .headline(DEFAULT_TITLE)
                .subtitle(DEFAULT_SUBTITLE)
                .description(DEFAULT_DESCRIPTION)
                .heroQuote(DEFAULT_HERO_QUOTE)
                .quote(DEFAULT_HERO_QUOTE)
                .sectionTitle(DEFAULT_MANIFESTO_TITLE)
                .manifestoTitle(DEFAULT_MANIFESTO_TITLE)
                .manifestoText(DEFAULT_MANIFESTO_TEXT)
                .bodyText(DEFAULT_MANIFESTO_TEXT)
                .body(DEFAULT_MANIFESTO_TEXT)
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

package com.wbscouting.api.controller.admin;

import com.wbscouting.api.dto.content.*;
import com.wbscouting.api.entity.Admin;
import com.wbscouting.api.entity.SiteContent;
import com.wbscouting.api.repository.SiteContentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.*;

@Slf4j
@RestController
@RequestMapping({"/api/v1/admin/institutional/translations", "/admin/institutional/translations"})
@PreAuthorize("hasAnyRole('WEBMASTER', 'SUPER_ADMIN', 'CONTENT_ADMIN', 'ADMIN')")
@RequiredArgsConstructor
public class AdminInstitutionalTranslationController {

    public static final String SECTION_ABOUT_MANIFESTO = "ABOUT_MANIFESTO";
    public static final String SECTION_SCOUTING_GUIDELINES = "SCOUTING_GUIDELINES";
    public static final String SECTION_TERMS_OF_USE = "TERMS_OF_USE";
    public static final String SECTION_PRIVACY_POLICY = "PRIVACY_POLICY";
    public static final String SECTION_APPLY_HOW_IT_WORKS = "APPLY_HOW_IT_WORKS";

    private final SiteContentRepository siteContentRepository;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.wbscouting.api.repository.TranslationRepository translationRepository;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.wbscouting.api.repository.InstitutionalSettingRepository institutionalSettingRepository;

    @GetMapping
    public ResponseEntity<List<SectionSummaryDto>> listTranslatableSections() {
        log.info("Listando seções institucionais traduzíveis");
        List<SectionSummaryDto> sections = List.of(
                SectionSummaryDto.builder()
                        .sectionKey(SECTION_ABOUT_MANIFESTO)
                        .title("Manifesto da Agência (Sobre Nós)")
                        .description("Posicionamento editorial e manifesto da agência")
                        .build(),
                SectionSummaryDto.builder()
                        .sectionKey(SECTION_SCOUTING_GUIDELINES)
                        .title("Diretrizes de Scouting (Seja Modelo)")
                        .description("Requisitos técnicos e orientações de submissão")
                        .build(),
                SectionSummaryDto.builder()
                        .sectionKey(SECTION_APPLY_HOW_IT_WORKS)
                        .title("Próximos Passos Apply (Como Funciona)")
                        .description("Texto exibido no formulário de candidatura /apply, após o envio bem-sucedido")
                        .build(),
                SectionSummaryDto.builder()
                        .sectionKey(SECTION_TERMS_OF_USE)
                        .title("Termos de Uso & Direitos de Imagem")
                        .description("Termos e condições contratuais de uso")
                        .build(),
                SectionSummaryDto.builder()
                        .sectionKey(SECTION_PRIVACY_POLICY)
                        .title("Política de Privacidade (LGPD / GDPR)")
                        .description("Proteção de dados e conformidade regulatória")
                        .build()
        );
        return ResponseEntity.ok(sections);
    }

    @GetMapping("/{sectionKey}")
    public ResponseEntity<SectionTranslationResponseDto> getSectionTranslations(@PathVariable String sectionKey) {
        log.info("Consultando traduções para a seção: {}", sectionKey);
        String normalizedKey = sectionKey.trim().toUpperCase(Locale.ROOT);

        SiteContent content = findSectionWithAlias(normalizedKey)
                .orElseGet(() -> createDefaultContent(normalizedKey));

        return ResponseEntity.ok(toResponseDto(content, normalizedKey));
    }

    @PutMapping("/{sectionKey}")
    @com.wbscouting.api.security.audit.AuditAction(action = "UPDATE", resource = "INSTITUTIONAL_TRANSLATION", description = "Atualização de conteúdos bilíngues (PT/EN)")
    public ResponseEntity<SectionTranslationResponseDto> updateSectionTranslations(
            @PathVariable String sectionKey,
            @RequestBody TranslationUpdateRequestDto request,
            Authentication authentication
    ) {
        return handleUpdateTranslations(sectionKey, request, authentication);
    }

    @PatchMapping("/{sectionKey}")
    @com.wbscouting.api.security.audit.AuditAction(action = "UPDATE", resource = "INSTITUTIONAL_TRANSLATION", description = "Atualização de conteúdos bilíngues (PT/EN)")
    public ResponseEntity<SectionTranslationResponseDto> patchSectionTranslations(
            @PathVariable String sectionKey,
            @RequestBody TranslationUpdateRequestDto request,
            Authentication authentication
    ) {
        return handleUpdateTranslations(sectionKey, request, authentication);
    }

    private ResponseEntity<SectionTranslationResponseDto> handleUpdateTranslations(
            String sectionKey,
            TranslationUpdateRequestDto request,
            Authentication authentication
    ) {
        log.info("Atualizando traduções paralelas da seção: {}", sectionKey);
        String normalizedKey = sectionKey.trim().toUpperCase(Locale.ROOT);

        SiteContent content = findSectionWithAlias(normalizedKey)
                .orElseGet(() -> createDefaultContent(normalizedKey));
        content.setSectionKey(normalizedKey);

        UUID adminId = extractAdminId(authentication);
        if (adminId != null) {
            content.setUpdatedBy(adminId);
        }

        TranslationContentDto ptDto = request.getResolvedPt();
        TranslationContentDto enDto = request.getResolvedEn();

        String ptText = ptDto.getContent() != null && !ptDto.getContent().isBlank() ? ptDto.getContent() : (ptDto.getBody() != null ? ptDto.getBody() : "");
        String enText = enDto.getContent() != null && !enDto.getContent().isBlank() ? enDto.getContent() : (enDto.getBody() != null ? enDto.getBody() : "");

        boolean isSimpleContent = "TERMS".equals(normalizedKey) || SECTION_TERMS_OF_USE.equals(normalizedKey)
                || "PRIVACY".equals(normalizedKey) || SECTION_PRIVACY_POLICY.equals(normalizedKey);

        Map<String, Object> ptMap;
        Map<String, Object> enMap;

        if (isSimpleContent) {
            ptMap = new HashMap<>();
            ptMap.put("content", ptText);

            enMap = new HashMap<>();
            enMap.put("content", enText);
        } else {
            ptMap = content.getPayloadPt() != null ? new HashMap<>(content.getPayloadPt()) : new HashMap<>();
            ptMap.put("headline", ptDto.getHeadline() != null ? ptDto.getHeadline() : "");
            ptMap.put("quote", ptDto.getQuote() != null ? ptDto.getQuote() : "");
            ptMap.put("sectionTitle", ptDto.getSectionTitle() != null ? ptDto.getSectionTitle() : "");
            ptMap.put("body", ptText);
            ptMap.put("content", ptText);

            enMap = content.getPayloadEn() != null ? new HashMap<>(content.getPayloadEn()) : new HashMap<>();
            enMap.put("headline", enDto.getHeadline() != null ? enDto.getHeadline() : "");
            enMap.put("quote", enDto.getQuote() != null ? enDto.getQuote() : "");
            enMap.put("sectionTitle", enDto.getSectionTitle() != null ? enDto.getSectionTitle() : "");
            enMap.put("body", enText);
            enMap.put("content", enText);
        }

        content.setPayloadPt(ptMap);
        content.setPayloadEn(enMap);

        SiteContent saved = siteContentRepository.save(content);

        // Sincroniza aliases bidirecionalmente para TERMS e PRIVACY garantindo persistencia em ambas as chaves
        if ("TERMS".equals(normalizedKey) || SECTION_TERMS_OF_USE.equals(normalizedKey)) {
            String targetAlias = "TERMS".equals(normalizedKey) ? SECTION_TERMS_OF_USE : "TERMS";
            SiteContent aliasContent = siteContentRepository.findBySectionKey(targetAlias)
                    .orElseGet(() -> SiteContent.builder().sectionKey(targetAlias).build());
            aliasContent.setPayloadPt(saved.getPayloadPt());
            aliasContent.setPayloadEn(saved.getPayloadEn());
            aliasContent.setUpdatedAt(OffsetDateTime.now());
            siteContentRepository.save(aliasContent);
        }
        if ("PRIVACY".equals(normalizedKey) || SECTION_PRIVACY_POLICY.equals(normalizedKey)) {
            String targetAlias = "PRIVACY".equals(normalizedKey) ? SECTION_PRIVACY_POLICY : "PRIVACY";
            SiteContent aliasContent = siteContentRepository.findBySectionKey(targetAlias)
                    .orElseGet(() -> SiteContent.builder().sectionKey(targetAlias).build());
            aliasContent.setPayloadPt(saved.getPayloadPt());
            aliasContent.setPayloadEn(saved.getPayloadEn());
            aliasContent.setUpdatedAt(OffsetDateTime.now());
            siteContentRepository.save(aliasContent);
        }

        if (translationRepository != null) {
            try {
                upsertTranslation("pt", normalizedKey + ".headline", ptDto.getHeadline());
                upsertTranslation("pt", normalizedKey + ".quote", ptDto.getQuote());
                upsertTranslation("pt", normalizedKey + ".sectionTitle", ptDto.getSectionTitle());
                upsertTranslation("pt", normalizedKey + ".body", ptDto.getBody());
                upsertTranslation("en", normalizedKey + ".headline", enDto.getHeadline());
                upsertTranslation("en", normalizedKey + ".quote", enDto.getQuote());
                upsertTranslation("en", normalizedKey + ".sectionTitle", enDto.getSectionTitle());
                upsertTranslation("en", normalizedKey + ".body", enDto.getBody());
            } catch (Exception ex) {
                log.warn("Erro ao sincronizar tabela translations: {}", ex.getMessage());
            }
        }

        if (SECTION_ABOUT_MANIFESTO.equalsIgnoreCase(normalizedKey) && institutionalSettingRepository != null) {
            try {
                com.wbscouting.api.entity.InstitutionalSetting setting = institutionalSettingRepository.findBySettingKey("ABOUT_PAGE")
                        .orElseGet(() -> com.wbscouting.api.entity.InstitutionalSetting.builder()
                                .settingKey("ABOUT_PAGE")
                                .build());

                if (ptDto.getHeadline() != null) {
                    setting.setTitle(ptDto.getHeadline().trim());
                }
                if (ptDto.getBody() != null) {
                    setting.setDescription(ptDto.getBody().trim());
                }
                Map<String, Object> data = setting.getContentDataWithFallback();
                if (data == null) data = new LinkedHashMap<>();
                if (ptDto.getQuote() != null) data.put("heroQuote", ptDto.getQuote().trim());
                if (ptDto.getSectionTitle() != null) {
                    data.put("sectionTitle", ptDto.getSectionTitle().trim());
                    data.put("manifestoTitle", ptDto.getSectionTitle().trim());
                }
                if (ptDto.getBody() != null) data.put("manifestoText", ptDto.getBody().trim());
                setting.setContentData(data);
                setting.setContentJson(data);
                institutionalSettingRepository.save(setting);
                log.info("Sincronização de ABOUT_MANIFESTO com institutional_settings (ABOUT_PAGE) realizada com sucesso");
            } catch (Exception ex) {
                log.warn("Erro ao sincronizar institutionalSettingRepository: {}", ex.getMessage());
            }
        }

        return ResponseEntity.ok(toResponseDto(saved, normalizedKey));
    }

    private void upsertTranslation(String locale, String key, String value) {
        if (value == null) return;
        Optional<com.wbscouting.api.entity.Translation> opt = translationRepository.findByLocaleAndKey(locale, key);
        com.wbscouting.api.entity.Translation t = opt.orElseGet(() -> com.wbscouting.api.entity.Translation.builder()
                .locale(locale)
                .key(key)
                .createdAt(OffsetDateTime.now())
                .build());
        t.setValue(value);
        t.setUpdatedAt(OffsetDateTime.now());
        translationRepository.save(t);
    }

    private SectionTranslationResponseDto toResponseDto(SiteContent content, String sectionKey) {
        Map<String, Object> pt = content.getPayloadPt() != null ? content.getPayloadPt() : Collections.emptyMap();
        Map<String, Object> en = content.getPayloadEn() != null ? content.getPayloadEn() : Collections.emptyMap();

        String ptQuote = (String) pt.getOrDefault("quote", "");
        String ptSectionTitle = (String) pt.getOrDefault("sectionTitle", "");
        String enQuote = (String) en.getOrDefault("quote", "");
        String enSectionTitle = (String) en.getOrDefault("sectionTitle", "");

        String ptContent = (String) pt.getOrDefault("content", pt.getOrDefault("body", ""));
        String enContent = (String) en.getOrDefault("content", en.getOrDefault("body", ""));

        TranslationContentDto ptDto = TranslationContentDto.builder()
                .headline((String) pt.getOrDefault("headline", ""))
                .quote(ptQuote)
                .sectionTitle(ptSectionTitle)
                .body(ptContent)
                .content(ptContent)
                .build();

        TranslationContentDto enDto = TranslationContentDto.builder()
                .headline((String) en.getOrDefault("headline", ""))
                .quote(enQuote)
                .sectionTitle(enSectionTitle)
                .body(enContent)
                .content(enContent)
                .build();

        return SectionTranslationResponseDto.builder()
                .sectionKey(sectionKey)
                .title(resolveTitle(sectionKey))
                .lastUpdated(content.getUpdatedAt() != null ? content.getUpdatedAt() : OffsetDateTime.now())
                .translations(BilingualTranslationsDto.builder()
                        .pt(ptDto)
                        .en(enDto)
                        .build())
                .build();
    }

    private String resolveTitle(String sectionKey) {
        return switch (sectionKey) {
            case SECTION_ABOUT_MANIFESTO -> "Manifesto da Agência (Sobre Nós)";
            case SECTION_SCOUTING_GUIDELINES -> "Diretrizes de Scouting (Seja Modelo)";
            case SECTION_APPLY_HOW_IT_WORKS -> "Próximos Passos Apply (Como Funciona o Scouting)";
            case "TERMS", SECTION_TERMS_OF_USE -> "Termos de Uso";
            case "PRIVACY", SECTION_PRIVACY_POLICY -> "Privacidade & LGPD";
            default -> sectionKey;
        };
    }

    private SiteContent createDefaultContent(String sectionKey) {
        Map<String, Object> pt = new HashMap<>();
        Map<String, Object> en = new HashMap<>();

        switch (sectionKey) {
            case SECTION_ABOUT_MANIFESTO -> {
                pt.put("headline", "A Nova Estética do Scouting Global");
                pt.put("quote", "Acreditamos na autenticidade, na força da personalidade e na beleza singular de cada indivíduo.");
                pt.put("sectionTitle", "Nossa Filosofia");
                pt.put("body", "Conectamos talentos às principais marcas com curadoria estratégica, visão de vanguarda e compromisso com o desenvolvimento humano e profissional em escala global.");

                en.put("headline", "The New Aesthetic of Global Scouting");
                en.put("quote", "We believe in authenticity, personal strength, and the unique beauty of every individual.");
                en.put("sectionTitle", "Our Philosophy");
                en.put("body", "We connect talent to leading global brands with strategic curation, avant-garde vision, and a commitment to human and professional growth on a global scale.");
            }
            case SECTION_SCOUTING_GUIDELINES -> {
                pt.put("headline", "Critérios e Recomendações de Envio");
                pt.put("quote", "Transparência, naturalidade e conformidade documental.");
                pt.put("body", "Para avaliação do casting internacional, solicitamos polaroids digitais sem maquiagem e com iluminação natural. Candidatos menores de idade devem submeter a anuência prévia dos responsáveis legais.");

                en.put("headline", "Scouting Standards & Submission Guidelines");
                en.put("quote", "Transparency, natural posture, and legal compliance.");
                en.put("body", "For international casting evaluation, we require clean digital polaroids without styling or makeup, captured in natural daylight. Submissions from under-age talents strictly require prior verified parental consent.");
            }
            case "TERMS", SECTION_TERMS_OF_USE -> {
                String ptText = "Termos e Condições de Uso da WB Agency.\n\nAo acessar e utilizar este website, você concorda expressamente com os termos e condições aqui estabelecidos. O conteúdo, fotografias, marcas e composites são de titularidade da WB Agency ou de seus parceiros credenciados.";
                pt.put("headline", "Termos e Condições de Uso da WB Agency");
                pt.put("quote", "Proteção patrimonial, segurança jurídica e transparência no agenciamento.");
                pt.put("body", ptText);
                pt.put("content", ptText);

                String enText = "WB Agency Terms of Use.\n\nBy accessing and using this website, you agree to comply with the terms and conditions set forth herein. All imagery, trademarks, composites, and texts are property of WB Agency or accredited partners.";
                en.put("headline", "WB Agency Terms of Use");
                en.put("quote", "Asset protection, legal compliance, and agency transparency.");
                en.put("body", enText);
                en.put("content", enText);
            }
            case "PRIVACY", SECTION_PRIVACY_POLICY -> {
                String ptText = "Política de Privacidade & Diretrizes LGPD (Lei nº 13.709/2018).\n\nA WB Agency trata dados pessoais exclusivamente para finalidades de triagem, comunicação profissional e representação artística. Garantimos o sigilo de fotografias de candidaturas e o direito de exclusão conforme a legislação vigente.";
                pt.put("headline", "Política de Privacidade & Diretrizes LGPD");
                pt.put("quote", "Conformidade rigorosa com a LGPD e o Regulamento Geral de Proteção de Dados (GDPR).");
                pt.put("body", ptText);
                pt.put("content", ptText);

                String enText = "Privacy Policy & GDPR/LGPD Compliance.\n\nWB Agency handles personal data strictly for casting screening, professional communication, and representation. Candidate photos and personal data are kept confidential under strict legal guidelines.";
                en.put("headline", "Privacy Policy & GDPR/LGPD Compliance");
                en.put("quote", "Strict compliance with LGPD and General Data Protection Regulation (GDPR).");
                en.put("body", enText);
                en.put("content", enText);
            }
            case SECTION_APPLY_HOW_IT_WORKS -> {
                // 🆕 Conteúdo padrão do texto "Próximos Passos • Como Funciona" exibido no /apply apos envio com sucesso.
                pt.put("headline", "Próximos Passos • Como Funciona");
                pt.put("quote", "Transparência total no processo de avaliação de novos talentos.");
                pt.put("body", "Nossa diretoria de casting analisa todas as candidaturas em até 5 dias úteis.|||Em caso de compatibilidade de perfil com nosso casting comercial ou fashion, nossa equipe entrará em contato via telefone ou e-mail cadastrado.|||A WB Agency nunca cobra taxas para avaliação de perfil ou agenciamento inicial.");

                en.put("headline", "Next Steps • How It Works");
                en.put("quote", "Full transparency throughout our new talent evaluation workflow.");
                en.put("body", "Our casting board reviews every submission within 5 business days.|||When your profile matches our commercial or high fashion rosters, our scouting team contacts you via the phone or email you registered.|||WB Agency never charges assessment fees or upfront agency deposits of any kind.");
            }
            default -> {
                pt.put("headline", "Título Institucional");
                pt.put("quote", "Destaque conceitual");
                pt.put("body", "Texto descritivo em português.");

                en.put("headline", "Institutional Title");
                en.put("quote", "Conceptual highlight");
                en.put("body", "English descriptive text.");
            }
        }

        SiteContent content = SiteContent.builder()
                .sectionKey(sectionKey)
                .payloadPt(pt)
                .payloadEn(en)
                .updatedAt(OffsetDateTime.now())
                .build();

        return siteContentRepository.save(content);
    }

    private Optional<SiteContent> findSectionWithAlias(String sectionKey) {
        if (sectionKey == null) return Optional.empty();
        Optional<SiteContent> opt = siteContentRepository.findBySectionKey(sectionKey);
        if (opt.isPresent()) return opt;
        if ("TERMS".equalsIgnoreCase(sectionKey)) {
            return siteContentRepository.findBySectionKey(SECTION_TERMS_OF_USE);
        }
        if ("TERMS_OF_USE".equalsIgnoreCase(sectionKey)) {
            return siteContentRepository.findBySectionKey("TERMS");
        }
        if ("PRIVACY".equalsIgnoreCase(sectionKey)) {
            return siteContentRepository.findBySectionKey(SECTION_PRIVACY_POLICY);
        }
        if ("PRIVACY_POLICY".equalsIgnoreCase(sectionKey)) {
            return siteContentRepository.findBySectionKey("PRIVACY");
        }
        return Optional.empty();
    }

    private UUID extractAdminId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Admin admin) {
            return admin.getId();
        }
        return null;
    }
}

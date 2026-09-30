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
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CONTENT_ADMIN', 'ADMIN')")
@RequiredArgsConstructor
public class AdminInstitutionalTranslationController {

    public static final String SECTION_ABOUT_MANIFESTO = "ABOUT_MANIFESTO";
    public static final String SECTION_SCOUTING_GUIDELINES = "SCOUTING_GUIDELINES";
    public static final String SECTION_TERMS_OF_USE = "TERMS_OF_USE";
    public static final String SECTION_PRIVACY_POLICY = "PRIVACY_POLICY";

    private final SiteContentRepository siteContentRepository;

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

        SiteContent content = siteContentRepository.findBySectionKey(normalizedKey)
                .orElseGet(() -> createDefaultContent(normalizedKey));

        return ResponseEntity.ok(toResponseDto(content, normalizedKey));
    }

    @PutMapping("/{sectionKey}")
    public ResponseEntity<SectionTranslationResponseDto> updateSectionTranslations(
            @PathVariable String sectionKey,
            @RequestBody TranslationUpdateRequestDto request,
            Authentication authentication
    ) {
        return handleUpdateTranslations(sectionKey, request, authentication);
    }

    @PatchMapping("/{sectionKey}")
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

        SiteContent content = siteContentRepository.findBySectionKey(normalizedKey)
                .orElseGet(() -> createDefaultContent(normalizedKey));

        UUID adminId = extractAdminId(authentication);
        if (adminId != null) {
            content.setUpdatedBy(adminId);
        }

        TranslationContentDto ptDto = request.getResolvedPt();
        TranslationContentDto enDto = request.getResolvedEn();

        Map<String, Object> ptMap = content.getPayloadPt() != null ? new HashMap<>(content.getPayloadPt()) : new HashMap<>();
        ptMap.put("headline", ptDto.getHeadline() != null ? ptDto.getHeadline() : "");
        ptMap.put("quote", ptDto.getQuote() != null ? ptDto.getQuote() : "");
        ptMap.put("body", ptDto.getBody() != null ? ptDto.getBody() : "");
        content.setPayloadPt(ptMap);

        Map<String, Object> enMap = content.getPayloadEn() != null ? new HashMap<>(content.getPayloadEn()) : new HashMap<>();
        enMap.put("headline", enDto.getHeadline() != null ? enDto.getHeadline() : "");
        enMap.put("quote", enDto.getQuote() != null ? enDto.getQuote() : "");
        enMap.put("body", enDto.getBody() != null ? enDto.getBody() : "");
        content.setPayloadEn(enMap);

        SiteContent saved = siteContentRepository.save(content);

        return ResponseEntity.ok(toResponseDto(saved, normalizedKey));
    }

    private SectionTranslationResponseDto toResponseDto(SiteContent content, String sectionKey) {
        Map<String, Object> pt = content.getPayloadPt() != null ? content.getPayloadPt() : Collections.emptyMap();
        Map<String, Object> en = content.getPayloadEn() != null ? content.getPayloadEn() : Collections.emptyMap();

        TranslationContentDto ptDto = TranslationContentDto.builder()
                .headline((String) pt.getOrDefault("headline", ""))
                .quote((String) pt.getOrDefault("quote", ""))
                .body((String) pt.getOrDefault("body", ""))
                .build();

        TranslationContentDto enDto = TranslationContentDto.builder()
                .headline((String) en.getOrDefault("headline", ""))
                .quote((String) en.getOrDefault("quote", ""))
                .body((String) en.getOrDefault("body", ""))
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
            case SECTION_TERMS_OF_USE -> "Termos de Uso & Direitos de Imagem";
            case SECTION_PRIVACY_POLICY -> "Política de Privacidade (LGPD / GDPR)";
            default -> sectionKey;
        };
    }

    private SiteContent createDefaultContent(String sectionKey) {
        Map<String, Object> pt = new HashMap<>();
        Map<String, Object> en = new HashMap<>();

        switch (sectionKey) {
            case SECTION_ABOUT_MANIFESTO -> {
                pt.put("headline", "A Nova Estética do Scouting Global");
                pt.put("quote", "A beleza contemporânea nasce da singularidade e precisão.");
                pt.put("body", "A WB Agency consolidou-se como um núcleo editorial focado no desenvolvimento integral de modelos para os principais mercados da moda internacional. Nossa metodologia rejeita a padronização e prioriza a identidade visual autêntica, conectando talentos a marcas com relevância estética global.");

                en.put("headline", "The New Aesthetic of Global Scouting");
                en.put("quote", "Contemporary beauty stems from uniqueness and precision.");
                en.put("body", "WB Agency has established itself as an editorial powerhouse dedicated to the comprehensive development of models for premier global fashion markets. Our scouting methodology moves beyond mass standards to foster authentic personal identity, positioning talents at the intersection of high fashion and international relevance.");
            }
            case SECTION_SCOUTING_GUIDELINES -> {
                pt.put("headline", "Critérios e Recomendações de Envio");
                pt.put("quote", "Transparência, naturalidade e conformidade documental.");
                pt.put("body", "Para avaliação do casting internacional, solicitamos polaroids digitais sem maquiagem e com iluminação natural. Candidatos menores de idade devem submeter a anuência prévia dos responsáveis legais.");

                en.put("headline", "Scouting Standards & Submission Guidelines");
                en.put("quote", "Transparency, natural posture, and legal compliance.");
                en.put("body", "For international casting evaluation, we require clean digital polaroids without styling or makeup, captured in natural daylight. Submissions from under-age talents strictly require prior verified parental consent.");
            }
            case SECTION_TERMS_OF_USE -> {
                pt.put("headline", "Termos e Condições de Uso da Plataforma");
                pt.put("quote", "Proteção patrimonial, segurança jurídica e transparência no agenciamento.");
                pt.put("body", "O acesso e a utilização dos serviços da WB Agency regem-se pelas normas de propriedade intelectual e direitos autorais internacionais. O uso não autorizado de books e composites é estritamente proibido.");

                en.put("headline", "Terms and Conditions of Platform Use");
                en.put("quote", "Asset protection, legal compliance, and agency transparency.");
                en.put("body", "Access to and use of WB Agency services are governed by international intellectual property laws. Unauthorized reproduction of model books and digital composites is strictly prohibited.");
            }
            case SECTION_PRIVACY_POLICY -> {
                pt.put("headline", "Privacidade e Proteção de Dados Pessoais");
                pt.put("quote", "Conformidade rigorosa com a LGPD e o Regulamento Geral de Proteção de Dados (GDPR).");
                pt.put("body", "Coletamos e processamos dados biométricos e fotográficos exclusivamente para avaliação técnica de agenciamento e submissão a castings internacionais, com opção permanente de exclusão segura a pedido do titular.");

                en.put("headline", "Privacy Policy & Personal Data Protection");
                en.put("quote", "Strict compliance with LGPD and General Data Protection Regulation (GDPR).");
                en.put("body", "We collect and process biometric and photographic data exclusively for casting assessment and international booking submissions, with full rights of safe data erasure upon user request.");
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

    private UUID extractAdminId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Admin admin) {
            return admin.getId();
        }
        return null;
    }
}

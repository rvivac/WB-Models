package com.wbscouting.api.service.content;

import com.wbscouting.api.config.SupabaseProperties;
import com.wbscouting.api.dto.content.AssetUploadResponseDto;
import com.wbscouting.api.dto.content.SiteContentAdminDto;
import com.wbscouting.api.dto.content.SiteContentPublicDto;
import com.wbscouting.api.dto.content.SiteContentUpdateRequestDto;
import com.wbscouting.api.entity.InstitutionalSetting;
import com.wbscouting.api.entity.SiteContent;
import com.wbscouting.api.exception.FileSizeExceededException;
import com.wbscouting.api.exception.InvalidFileException;
import com.wbscouting.api.exception.ResourceNotFoundException;
import com.wbscouting.api.repository.SiteContentRepository;
import com.wbscouting.api.service.storage.StorageService;
import com.wbscouting.api.service.storage.SupabaseStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SiteContentServiceImpl implements SiteContentService {

    private final SiteContentRepository siteContentRepository;
    private final StorageService storageService;
    private final SupabaseProperties supabaseProperties;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.wbscouting.api.repository.InstitutionalSettingRepository institutionalSettingRepository;

    @Override
    @Transactional(readOnly = true)
    public SiteContentPublicDto getPublicContent(String sectionKey, String lang) {
        log.info("Consultando conteúdo público para sectionKey='{}', lang='{}'", sectionKey, lang);

        String upper = sectionKey != null ? sectionKey.trim().toUpperCase(Locale.ROOT) : "";
        String normalizedKey = switch (upper) {
            case "TERMS", "TERMS_OF_USE" -> "TERMS";
            case "PRIVACY", "PRIVACY_POLICY" -> "PRIVACY";
            case "ABOUT", "MANIFESTO" -> "ABOUT_MANIFESTO";
            default -> upper;
        };

        SiteContent content = findSectionWithAlias(normalizedKey)
                .orElseGet(() -> {
                    if ("ABOUT_MANIFESTO".equalsIgnoreCase(normalizedKey)) {
                        return createDefaultAboutManifesto();
                    }
                    // 🆕 Fallback para APPLY_HOW_IT_WORKS: nunca da 404 no /apply
                    if ("APPLY_HOW_IT_WORKS".equalsIgnoreCase(normalizedKey)) {
                        return createDefaultApplyHowItWorks();
                    }
                    if ("TERMS".equalsIgnoreCase(normalizedKey)) {
                        return createDefaultTerms();
                    }
                    if ("PRIVACY".equalsIgnoreCase(normalizedKey)) {
                        return createDefaultPrivacy();
                    }
                    throw new ResourceNotFoundException("Conteúdo da seção não encontrado: " + sectionKey);
                });

        String resolvedLang = resolveLanguage(lang);
        Map<String, Object> resolvedPayload = resolvePayloadByLanguage(content, resolvedLang);

        String extractedContent = "";
        if (resolvedPayload != null) {
            if (resolvedPayload.containsKey("content") && resolvedPayload.get("content") != null) {
                extractedContent = resolvedPayload.get("content").toString();
            } else if (resolvedPayload.containsKey("body") && resolvedPayload.get("body") != null) {
                extractedContent = resolvedPayload.get("body").toString();
            } else if (resolvedPayload.containsKey("text") && resolvedPayload.get("text") != null) {
                extractedContent = resolvedPayload.get("text").toString();
            }
        }

        return SiteContentPublicDto.builder()
                .sectionKey(content.getSectionKey())
                .payload(resolvedPayload)
                .mediaUrls(content.getMediaUrls())
                .lang(resolvedLang)
                .content(extractedContent)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Map<String, Object>> getAllPublicContent(String lang) {
        String resolvedLang = resolveLanguage(lang);
        log.info("Consultando mapa consolidado de conteúdos públicos para lang='{}'", resolvedLang);

        List<SiteContent> contents = siteContentRepository.findAll();
        Map<String, Map<String, Object>> consolidated = new LinkedHashMap<>();

        for (SiteContent content : contents) {
            Map<String, Object> payload = resolvePayloadByLanguage(content, resolvedLang);
            consolidated.put(content.getSectionKey(), payload != null ? payload : Collections.emptyMap());
        }

        return consolidated;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SiteContentAdminDto> getAllAdminContent() {
        log.info("Listando todas as seções e payloads de conteúdos para painel CMS");

        return siteContentRepository.findAll().stream()
                .map(this::toAdminDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SiteContentAdminDto updateContent(String sectionKey, SiteContentUpdateRequestDto dto, UUID adminId) {
        log.info("Executando upsert de conteúdo institucional para sectionKey='{}', adminId={}", sectionKey, adminId);

        SiteContent content = siteContentRepository.findBySectionKey(sectionKey)
                .orElseGet(() -> SiteContent.builder()
                        .sectionKey(sectionKey)
                        .build());

        content.setPayloadPt(dto.getPayloadPt());
        content.setPayloadEn(dto.getPayloadEn());
        if (dto.getMediaUrls() != null) {
            content.setMediaUrls(dto.getMediaUrls());
        }
        content.setUpdatedBy(adminId);

        SiteContent saved = siteContentRepository.save(content);
        log.info("Conteúdo institucional da seção '{}' atualizado com sucesso.", sectionKey);

        if ("ABOUT_MANIFESTO".equalsIgnoreCase(sectionKey) && institutionalSettingRepository != null && dto.getPayloadPt() != null) {
            try {
                Map<String, Object> pt = dto.getResolvedPayloadPt();
                String headline = (String) pt.get("headline");
                String quote = (String) pt.get("quote");
                String sectionTitle = (String) pt.get("sectionTitle");
                String body = (String) pt.get("body");
                InstitutionalSetting setting = institutionalSettingRepository.findBySettingKey("ABOUT_PAGE")
                        .orElseGet(() -> InstitutionalSetting.builder()
                                .settingKey("ABOUT_PAGE")
                                .build());

                if (headline != null) {
                    setting.setTitle(headline.trim());
                }
                if (body != null) {
                    setting.setDescription(body.trim());
                }
                Map<String, Object> data = setting.getContentDataWithFallback();
                if (data == null) data = new LinkedHashMap<>();
                if (quote != null) data.put("heroQuote", quote.trim());
                if (sectionTitle != null) {
                    data.put("sectionTitle", sectionTitle.trim());
                    data.put("manifestoTitle", sectionTitle.trim());
                }
                if (body != null) data.put("manifestoText", body.trim());
                setting.setContentData(data);
                setting.setContentJson(data);
                institutionalSettingRepository.save(setting);
                log.info("Sincronização de ABOUT_MANIFESTO com institutional_settings (ABOUT_PAGE) via SiteContentService concluída com sucesso");
            } catch (Exception ex) {
                log.warn("Erro ao sincronizar institutionalSettingRepository no SiteContentServiceImpl: {}", ex.getMessage());
            }
        }

        return toAdminDto(saved);
    }

    @Override
    public AssetUploadResponseDto uploadAsset(MultipartFile file, String folder) {
        log.info("Iniciando upload de ativo institucional. Nome='{}', Pasta='{}'",
                file != null ? file.getOriginalFilename() : "null", folder);

        String bucket = resolveBucketName();
        validateAssetFile(file, bucket);

        String targetFolder = StringUtils.hasText(folder) ? folder.trim() : "assets";
        String sanitizedFilename = sanitizeFilename(file.getOriginalFilename());
        String storagePath = String.format("%s/%s-%s", targetFolder, UUID.randomUUID(), sanitizedFilename);

        storageService.uploadFile(bucket, storagePath, file);
        String publicUrl = storageService.getPublicUrl(bucket, storagePath);

        log.info("Upload de ativo institucional concluído com sucesso: URL={}", publicUrl);

        return AssetUploadResponseDto.builder()
                .fileUrl(publicUrl)
                .storagePath(storagePath)
                .fileType(file.getContentType())
                .fileSizeBytes(file.getSize())
                .build();
    }

    private String resolveLanguage(String lang) {
        if (StringUtils.hasText(lang) && "en".equalsIgnoreCase(lang.trim())) {
            return "en";
        }
        return "pt";
    }

    private Map<String, Object> resolvePayloadByLanguage(SiteContent content, String lang) {
        if ("en".equalsIgnoreCase(lang)) {
            if (content.getPayloadEn() != null && !content.getPayloadEn().isEmpty()) {
                return content.getPayloadEn();
            }
            // Fallback para PT
            return content.getPayloadPt();
        }

        // Padrão PT
        if (content.getPayloadPt() != null && !content.getPayloadPt().isEmpty()) {
            return content.getPayloadPt();
        }
        return content.getPayloadEn();
    }

    private SiteContentAdminDto toAdminDto(SiteContent content) {
        return SiteContentAdminDto.builder()
                .id(content.getId())
                .sectionKey(content.getSectionKey())
                .payloadPt(content.getPayloadPt())
                .payloadEn(content.getPayloadEn())
                .mediaUrls(content.getMediaUrls())
                .createdAt(content.getCreatedAt())
                .updatedAt(content.getUpdatedAt())
                .updatedBy(content.getUpdatedBy())
                .build();
    }

    private String sanitizeFilename(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "file";
        }
        return filename.trim().replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String resolveBucketName() {
        return supabaseProperties.resolveBucketSiteAssets();
    }

    private void validateAssetFile(MultipartFile file, String bucket) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("O arquivo para upload é obrigatório e não pode estar vazio.");
        }
        long maxSize = SupabaseStorageService.MAX_SIZE_SITE_ASSETS;
        if (file.getSize() > maxSize) {
            long maxMb = maxSize / (1024 * 1024);
            throw new FileSizeExceededException(
                    String.format("O arquivo enviado (%.2f MB) excede o limite máximo permitido de %d MB para o bucket '%s'.",
                            file.getSize() / (1024.0 * 1024.0), maxMb, bucket));
        }
        String contentType = file.getContentType();
        boolean isImage = SupabaseStorageService.ALLOWED_IMAGE_TYPES.contains(contentType);
        boolean isVideo = SupabaseStorageService.ALLOWED_VIDEO_TYPES.contains(contentType);
        if (!isImage && !isVideo) {
            throw new InvalidFileException(
                    String.format("Formato de arquivo '%s' não suportado para o bucket '%s'. Tipos permitidos: imagens (%s) e vídeos (%s).",
                            contentType, bucket,
                            String.join(", ", SupabaseStorageService.ALLOWED_IMAGE_TYPES),
                            String.join(", ", SupabaseStorageService.ALLOWED_VIDEO_TYPES)));
        }
    }

    private SiteContent createDefaultAboutManifesto() {
        Map<String, Object> pt = new HashMap<>();
        pt.put("headline", "A Nova Estética do Scouting Global");
        pt.put("quote", "Acreditamos na autenticidade, na força da personalidade e na beleza singular de cada indivíduo.");
        pt.put("sectionTitle", "Nossa Filosofia");
        pt.put("body", "Conectamos talentos às principais marcas com curadoria estratégica, visão de vanguarda e compromisso com o desenvolvimento humano e profissional em escala global.");

        Map<String, Object> en = new HashMap<>();
        en.put("headline", "The New Aesthetic of Global Scouting");
        en.put("quote", "We believe in authenticity, personal strength, and the unique beauty of every individual.");
        en.put("sectionTitle", "Our Philosophy");
        en.put("body", "We connect talent to leading global brands with strategic curation, avant-garde vision, and a commitment to human and professional growth on a global scale.");

        return SiteContent.builder()
                .sectionKey("ABOUT_MANIFESTO")
                .payloadPt(pt)
                .payloadEn(en)
                .build();
    }

    // 🆕 Fallback padrão para a seção de Próximos Passos / Como Funciona do Apply.
    // Idêntico ao conteúdo do i18n (apply_page.step1/2/3) em PT e EN.
    private SiteContent createDefaultApplyHowItWorks() {
        Map<String, Object> pt = new HashMap<>();
        pt.put("headline", "Próximos Passos • Como Funciona");
        pt.put("quote", "Transparência total no processo de avaliação de novos talentos.");
        // 3 passos separados por "|||" — o frontend faz split e renderiza <ul><li>
        pt.put("body", "Nossa diretoria de casting analisa todas as candidaturas em até 5 dias úteis.|||Em caso de compatibilidade de perfil com nosso casting comercial ou fashion, nossa equipe entrará em contato via telefone ou e-mail cadastrado.|||A WB Agency nunca cobra taxas para avaliação de perfil ou agenciamento inicial.");

        Map<String, Object> en = new HashMap<>();
        en.put("headline", "Next Steps • How It Works");
        en.put("quote", "Full transparency throughout our new talent evaluation workflow.");
        en.put("body", "Our casting board reviews every submission within 5 business days.|||When your profile matches our commercial or high fashion rosters, our scouting team contacts you via the phone or email you registered.|||WB Agency never charges assessment fees or upfront agency deposits of any kind.");

        return SiteContent.builder()
                .sectionKey("APPLY_HOW_IT_WORKS")
                .payloadPt(pt)
                .payloadEn(en)
                .build();
    }

    private Optional<SiteContent> findSectionWithAlias(String sectionKey) {
        if (sectionKey == null) return Optional.empty();
        String upper = sectionKey.trim().toUpperCase(Locale.ROOT);
        String normalized = switch (upper) {
            case "TERMS", "TERMS_OF_USE" -> "TERMS";
            case "PRIVACY", "PRIVACY_POLICY" -> "PRIVACY";
            case "ABOUT", "MANIFESTO" -> "ABOUT_MANIFESTO";
            default -> upper;
        };

        Optional<SiteContent> opt = siteContentRepository.findBySectionKey(normalized);
        if (opt.isPresent()) return opt;

        opt = siteContentRepository.findBySectionKey(sectionKey.trim());
        if (opt.isPresent()) return opt;

        if ("TERMS".equals(normalized)) {
            return siteContentRepository.findBySectionKey("TERMS_OF_USE");
        }
        if ("PRIVACY".equals(normalized)) {
            return siteContentRepository.findBySectionKey("PRIVACY_POLICY");
        }
        if ("ABOUT_MANIFESTO".equals(normalized)) {
            return siteContentRepository.findBySectionKey("ABOUT");
        }
        return Optional.empty();
    }

    private SiteContent createDefaultTerms() {
        Map<String, Object> pt = new HashMap<>();
        String ptText = "Termos e Condições de Uso da WB Agency.\n\nAo acessar e utilizar este website, você concorda expressamente com os termos e condições aqui estabelecidos. O conteúdo, fotografias, marcas e composites são de titularidade da WB Agency ou de seus parceiros credenciados.";
        pt.put("content", ptText);
        pt.put("body", ptText);

        Map<String, Object> en = new HashMap<>();
        String enText = "WB Agency Terms of Use.\n\nBy accessing and using this website, you agree to comply with the terms and conditions set forth herein. All imagery, trademarks, composites, and texts are property of WB Agency or accredited partners.";
        en.put("content", enText);
        en.put("body", enText);

        return SiteContent.builder()
                .sectionKey("TERMS")
                .payloadPt(pt)
                .payloadEn(en)
                .build();
    }

    private SiteContent createDefaultPrivacy() {
        Map<String, Object> pt = new HashMap<>();
        String ptText = "Política de Privacidade & Diretrizes LGPD (Lei nº 13.709/2018).\n\nA WB Agency trata dados pessoais exclusivamente para finalidades de triagem, comunicação profissional e representação artística. Garantimos o sigilo de fotografias de candidaturas e o direito de exclusão conforme a legislação vigente.";
        pt.put("content", ptText);
        pt.put("body", ptText);

        Map<String, Object> en = new HashMap<>();
        String enText = "Privacy Policy & GDPR/LGPD Compliance.\n\nWB Agency handles personal data strictly for casting screening, professional communication, and representation. Candidate photos and personal data are kept confidential under strict legal guidelines.";
        en.put("content", enText);
        en.put("body", enText);

        return SiteContent.builder()
                .sectionKey("PRIVACY")
                .payloadPt(pt)
                .payloadEn(en)
                .build();
    }
}

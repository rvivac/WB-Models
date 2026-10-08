package com.wbscouting.api.service.content;

import com.wbscouting.api.constant.ContentSectionKey;
import com.wbscouting.api.dto.admin.contact.ContactChannelsUpdateRequestDto;
import com.wbscouting.api.dto.publicapi.ContactChannelsPublicDto;
import com.wbscouting.api.entity.SiteContent;
import com.wbscouting.api.repository.SiteContentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContactChannelsServiceImpl implements ContactChannelsService {

    private static final String DEFAULT_EMAIL = "info@wbagency.com.br";
    private static final String DEFAULT_WHATSAPP = "5511970656003";
    private static final String DEFAULT_MESSAGE_PT = "Olá! Gostaria de mais informações sobre a agência WB Agency.";
    private static final String DEFAULT_MESSAGE_EN = "Hello! I would like more information about WB Agency.";
    private static final String DEFAULT_INSTAGRAM = "@wbagency";
    private static final String DEFAULT_ADDRESS_PT = "São Paulo - SP, Brasil";
    private static final String DEFAULT_ADDRESS_EN = "São Paulo - SP, Brazil";
    private static final String DEFAULT_OFFICE_HOURS_PT = "Segunda a Sexta, das 09h às 18h";
    private static final String DEFAULT_OFFICE_HOURS_EN = "Monday to Friday, 9:00 AM - 6:00 PM";

    private final SiteContentRepository siteContentRepository;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.wbscouting.api.repository.ContactChannelRepository contactChannelRepository;

    @Override
    @Transactional(readOnly = true)
    public ContactChannelsPublicDto getContactChannels(String lang) {
        boolean isEn = "en".equalsIgnoreCase(lang);

        Optional<SiteContent> contentOpt = siteContentRepository.findBySectionKey(ContentSectionKey.CONTACT_INFO);
        if (contentOpt.isEmpty()) {
            contentOpt = siteContentRepository.findBySectionKey("contact");
        }

        String email = DEFAULT_EMAIL;
        String whatsappNumber = DEFAULT_WHATSAPP;
        String defaultMessage = isEn ? DEFAULT_MESSAGE_EN : DEFAULT_MESSAGE_PT;
        String instagramHandle = DEFAULT_INSTAGRAM;
        String address = isEn ? DEFAULT_ADDRESS_EN : DEFAULT_ADDRESS_PT;
        String officeHours = isEn ? DEFAULT_OFFICE_HOURS_EN : DEFAULT_OFFICE_HOURS_PT;

        // Se a tabela dedicada contact_channels tiver registros ativos, sobrepõe os dados institucionais
        if (contactChannelRepository != null) {
            try {
                List<com.wbscouting.api.entity.ContactChannel> activeChannels =
                        contactChannelRepository.findByActiveTrueOrderByDisplayOrderAsc();
                for (com.wbscouting.api.entity.ContactChannel ch : activeChannels) {
                    if ("EMAIL".equalsIgnoreCase(ch.getType()) && org.springframework.util.StringUtils.hasText(ch.getValue())) {
                        email = ch.getValue().trim();
                    } else if ("WHATSAPP".equalsIgnoreCase(ch.getType()) && org.springframework.util.StringUtils.hasText(ch.getValue())) {
                        whatsappNumber = ch.getValue().replaceAll("\\D+", "");
                    } else if ("INSTAGRAM".equalsIgnoreCase(ch.getType()) && org.springframework.util.StringUtils.hasText(ch.getValue())) {
                        instagramHandle = sanitizeInstagram(ch.getValue().trim());
                    } else if ("ADDRESS".equalsIgnoreCase(ch.getType()) && org.springframework.util.StringUtils.hasText(ch.getValue())) {
                        address = ch.getValue().trim();
                    } else if ("OFFICE_HOURS".equalsIgnoreCase(ch.getType()) && org.springframework.util.StringUtils.hasText(ch.getValue())) {
                        officeHours = ch.getValue().trim();
                    }
                }
            } catch (Exception ex) {
                log.warn("Erro ao ler canais dedicados da tabela contact_channels: {}", ex.getMessage());
            }
        }

        if (contentOpt.isPresent()) {
            SiteContent content = contentOpt.get();
            Map<String, Object> pt = content.getPayloadPt() != null ? content.getPayloadPt() : Map.of();
            Map<String, Object> en = content.getPayloadEn() != null ? content.getPayloadEn() : Map.of();

            if (isEn) {
                email = sanitizeVal(getString(en, "email", getString(pt, "email", DEFAULT_EMAIL)), DEFAULT_EMAIL);
                whatsappNumber = sanitizeVal(getString(en, "whatsappNumber", getString(pt, "whatsappNumber", DEFAULT_WHATSAPP)), DEFAULT_WHATSAPP);
                defaultMessage = getString(en, "whatsappDefaultMessage", getString(pt, "whatsappDefaultMessage", DEFAULT_MESSAGE_EN));
                instagramHandle = sanitizeInstagram(getString(en, "instagramHandle", getString(pt, "instagramHandle", DEFAULT_INSTAGRAM)));
                address = getString(en, "address", getString(pt, "address", DEFAULT_ADDRESS_EN));
                officeHours = getString(en, "officeHours", getString(pt, "officeHours", DEFAULT_OFFICE_HOURS_EN));
            } else {
                email = sanitizeVal(getString(pt, "email", DEFAULT_EMAIL), DEFAULT_EMAIL);
                whatsappNumber = sanitizeVal(getString(pt, "whatsappNumber", DEFAULT_WHATSAPP), DEFAULT_WHATSAPP);
                defaultMessage = getString(pt, "whatsappDefaultMessage", DEFAULT_MESSAGE_PT);
                instagramHandle = sanitizeInstagram(getString(pt, "instagramHandle", DEFAULT_INSTAGRAM));
                address = getString(pt, "address", DEFAULT_ADDRESS_PT);
                officeHours = getString(pt, "officeHours", DEFAULT_OFFICE_HOURS_PT);
            }
        }

        String sanitizedNumber = whatsappNumber != null ? whatsappNumber.replaceAll("\\D+", "") : "";
        String whatsappUrl = buildWhatsappUrl(sanitizedNumber, defaultMessage);

        return ContactChannelsPublicDto.builder()
                .email(email)
                .whatsappNumber(sanitizedNumber)
                .whatsappUrl(whatsappUrl)
                .instagramHandle(instagramHandle)
                .address(address)
                .officeHours(officeHours)
                .build();
    }

    @Override
    @Transactional
    public ContactChannelsPublicDto updateContactChannels(ContactChannelsUpdateRequestDto dto, UUID adminId) {
        log.info("Atualizando canais oficiais de contato por adminId: {}", adminId);

        String cleanNumber = dto.getWhatsappNumber().replaceAll("\\D+", "");

        Map<String, Object> payloadPt = new LinkedHashMap<>();
        payloadPt.put("email", dto.getEmail().trim());
        payloadPt.put("whatsappNumber", cleanNumber);
        payloadPt.put("whatsappDefaultMessage", dto.getWhatsappDefaultMessagePt().trim());
        payloadPt.put("instagramHandle", dto.getInstagramHandle() != null ? dto.getInstagramHandle().trim() : DEFAULT_INSTAGRAM);
        payloadPt.put("address", dto.getAddressPt().trim());
        payloadPt.put("officeHours", dto.getOfficeHoursPt().trim());

        Map<String, Object> payloadEn = new LinkedHashMap<>();
        payloadEn.put("email", dto.getEmail().trim());
        payloadEn.put("whatsappNumber", cleanNumber);
        payloadEn.put("whatsappDefaultMessage", StringUtils.hasText(dto.getWhatsappDefaultMessageEn())
                ? dto.getWhatsappDefaultMessageEn().trim()
                : dto.getWhatsappDefaultMessagePt().trim());
        payloadEn.put("instagramHandle", dto.getInstagramHandle() != null ? dto.getInstagramHandle().trim() : DEFAULT_INSTAGRAM);
        payloadEn.put("address", StringUtils.hasText(dto.getAddressEn())
                ? dto.getAddressEn().trim()
                : dto.getAddressPt().trim());
        payloadEn.put("officeHours", StringUtils.hasText(dto.getOfficeHoursEn())
                ? dto.getOfficeHoursEn().trim()
                : dto.getOfficeHoursPt().trim());

        SiteContent content = siteContentRepository.findBySectionKey(ContentSectionKey.CONTACT_INFO)
                .orElseGet(() -> SiteContent.builder()
                        .sectionKey(ContentSectionKey.CONTACT_INFO)
                        .build());

        content.setPayloadPt(payloadPt);
        content.setPayloadEn(payloadEn);
        content.setUpdatedBy(adminId);
        content.setUpdatedAt(OffsetDateTime.now());

        siteContentRepository.save(content);
        log.info("Canais de contato atualizados com sucesso sob a chave '{}'", ContentSectionKey.CONTACT_INFO);

        if (contactChannelRepository != null) {
            syncChannelsTable(dto);
        }

        return getContactChannels("pt");
    }

    private void syncChannelsTable(ContactChannelsUpdateRequestDto dto) {
        try {
            upsertChannel("EMAIL", dto.getEmail(), "E-mail Oficial", 1);
            upsertChannel("WHATSAPP", dto.getWhatsappNumber(), "WhatsApp Oficial", 2);
            if (StringUtils.hasText(dto.getInstagramHandle())) {
                upsertChannel("INSTAGRAM", dto.getInstagramHandle(), "Instagram Oficial", 3);
            }
            if (StringUtils.hasText(dto.getAddressPt())) {
                upsertChannel("ADDRESS", dto.getAddressPt(), "Endereço Matriz", 4);
            }
            if (StringUtils.hasText(dto.getOfficeHoursPt())) {
                upsertChannel("OFFICE_HOURS", dto.getOfficeHoursPt(), "Horário de Atendimento", 5);
            }
        } catch (Exception e) {
            log.warn("Erro ao sincronizar tabela contact_channels: {}", e.getMessage());
        }
    }

    private void upsertChannel(String type, String value, String label, int order) {
        if (!StringUtils.hasText(value)) return;
        List<com.wbscouting.api.entity.ContactChannel> existing =
                contactChannelRepository.findByTypeIgnoreCaseOrderByDisplayOrderAsc(type);
        if (!existing.isEmpty()) {
            com.wbscouting.api.entity.ContactChannel ch = existing.get(0);
            ch.setValue(value.trim());
            ch.setLabel(label);
            ch.setActive(true);
            ch.setUpdatedAt(OffsetDateTime.now());
            contactChannelRepository.save(ch);
        } else {
            com.wbscouting.api.entity.ContactChannel ch = com.wbscouting.api.entity.ContactChannel.builder()
                    .type(type.toUpperCase())
                    .value(value.trim())
                    .label(label)
                    .active(true)
                    .displayOrder(order)
                    .createdAt(OffsetDateTime.now())
                    .updatedAt(OffsetDateTime.now())
                    .build();
            contactChannelRepository.save(ch);
        }
    }

    private String buildWhatsappUrl(String sanitizedNumber, String message) {
        if (!StringUtils.hasText(sanitizedNumber)) {
            return null;
        }
        String encodedMessage = "";
        if (StringUtils.hasText(message)) {
            encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8).replace("+", "%20");
        }
        return "https://wa.me/" + sanitizedNumber + "?text=" + encodedMessage;
    }

    private String getString(Map<String, Object> map, String key, String defaultValue) {
        if (map == null || !map.containsKey(key)) {
            return defaultValue;
        }
        Object val = map.get(key);
        if (val == null) {
            return defaultValue;
        }
        String str = val.toString().trim();
        return str.isEmpty() ? defaultValue : str;
    }

    private String sanitizeVal(String val, String fallback) {
        if (!StringUtils.hasText(val) || val.contains("wbscouting.com") || val.contains("99999-9999") || val.equals("5511999999999")) {
            return fallback;
        }
        return val;
    }

    private String sanitizeInstagram(String val) {
        if (!StringUtils.hasText(val) || val.contains("wbscouting")) {
            return DEFAULT_INSTAGRAM;
        }
        return val;
    }
}

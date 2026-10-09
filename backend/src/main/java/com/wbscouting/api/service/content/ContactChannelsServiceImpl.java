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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContactChannelsServiceImpl implements ContactChannelsService {

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

        String email = null;
        String whatsappNumber = null;
        String defaultMessage = null;
        String instagramHandle = null;
        String address = null;
        String officeHours = null;

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

        List<ContactChannelsPublicDto.SocialMediaItemDto> socialMediaList = new ArrayList<>();
        if (contentOpt.isPresent()) {
            SiteContent content = contentOpt.get();
            Map<String, Object> pt = content.getPayloadPt() != null ? content.getPayloadPt() : Map.of();
            Map<String, Object> en = content.getPayloadEn() != null ? content.getPayloadEn() : Map.of();
            Map<String, Object> targetMap = isEn ? en : pt;

            Object smListObj = targetMap.get("socialMediaList");
            if (smListObj instanceof List<?> rawList) {
                for (Object itemObj : rawList) {
                    if (itemObj instanceof Map<?, ?> itemMap) {
                        String id = itemMap.get("id") != null ? itemMap.get("id").toString() : null;
                        String name = itemMap.get("name") != null ? itemMap.get("name").toString() : "";
                        String url = itemMap.get("url") != null ? itemMap.get("url").toString() : "";
                        if (!name.isBlank() || !url.isBlank()) {
                            socialMediaList.add(ContactChannelsPublicDto.SocialMediaItemDto.builder()
                                    .id(id)
                                    .name(name)
                                    .url(url)
                                    .build());
                        }
                    }
                }
            }

            if (isEn) {
                email = sanitizeVal(getString(en, "email", getString(pt, "email", email)), email);
                whatsappNumber = sanitizeVal(getString(en, "whatsappNumber", getString(pt, "whatsappNumber", whatsappNumber)), whatsappNumber);
                defaultMessage = getString(en, "whatsappDefaultMessage", getString(pt, "whatsappDefaultMessage", defaultMessage));
                instagramHandle = sanitizeInstagram(getString(en, "instagramHandle", getString(pt, "instagramHandle", instagramHandle)));
                address = getString(en, "address", getString(pt, "address", address));
                officeHours = getString(en, "officeHours", getString(pt, "officeHours", officeHours));
            } else {
                email = sanitizeVal(getString(pt, "email", email), email);
                whatsappNumber = sanitizeVal(getString(pt, "whatsappNumber", whatsappNumber), whatsappNumber);
                defaultMessage = getString(pt, "whatsappDefaultMessage", defaultMessage);
                instagramHandle = sanitizeInstagram(getString(pt, "instagramHandle", instagramHandle));
                address = getString(pt, "address", address);
                officeHours = getString(pt, "officeHours", officeHours);
            }
        }

        String sanitizedNumber = whatsappNumber != null ? whatsappNumber.replaceAll("\\D+", "") : "";
        String whatsappUrl = !sanitizedNumber.isBlank() ? buildWhatsappUrl(sanitizedNumber, defaultMessage) : null;

        return ContactChannelsPublicDto.builder()
                .email(email)
                .whatsappNumber(sanitizedNumber)
                .whatsappUrl(whatsappUrl)
                .instagramHandle(instagramHandle)
                .address(address)
                .officeHours(officeHours)
                .socialMediaList(socialMediaList)
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
        payloadPt.put("instagramHandle", dto.getInstagramHandle() != null ? dto.getInstagramHandle().trim() : "");
        payloadPt.put("address", dto.getAddressPt().trim());
        payloadPt.put("officeHours", dto.getOfficeHoursPt().trim());

        Map<String, Object> payloadEn = new LinkedHashMap<>();
        payloadEn.put("email", dto.getEmail().trim());
        payloadEn.put("whatsappNumber", cleanNumber);
        payloadEn.put("whatsappDefaultMessage", StringUtils.hasText(dto.getWhatsappDefaultMessageEn())
                ? dto.getWhatsappDefaultMessageEn().trim()
                : dto.getWhatsappDefaultMessagePt().trim());
        payloadEn.put("instagramHandle", dto.getInstagramHandle() != null ? dto.getInstagramHandle().trim() : "");
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
            return null;
        }
        return val;
    }
}

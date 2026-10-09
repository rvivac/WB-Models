package com.wbscouting.api.controller.admin;

import com.wbscouting.api.constant.ContentSectionKey;
import com.wbscouting.api.dto.content.ContactSettingsDto;
import com.wbscouting.api.entity.Admin;
import com.wbscouting.api.entity.SiteContent;
import com.wbscouting.api.repository.SiteContentRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RestController
@RequestMapping({"/api/v1/admin/institutional/contact", "/admin/institutional/contact"})
@PreAuthorize("hasAnyRole('WEBMASTER', 'SUPER_ADMIN', 'CONTENT_ADMIN', 'ADMIN')")
@RequiredArgsConstructor
public class AdminInstitutionalContactController {

    private static final String SECTION_CONTACT = "contact";

    private final SiteContentRepository siteContentRepository;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.wbscouting.api.repository.ContactChannelRepository contactChannelRepository;

    @GetMapping
    public ResponseEntity<ContactSettingsDto> getContactSettings() {
        log.info("Consultando canais institucionais de contato e redes sociais");

        Optional<SiteContent> contentOpt = siteContentRepository.findBySectionKey(SECTION_CONTACT);
        if (contentOpt.isEmpty()) {
            contentOpt = siteContentRepository.findBySectionKey(ContentSectionKey.CONTACT_INFO);
        }

        if (contentOpt.isEmpty()) {
            return ResponseEntity.ok(ContactSettingsDto.builder()
                    .socialMediaList(Collections.emptyList())
                    .build());
        }

        return ResponseEntity.ok(toDto(contentOpt.get()));
    }

    @PutMapping
    @com.wbscouting.api.security.audit.AuditAction(action = "UPDATE", resource = "INSTITUTIONAL_CONTACT", description = "Atualização de canais institucionais de contato e endereços")
    public ResponseEntity<ContactSettingsDto> updateContactSettings(
            @Valid @RequestBody ContactSettingsDto dto,
            Authentication authentication
    ) {
        log.info("Atualizando canais de contato e redes sociais da agência");

        SiteContent content = siteContentRepository.findBySectionKey(SECTION_CONTACT)
                .orElseGet(() -> SiteContent.builder().sectionKey(SECTION_CONTACT).build());

        UUID adminId = extractAdminId(authentication);
        if (adminId != null) {
            content.setUpdatedBy(adminId);
        }

        Map<String, Object> payload = toMap(dto);
        content.setPayloadPt(payload);
        content.setPayloadEn(payload);

        SiteContent saved = siteContentRepository.save(content);

        // Sincroniza também com CONTACT_INFO para manter compatibilidade com widgets públicos existentes
        syncLegacyContactInfo(dto, adminId);

        return ResponseEntity.ok(toDto(saved));
    }

    @PatchMapping
    public ResponseEntity<ContactSettingsDto> patchContactSettings(
            @RequestBody Map<String, Object> patchUpdates,
            Authentication authentication
    ) {
        log.info("Atualizando campos individuais dos canais de contato: {}", patchUpdates.keySet());

        SiteContent content = siteContentRepository.findBySectionKey(SECTION_CONTACT)
                .orElseGet(() -> SiteContent.builder().sectionKey(SECTION_CONTACT).build());

        UUID adminId = extractAdminId(authentication);
        if (adminId != null) {
            content.setUpdatedBy(adminId);
        }

        Map<String, Object> payload = content.getPayloadPt() != null ? new LinkedHashMap<>(content.getPayloadPt()) : new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry : patchUpdates.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (("address".equals(key) || "socialMedia".equals(key)) && value instanceof Map<?, ?> nestedPatch) {
                Map<String, Object> currentNested = new LinkedHashMap<>(castToMap(payload.get(key)));
                for (Map.Entry<?, ?> nestedEntry : nestedPatch.entrySet()) {
                    if (nestedEntry.getKey() != null) {
                        currentNested.put(nestedEntry.getKey().toString(), nestedEntry.getValue());
                    }
                }
                payload.put(key, currentNested);
            } else {
                payload.put(key, value);
            }
        }

        content.setPayloadPt(payload);
        content.setPayloadEn(payload);

        SiteContent saved = siteContentRepository.save(content);
        ContactSettingsDto updatedDto = toDto(saved);
        syncLegacyContactInfo(updatedDto, adminId);

        return ResponseEntity.ok(updatedDto);
    }

    private void syncLegacyContactInfo(ContactSettingsDto dto, UUID adminId) {
        try {
            SiteContent legacy = siteContentRepository.findBySectionKey(ContentSectionKey.CONTACT_INFO)
                    .orElseGet(() -> SiteContent.builder().sectionKey(ContentSectionKey.CONTACT_INFO).build());

            String cleanWhatsapp = dto.getWhatsapp() != null ? dto.getWhatsapp().replaceAll("\\D+", "") : "";

            Map<String, Object> payloadPt = new LinkedHashMap<>();
            payloadPt.put("email", dto.getPrimaryEmail());
            payloadPt.put("whatsappNumber", cleanWhatsapp);
            String instagram = (dto.getSocialMedia() != null && dto.getSocialMedia().getInstagram() != null && !dto.getSocialMedia().getInstagram().isBlank())
                    ? dto.getSocialMedia().getInstagram().trim()
                    : null;
            if (instagram != null) {
                payloadPt.put("instagramHandle", instagram);
            }
            if (dto.getAddress() != null && dto.getAddress().getCity() != null) {
                payloadPt.put("address", dto.getAddress().getCity() + (dto.getAddress().getState() != null ? " - " + dto.getAddress().getState() : ""));
            }
            if (dto.getBusinessHours() != null) {
                payloadPt.put("officeHours", dto.getBusinessHours());
            }

            if (dto.getSocialMediaList() != null) {
                List<Map<String, String>> smList = new ArrayList<>();
                for (ContactSettingsDto.SocialMediaItemDto item : dto.getSocialMediaList()) {
                    if (item != null && ((item.getName() != null && !item.getName().isBlank()) || (item.getUrl() != null && !item.getUrl().isBlank()))) {
                        Map<String, String> itemMap = new LinkedHashMap<>();
                        itemMap.put("id", item.getId() != null ? item.getId() : UUID.randomUUID().toString());
                        itemMap.put("name", item.getName() != null ? item.getName() : "");
                        itemMap.put("url", item.getUrl() != null ? item.getUrl() : "");
                        smList.add(itemMap);
                    }
                }
                payloadPt.put("socialMediaList", smList);
            }

            legacy.setPayloadPt(payloadPt);
            legacy.setPayloadEn(payloadPt);
            if (adminId != null) {
                legacy.setUpdatedBy(adminId);
            }
            siteContentRepository.save(legacy);
        } catch (Exception e) {
            log.warn("Falha ao sincronizar com CONTACT_INFO legado: {}", e.getMessage());
        }

        if (contactChannelRepository != null) {
            syncDedicatedChannels(dto);
        }
    }

    private void syncDedicatedChannels(ContactSettingsDto dto) {
        try {
            if (dto.getPrimaryEmail() != null && !dto.getPrimaryEmail().isBlank()) {
                upsertDedicatedChannel("EMAIL", dto.getPrimaryEmail(), "E-mail Geral & Atendimento", 1);
            }
            if (dto.getWhatsapp() != null && !dto.getWhatsapp().isBlank()) {
                upsertDedicatedChannel("WHATSAPP", dto.getWhatsapp(), "WhatsApp Oficial", 2);
            }
            if (dto.getPhone() != null && !dto.getPhone().isBlank()) {
                upsertDedicatedChannel("PHONE", dto.getPhone(), "Telefone Comercial", 3);
            }
            if (dto.getSocialMedia() != null && dto.getSocialMedia().getInstagram() != null && !dto.getSocialMedia().getInstagram().isBlank()) {
                upsertDedicatedChannel("INSTAGRAM", dto.getSocialMedia().getInstagram(), "Instagram Oficial", 4);
            }
            if (dto.getAddress() != null && dto.getAddress().getStreet() != null) {
                String fullAddr = dto.getAddress().getStreet()
                        + (dto.getAddress().getComplement() != null ? ", " + dto.getAddress().getComplement() : "")
                        + " - " + (dto.getAddress().getCity() != null ? dto.getAddress().getCity() : "")
                        + " - " + (dto.getAddress().getState() != null ? dto.getAddress().getState() : "");
                upsertDedicatedChannel("ADDRESS", fullAddr, "Endereço Matriz", 5);
            }
            if (dto.getBusinessHours() != null && !dto.getBusinessHours().isBlank()) {
                upsertDedicatedChannel("OFFICE_HOURS", dto.getBusinessHours(), "Horário de Atendimento", 6);
            }
            if (dto.getSocialMediaList() != null) {
                int smOrder = 10;
                for (ContactSettingsDto.SocialMediaItemDto item : dto.getSocialMediaList()) {
                    if (item != null && org.springframework.util.StringUtils.hasText(item.getUrl())) {
                        upsertDedicatedChannel("SOCIAL", item.getUrl(), item.getName() != null ? item.getName() : "Rede Social", smOrder++);
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("Falha ao sincronizar tabela contact_channels dedicada: {}", ex.getMessage());
        }
    }

    private void upsertDedicatedChannel(String type, String value, String label, int order) {
        List<com.wbscouting.api.entity.ContactChannel> existing =
                contactChannelRepository.findByTypeIgnoreCaseOrderByDisplayOrderAsc(type);
        if (!existing.isEmpty()) {
            com.wbscouting.api.entity.ContactChannel ch = existing.get(0);
            ch.setValue(value.trim());
            ch.setLabel(label);
            ch.setActive(true);
            ch.setUpdatedAt(java.time.OffsetDateTime.now());
            contactChannelRepository.save(ch);
        } else {
            com.wbscouting.api.entity.ContactChannel ch = com.wbscouting.api.entity.ContactChannel.builder()
                    .type(type.toUpperCase())
                    .value(value.trim())
                    .label(label)
                    .active(true)
                    .displayOrder(order)
                    .createdAt(java.time.OffsetDateTime.now())
                    .updatedAt(java.time.OffsetDateTime.now())
                    .build();
            contactChannelRepository.save(ch);
        }
    }

    private Map<String, Object> toMap(ContactSettingsDto dto) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("primaryEmail", dto.getPrimaryEmail());
        map.put("scoutingEmail", dto.getScoutingEmail());
        map.put("pressEmail", dto.getPressEmail());
        map.put("phone", dto.getPhone());
        map.put("whatsapp", dto.getWhatsapp());
        map.put("whatsappDefaultMessage", dto.getWhatsappDefaultMessage());
        map.put("businessHours", dto.getBusinessHours());

        if (dto.getAddress() != null) {
            Map<String, Object> addr = new LinkedHashMap<>();
            addr.put("street", dto.getAddress().getStreet());
            addr.put("complement", dto.getAddress().getComplement());
            addr.put("neighborhood", dto.getAddress().getNeighborhood());
            addr.put("city", dto.getAddress().getCity());
            addr.put("state", dto.getAddress().getState());
            addr.put("zipCode", dto.getAddress().getZipCode());
            addr.put("country", dto.getAddress().getCountry());
            map.put("address", addr);
        }

        if (dto.getSocialMedia() != null) {
            Map<String, Object> sm = new LinkedHashMap<>();
            sm.put("instagram", dto.getSocialMedia().getInstagram());
            sm.put("linkedin", dto.getSocialMedia().getLinkedin());
            sm.put("facebook", dto.getSocialMedia().getFacebook());
            sm.put("tiktok", dto.getSocialMedia().getTiktok());
            map.put("socialMedia", sm);
        }

        if (dto.getSocialMediaList() != null) {
            List<Map<String, String>> smList = new ArrayList<>();
            for (ContactSettingsDto.SocialMediaItemDto item : dto.getSocialMediaList()) {
                if (item != null && ((item.getName() != null && !item.getName().isBlank()) || (item.getUrl() != null && !item.getUrl().isBlank()))) {
                    Map<String, String> itemMap = new LinkedHashMap<>();
                    itemMap.put("id", item.getId() != null ? item.getId() : UUID.randomUUID().toString());
                    itemMap.put("name", item.getName() != null ? item.getName() : "");
                    itemMap.put("url", item.getUrl() != null ? item.getUrl() : "");
                    smList.add(itemMap);
                }
            }
            map.put("socialMediaList", smList);
        }

        return map;
    }

    private static Map<String, Object> castToMap(Object obj) {
        if (obj instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() != null) {
                    result.put(entry.getKey().toString(), entry.getValue());
                }
            }
            return result;
        }
        return Collections.emptyMap();
    }

    private ContactSettingsDto toDto(SiteContent content) {
        Map<String, Object> pt = content.getPayloadPt() != null ? content.getPayloadPt() : Collections.emptyMap();

        Map<String, Object> addrMap = castToMap(pt.get("address"));
        Map<String, Object> smMap = castToMap(pt.get("socialMedia"));

        ContactSettingsDto.AddressDto address = addrMap.isEmpty() ? null : ContactSettingsDto.AddressDto.builder()
                .street(getString(addrMap, "street", null))
                .complement(getString(addrMap, "complement", null))
                .neighborhood(getString(addrMap, "neighborhood", null))
                .city(getString(addrMap, "city", null))
                .state(getString(addrMap, "state", null))
                .zipCode(getString(addrMap, "zipCode", null))
                .country(getString(addrMap, "country", null))
                .build();

        ContactSettingsDto.SocialMediaDto socialMedia = smMap.isEmpty() ? null : ContactSettingsDto.SocialMediaDto.builder()
                .instagram(getString(smMap, "instagram", null))
                .linkedin(getString(smMap, "linkedin", null))
                .facebook(getString(smMap, "facebook", null))
                .tiktok(getString(smMap, "tiktok", null))
                .build();

        List<ContactSettingsDto.SocialMediaItemDto> socialMediaList = new ArrayList<>();
        Object smListObj = pt.get("socialMediaList");
        if (smListObj instanceof List<?> rawList) {
            for (Object itemObj : rawList) {
                if (itemObj instanceof Map<?, ?> itemMap) {
                    Map<String, Object> safeMap = castToMap(itemMap);
                    String id = getString(safeMap, "id", UUID.randomUUID().toString());
                    String name = getString(safeMap, "name", "");
                    String url = getString(safeMap, "url", "");
                    if (!name.isBlank() || !url.isBlank()) {
                        socialMediaList.add(ContactSettingsDto.SocialMediaItemDto.builder()
                                .id(id)
                                .name(name)
                                .url(url)
                                .build());
                    }
                }
            }
        }

        return ContactSettingsDto.builder()
                .primaryEmail(getString(pt, "primaryEmail", getString(pt, "email", null)))
                .scoutingEmail(getString(pt, "scoutingEmail", null))
                .pressEmail(getString(pt, "pressEmail", null))
                .phone(getString(pt, "phone", null))
                .whatsapp(getString(pt, "whatsapp", getString(pt, "whatsappNumber", null)))
                .whatsappDefaultMessage(getString(pt, "whatsappDefaultMessage", null))
                .businessHours(getString(pt, "businessHours", getString(pt, "officeHours", null)))
                .address(address)
                .socialMedia(socialMedia)
                .socialMediaList(socialMediaList)
                .build();
    }

    private String getString(Map<String, Object> map, String key, String defaultValue) {
        Object val = map.get(key);
        return val != null ? val.toString() : defaultValue;
    }

    private UUID extractAdminId(Authentication authentication) {
        Authentication auth = authentication != null ? authentication : SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Admin admin) {
            return admin.getId();
        }
        return null;
    }
}

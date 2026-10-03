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

    @GetMapping
    public ResponseEntity<ContactSettingsDto> getContactSettings() {
        log.info("Consultando canais institucionais de contato e redes sociais");

        SiteContent content = siteContentRepository.findBySectionKey(SECTION_CONTACT)
                .orElseGet(this::createDefaultContactContent);

        return ResponseEntity.ok(toDto(content));
    }

    @PutMapping
    @com.wbscouting.api.security.audit.AuditAction(action = "UPDATE", resource = "INSTITUTIONAL_CONTACT", description = "Atualização de canais institucionais de contato e endereços")
    public ResponseEntity<ContactSettingsDto> updateContactSettings(
            @Valid @RequestBody ContactSettingsDto dto,
            Authentication authentication
    ) {
        log.info("Atualizando canais de contato e redes sociais da agência");

        SiteContent content = siteContentRepository.findBySectionKey(SECTION_CONTACT)
                .orElseGet(this::createDefaultContactContent);

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
                .orElseGet(this::createDefaultContactContent);

        UUID adminId = extractAdminId(authentication);
        if (adminId != null) {
            content.setUpdatedBy(adminId);
        }

        Map<String, Object> payload = content.getPayloadPt() != null ? new LinkedHashMap<>(content.getPayloadPt()) : new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry : patchUpdates.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (("address".equals(key) || "socialMedia".equals(key)) && value instanceof Map<?, ?> nestedPatch) {
                Map<String, Object> currentNested = payload.get(key) instanceof Map ? new LinkedHashMap<>((Map<String, Object>) payload.get(key)) : new LinkedHashMap<>();
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
                    : "@wbagency";
            payloadPt.put("instagramHandle", instagram);
            payloadPt.put("address", dto.getAddress() != null ? dto.getAddress().getCity() + " - " + dto.getAddress().getState() : "São Paulo - SP");
            payloadPt.put("officeHours", dto.getBusinessHours());

            legacy.setPayloadPt(payloadPt);
            legacy.setPayloadEn(payloadPt);
            if (adminId != null) {
                legacy.setUpdatedBy(adminId);
            }
            siteContentRepository.save(legacy);
        } catch (Exception e) {
            log.warn("Falha ao sincronizar com CONTACT_INFO legado: {}", e.getMessage());
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

        return map;
    }

    @SuppressWarnings("unchecked")
    private ContactSettingsDto toDto(SiteContent content) {
        Map<String, Object> pt = content.getPayloadPt() != null ? content.getPayloadPt() : Collections.emptyMap();

        Map<String, Object> addrMap = pt.get("address") instanceof Map ? (Map<String, Object>) pt.get("address") : Collections.emptyMap();
        Map<String, Object> smMap = pt.get("socialMedia") instanceof Map ? (Map<String, Object>) pt.get("socialMedia") : Collections.emptyMap();

        ContactSettingsDto.AddressDto address = ContactSettingsDto.AddressDto.builder()
                .street(getString(addrMap, "street", "Avenida Paulista, 1000"))
                .complement(getString(addrMap, "complement", "Conjunto 1402"))
                .neighborhood(getString(addrMap, "neighborhood", "Bela Vista"))
                .city(getString(addrMap, "city", "São Paulo"))
                .state(getString(addrMap, "state", "SP"))
                .zipCode(getString(addrMap, "zipCode", "01310-100"))
                .country(getString(addrMap, "country", "Brasil"))
                .build();

        ContactSettingsDto.SocialMediaDto socialMedia = ContactSettingsDto.SocialMediaDto.builder()
                .instagram(getString(smMap, "instagram", "https://instagram.com/wbagency"))
                .linkedin(getString(smMap, "linkedin", "https://linkedin.com/company/wbagency"))
                .facebook(getString(smMap, "facebook", ""))
                .tiktok(getString(smMap, "tiktok", "https://tiktok.com/@wbagency"))
                .build();

        return ContactSettingsDto.builder()
                .primaryEmail(getString(pt, "primaryEmail", "info@wbagency.com.br"))
                .scoutingEmail(getString(pt, "scoutingEmail", "scouting@wbagency.com.br"))
                .pressEmail(getString(pt, "pressEmail", "press@wbagency.com.br"))
                .phone(getString(pt, "phone", "+55 11 97065-6003"))
                .whatsapp(getString(pt, "whatsapp", "+55 11 97065-6003"))
                .whatsappDefaultMessage(getString(pt, "whatsappDefaultMessage", "Olá! Gostaria de falar com a equipe de atendimento da WB Agency."))
                .businessHours(getString(pt, "businessHours", "Segunda a Sexta: 09h às 18h (GMT-3)"))
                .address(address)
                .socialMedia(socialMedia)
                .build();
    }

    private SiteContent createDefaultContactContent() {
        ContactSettingsDto defaultDto = ContactSettingsDto.builder()
                .primaryEmail("info@wbagency.com.br")
                .scoutingEmail("scouting@wbagency.com.br")
                .pressEmail("press@wbagency.com.br")
                .phone("+55 11 97065-6003")
                .whatsapp("+55 11 97065-6003")
                .whatsappDefaultMessage("Olá! Gostaria de falar com a equipe de atendimento da WB Agency.")
                .businessHours("Segunda a Sexta: 09h às 18h (GMT-3)")
                .address(ContactSettingsDto.AddressDto.builder()
                        .street("Avenida Paulista, 1000")
                        .complement("Conjunto 1402")
                        .neighborhood("Bela Vista")
                        .city("São Paulo")
                        .state("SP")
                        .zipCode("01310-100")
                        .country("Brasil")
                        .build())
                .socialMedia(ContactSettingsDto.SocialMediaDto.builder()
                        .instagram("https://instagram.com/wbagency")
                        .linkedin("https://linkedin.com/company/wbagency")
                        .facebook("")
                        .tiktok("https://tiktok.com/@wbagency")
                        .build())
                .build();

        Map<String, Object> payload = toMap(defaultDto);
        SiteContent content = SiteContent.builder()
                .sectionKey(SECTION_CONTACT)
                .payloadPt(payload)
                .payloadEn(payload)
                .build();

        return siteContentRepository.save(content);
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

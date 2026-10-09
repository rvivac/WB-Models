package com.wbscouting.api.controller.publicapi;

import com.wbscouting.api.constant.ContentSectionKey;
import com.wbscouting.api.dto.content.ContactSettingsDto;
import com.wbscouting.api.dto.publicapi.ContactChannelsPublicDto;
import com.wbscouting.api.entity.SiteContent;
import com.wbscouting.api.repository.SiteContentRepository;
import com.wbscouting.api.service.content.ContactChannelsService;
import com.wbscouting.api.service.content.I18nDictionaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.TimeUnit;

@RestController
@RequiredArgsConstructor
public class PublicContactController {

    private final ContactChannelsService contactChannelsService;
    private final I18nDictionaryService i18nDictionaryService;
    private final SiteContentRepository siteContentRepository;

    @GetMapping({"/api/v1/public/contact-channels", "/public/contact-channels"})
    public ResponseEntity<ContactChannelsPublicDto> getContactChannels(
            @RequestParam(value = "lang", defaultValue = "pt") String lang) {

        ContactChannelsPublicDto dto = contactChannelsService.getContactChannels(lang);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(600, TimeUnit.SECONDS).cachePublic())
                .body(dto);
    }

    @GetMapping({"/api/v1/public/institutional/contact", "/public/institutional/contact"})
    public ResponseEntity<ContactSettingsDto> getInstitutionalContact() {
        Optional<SiteContent> contentOpt = siteContentRepository.findBySectionKey("contact");
        if (contentOpt.isEmpty()) {
            contentOpt = siteContentRepository.findBySectionKey(ContentSectionKey.CONTACT_INFO);
        }

        if (contentOpt.isEmpty()) {
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(300, TimeUnit.SECONDS).cachePublic())
                    .body(ContactSettingsDto.builder()
                            .socialMediaList(Collections.emptyList())
                            .build());
        }

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(300, TimeUnit.SECONDS).cachePublic())
                .body(toContactSettingsDto(contentOpt.get()));
    }

    @GetMapping({"/api/v1/public/i18n/{lang}", "/public/i18n/{lang}"})
    public ResponseEntity<Map<String, Object>> getDictionary(
            @PathVariable("lang") String lang) {

        Map<String, Object> dictionary = i18nDictionaryService.getDictionary(lang);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(300, TimeUnit.SECONDS).cachePublic())
                .body(dictionary);
    }

    private ContactSettingsDto toContactSettingsDto(SiteContent content) {
        Map<String, Object> pt = content.getPayloadPt() != null ? content.getPayloadPt() : Collections.emptyMap();

        String primaryEmail = getString(pt, "primaryEmail", getString(pt, "email", null));
        String phone = getString(pt, "phone", null);
        String whatsapp = getString(pt, "whatsapp", getString(pt, "whatsappNumber", null));
        String businessHours = getString(pt, "businessHours", getString(pt, "officeHours", null));

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
                .primaryEmail(primaryEmail)
                .phone(phone)
                .whatsapp(whatsapp)
                .businessHours(businessHours)
                .socialMediaList(socialMediaList)
                .build();
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

    private static String getString(Map<String, Object> map, String key, String defaultValue) {
        if (map == null || !map.containsKey(key)) return defaultValue;
        Object val = map.get(key);
        return (val != null && !val.toString().trim().isEmpty()) ? val.toString().trim() : defaultValue;
    }
}

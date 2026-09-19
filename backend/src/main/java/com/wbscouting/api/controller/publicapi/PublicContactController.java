package com.wbscouting.api.controller.publicapi;

import com.wbscouting.api.dto.publicapi.ContactChannelsPublicDto;
import com.wbscouting.api.service.content.ContactChannelsService;
import com.wbscouting.api.service.content.I18nDictionaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequiredArgsConstructor
public class PublicContactController {

    private final ContactChannelsService contactChannelsService;
    private final I18nDictionaryService i18nDictionaryService;

    @GetMapping({"/api/v1/public/contact-channels", "/public/contact-channels"})
    public ResponseEntity<ContactChannelsPublicDto> getContactChannels(
            @RequestParam(value = "lang", defaultValue = "pt") String lang) {

        ContactChannelsPublicDto dto = contactChannelsService.getContactChannels(lang);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(600, TimeUnit.SECONDS).cachePublic())
                .body(dto);
    }

    @GetMapping({"/api/v1/public/i18n/{lang}", "/public/i18n/{lang}"})
    public ResponseEntity<Map<String, Object>> getDictionary(
            @PathVariable("lang") String lang) {

        Map<String, Object> dictionary = i18nDictionaryService.getDictionary(lang);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(300, TimeUnit.SECONDS).cachePublic())
                .body(dictionary);
    }
}

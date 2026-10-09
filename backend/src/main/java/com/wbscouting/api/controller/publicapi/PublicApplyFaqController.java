package com.wbscouting.api.controller.publicapi;

import com.wbscouting.api.dto.ApplyFaqDto;
import com.wbscouting.api.dto.ApplyHeaderDto;
import com.wbscouting.api.service.content.ApplyFaqService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequiredArgsConstructor
public class PublicApplyFaqController {

    private final ApplyFaqService applyFaqService;

    @GetMapping({"/api/v1/public/apply-faq", "/public/apply-faq"})
    public ResponseEntity<List<ApplyFaqDto>> getPublicFaqs(@org.springframework.web.bind.annotation.RequestParam(value = "lang", required = false) String lang) {
        List<ApplyFaqDto> faqs = (lang != null && !lang.isBlank())
                ? applyFaqService.getPublicActiveFaqs(lang)
                : applyFaqService.getPublicActiveFaqs();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(300, TimeUnit.SECONDS).cachePublic())
                .body(faqs);
    }

    @GetMapping({"/api/v1/public/institutional/apply-header", "/public/institutional/apply-header"})
    public ResponseEntity<ApplyHeaderDto> getPublicApplyHeader() {
        ApplyHeaderDto header = applyFaqService.getPublicApplyHeader();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(300, TimeUnit.SECONDS).cachePublic())
                .body(header);
    }
}

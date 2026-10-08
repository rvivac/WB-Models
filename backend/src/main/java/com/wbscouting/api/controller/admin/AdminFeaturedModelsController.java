package com.wbscouting.api.controller.admin;

import com.wbscouting.api.dto.model.FeaturedModelOrderItemDto;
import com.wbscouting.api.dto.model.FeaturedModelResponseDto;
import com.wbscouting.api.dto.model.FeaturedModelsReorderRequestDto;
import com.wbscouting.api.security.audit.AuditAction;
import com.wbscouting.api.service.model.FeaturedModelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RestController
@RequestMapping({"/api/v1/admin/featured-models", "/admin/featured-models"})
@PreAuthorize("hasAnyRole('WEBMASTER', 'SUPER_ADMIN', 'CONTENT_ADMIN', 'ADMIN')")
@RequiredArgsConstructor
public class AdminFeaturedModelsController {

    private final FeaturedModelService featuredModelService;

    @GetMapping
    public ResponseEntity<List<FeaturedModelResponseDto>> getFeaturedHomeModels() {
        return ResponseEntity.ok(featuredModelService.getFeaturedHomeModels());
    }

    @PutMapping
    @AuditAction(action = "UPDATE", resource = "FEATURED_MODELS", description = "Atualização da vitrine da Home")
    public ResponseEntity<List<FeaturedModelResponseDto>> updateFeaturedHomeModels(
            @RequestBody Object rawPayload) {

        FeaturedModelsReorderRequestDto requestDto = parsePayload(rawPayload);
        return ResponseEntity.ok(featuredModelService.updateFeaturedHomeModels(requestDto));
    }

    @SuppressWarnings("unchecked")
    private FeaturedModelsReorderRequestDto parsePayload(Object raw) {
        if (raw == null) {
            return new FeaturedModelsReorderRequestDto();
        }

        List<FeaturedModelOrderItemDto> items = new ArrayList<>();

        if (raw instanceof List<?> rawList) {
            int order = 1;
            for (Object obj : rawList) {
                if (obj instanceof String idStr) {
                    try {
                        items.add(new FeaturedModelOrderItemDto(UUID.fromString(idStr), order++));
                    } catch (Exception ignored) {}
                } else if (obj instanceof Map<?, ?> map) {
                    Object mId = map.get("modelId");
                    if (mId == null) mId = map.get("id");
                    Object disp = map.get("displayOrder");
                    if (mId != null) {
                        try {
                            int dispOrder = disp instanceof Number n ? n.intValue() : order;
                            items.add(new FeaturedModelOrderItemDto(UUID.fromString(mId.toString()), dispOrder));
                        } catch (Exception ignored) {}
                    }
                    order++;
                }
            }
        } else if (raw instanceof Map<?, ?> map) {
            Object rawItems = map.get("items");
            Object rawModelIds = map.get("modelIds");

            if (rawItems instanceof List<?> list) {
                int order = 1;
                for (Object obj : list) {
                    if (obj instanceof Map<?, ?> itemMap) {
                        Object mId = itemMap.get("modelId");
                        if (mId == null) mId = itemMap.get("id");
                        Object disp = itemMap.get("displayOrder");
                        if (mId != null) {
                            try {
                                int dispOrder = disp instanceof Number n ? n.intValue() : order;
                                items.add(new FeaturedModelOrderItemDto(UUID.fromString(mId.toString()), dispOrder));
                            } catch (Exception ignored) {}
                        }
                        order++;
                    }
                }
            } else if (rawModelIds instanceof List<?> list) {
                int order = 1;
                for (Object obj : list) {
                    if (obj != null) {
                        try {
                            items.add(new FeaturedModelOrderItemDto(UUID.fromString(obj.toString()), order++));
                        } catch (Exception ignored) {}
                    }
                }
            }
        }

        FeaturedModelsReorderRequestDto dto = new FeaturedModelsReorderRequestDto();
        dto.setItems(items);
        return dto;
    }
}

package com.wbscouting.api.controller.publicapi;

import com.wbscouting.api.dto.publicapi.ModelCardPublicDto;
import com.wbscouting.api.dto.publicapi.ModelDetailPublicDto;
import com.wbscouting.api.dto.publicapi.PageResponseDto;
import com.wbscouting.api.enums.GenderType;
import com.wbscouting.api.service.publicapi.PublicModelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/public/models")
@RequiredArgsConstructor
public class PublicModelController {

    private final PublicModelService publicModelService;

    @GetMapping("/featured")
    public ResponseEntity<List<ModelCardPublicDto>> getFeaturedModels() {
        List<ModelCardPublicDto> featured = publicModelService.getFeaturedModels();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(60, TimeUnit.SECONDS).cachePublic())
                .body(featured);
    }


}

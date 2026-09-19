package com.wbscouting.api.controller.admin;

import com.wbscouting.api.dto.admin.contact.ContactChannelsUpdateRequestDto;
import com.wbscouting.api.dto.publicapi.ContactChannelsPublicDto;
import com.wbscouting.api.entity.Admin;
import com.wbscouting.api.service.content.ContactChannelsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/admin/contact-channels", "/admin/contact-channels"})
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CONTENT_ADMIN')")
@RequiredArgsConstructor
public class AdminContactChannelsController {

    private final ContactChannelsService contactChannelsService;

    @PutMapping
    public ResponseEntity<ContactChannelsPublicDto> updateContactChannels(
            @Valid @RequestBody ContactChannelsUpdateRequestDto request,
            Authentication authentication) {

        UUID adminId = null;
        Authentication auth = authentication != null ? authentication : SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Admin admin) {
            adminId = admin.getId();
        }

        ContactChannelsPublicDto updated = contactChannelsService.updateContactChannels(request, adminId);
        return ResponseEntity.ok(updated);
    }
}

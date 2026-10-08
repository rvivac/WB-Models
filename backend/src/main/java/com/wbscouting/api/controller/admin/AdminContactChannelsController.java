package com.wbscouting.api.controller.admin;

import com.wbscouting.api.dto.admin.contact.ContactChannelsUpdateRequestDto;
import com.wbscouting.api.dto.content.ContactChannelDto;
import com.wbscouting.api.dto.publicapi.ContactChannelsPublicDto;
import com.wbscouting.api.entity.Admin;
import com.wbscouting.api.entity.ContactChannel;
import com.wbscouting.api.exception.ResourceNotFoundException;
import com.wbscouting.api.repository.ContactChannelRepository;
import com.wbscouting.api.security.audit.AuditAction;
import com.wbscouting.api.service.content.ContactChannelsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping({"/api/v1/admin/contact-channels", "/admin/contact-channels"})
@PreAuthorize("hasAnyRole('WEBMASTER', 'SUPER_ADMIN', 'CONTENT_ADMIN', 'ADMIN')")
@RequiredArgsConstructor
public class AdminContactChannelsController {

    private final ContactChannelRepository contactChannelRepository;
    private final ContactChannelsService contactChannelsService;

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<ContactChannelDto>> listAllChannels() {
        List<ContactChannel> list = contactChannelRepository.findAllByOrderByDisplayOrderAsc();
        return ResponseEntity.ok(list.stream().map(ContactChannelDto::fromEntity).toList());
    }

    @PostMapping
    @Transactional
    @AuditAction(action = "CREATE", resource = "CONTACT_CHANNEL", description = "Criação de canal de contato")
    public ResponseEntity<ContactChannelDto> createChannel(@Valid @RequestBody ContactChannelDto dto) {
        log.info("Criando novo canal de contato: type={}, label={}", dto.getType(), dto.getLabel());

        ContactChannel entity = ContactChannel.builder()
                .type(dto.getType())
                .value(dto.getValue())
                .label(dto.getLabel())
                .active(dto.getActive() != null ? dto.getActive() : true)
                .displayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : 0)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        ContactChannel saved = contactChannelRepository.saveAndFlush(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(ContactChannelDto.fromEntity(saved));
    }

    @PutMapping("/{id}")
    @Transactional
    @AuditAction(action = "UPDATE", resource = "CONTACT_CHANNEL", description = "Atualização de canal de contato")
    public ResponseEntity<ContactChannelDto> updateChannel(
            @PathVariable UUID id,
            @Valid @RequestBody ContactChannelDto dto
    ) {
        log.info("Atualizando canal de contato ID: {}", id);

        ContactChannel channel = contactChannelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Canal de contato", "id", id));

        channel.setType(dto.getType());
        channel.setValue(dto.getValue());
        channel.setLabel(dto.getLabel());
        if (dto.getActive() != null) {
            channel.setActive(dto.getActive());
        }
        if (dto.getDisplayOrder() != null) {
            channel.setDisplayOrder(dto.getDisplayOrder());
        }
        channel.setUpdatedAt(OffsetDateTime.now());

        ContactChannel saved = contactChannelRepository.saveAndFlush(channel);
        return ResponseEntity.ok(ContactChannelDto.fromEntity(saved));
    }

    @DeleteMapping("/{id}")
    @Transactional
    @AuditAction(action = "DELETE", resource = "CONTACT_CHANNEL", description = "Exclusão de canal de contato")
    public ResponseEntity<Void> deleteChannel(@PathVariable UUID id) {
        log.info("Excluindo canal de contato ID: {}", id);

        if (!contactChannelRepository.existsById(id)) {
            throw new ResourceNotFoundException("Canal de contato", "id", id);
        }

        contactChannelRepository.deleteById(id);
        contactChannelRepository.flush();
        return ResponseEntity.noContent().build();
    }

    /**
     * Endpoint legado para compatibilidade com formulários de bloco único.
     */
    @PutMapping
    @AuditAction(action = "UPDATE", resource = "CONTACT_CHANNELS_LEGACY", description = "Atualização consolidada de canais de contato")
    public ResponseEntity<ContactChannelsPublicDto> updateContactChannels(
            @Valid @RequestBody ContactChannelsUpdateRequestDto request,
            Authentication authentication
    ) {
        UUID adminId = null;
        Authentication auth = authentication != null ? authentication : SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Admin admin) {
            adminId = admin.getId();
        }

        ContactChannelsPublicDto updated = contactChannelsService.updateContactChannels(request, adminId);
        return ResponseEntity.ok(updated);
    }
}

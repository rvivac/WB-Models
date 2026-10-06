package com.wbscouting.api.service.audit;

import com.wbscouting.api.dto.admin.AdminAuditLogResponseDto;
import com.wbscouting.api.entity.Admin;
import com.wbscouting.api.entity.AdminAuditLog;
import com.wbscouting.api.repository.AdminAuditLogRepository;
import com.wbscouting.api.repository.AdminRepository;
import com.wbscouting.api.specification.AdminAuditLogSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AdminAuditLogRepository auditLogRepository;
    private final AdminRepository adminRepository;

    /**
     * Persistência assíncrona não-bloqueante na tabela imutável admin_audit_logs.
     */
    @Async("auditExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordLogAsync(
            UUID adminId,
            String adminEmail,
            String action,
            String resourceType,
            String resourceId,
            String description,
            Map<String, Object> detailsJson,
            String ipAddress,
            String userAgent) {
        try {
            UUID resolvedAdminId = adminId;
            String resolvedEmail = (adminEmail != null && !adminEmail.isBlank()) ? adminEmail : "sistema@wbscouting.com";

            if (resolvedAdminId == null && adminEmail != null && !adminEmail.isBlank()) {
                Optional<Admin> adminOpt = adminRepository.findByEmail(adminEmail);
                if (adminOpt.isPresent()) {
                    resolvedAdminId = adminOpt.get().getId();
                }
            }

            AdminAuditLog logEntity = AdminAuditLog.builder()
                    .adminId(resolvedAdminId)
                    .adminEmail(resolvedEmail)
                    .action(action != null ? action.toUpperCase() : "UNKNOWN")
                    .resourceType(resourceType != null ? resourceType.toUpperCase() : "GENERAL")
                    .resourceId(resourceId)
                    .description(description)
                    .detailsJson(detailsJson)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .build();

            auditLogRepository.save(logEntity);
            log.debug("AuditLog persistido: [{} - {}] por {}", action, resourceType, resolvedEmail);
        } catch (Exception ex) {
            log.error("Falha ao registrar log de auditoria assíncrono: {}", ex.getMessage(), ex);
        }
    }

    /**
     * Consulta paginada e filtrada exclusiva para Webmasters.
     */
    @Transactional(readOnly = true)
    public Page<AdminAuditLogResponseDto> getAuditLogs(
            String adminEmail,
            String resourceType,
            String action,
            OffsetDateTime startDate,
            OffsetDateTime endDate,
            String search,
            Pageable pageable) {
        return auditLogRepository.findAll(
                AdminAuditLogSpecification.filter(adminEmail, resourceType, action, startDate, endDate, search),
                pageable
        ).map(this::toDto);
    }

    /**
     * Busca um registro de auditoria por ID.
     */
    @Transactional(readOnly = true)
    public AdminAuditLogResponseDto getAuditLogById(UUID id) {
        return auditLogRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new IllegalArgumentException("Registro de auditoria não encontrado: " + id));
    }

    private AdminAuditLogResponseDto toDto(AdminAuditLog entity) {
        return AdminAuditLogResponseDto.builder()
                .id(entity.getId())
                .adminId(entity.getAdminId())
                .adminEmail(entity.getAdminEmail())
                .action(entity.getAction())
                .resourceType(entity.getResourceType())
                .resourceId(entity.getResourceId())
                .description(entity.getDescription())
                .detailsJson(entity.getDetailsJson())
                .ipAddress(entity.getIpAddress())
                .userAgent(entity.getUserAgent())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}

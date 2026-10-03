package com.wbscouting.api.controller.admin;

import com.wbscouting.api.dto.admin.AdminAuditLogResponseDto;
import com.wbscouting.api.service.audit.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/admin/audit-logs", "/admin/audit-logs"})
@RequiredArgsConstructor
@PreAuthorize("hasRole('WEBMASTER')")
public class AdminAuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<Page<AdminAuditLogResponseDto>> listAuditLogs(
            @RequestParam(required = false) String adminEmail,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<AdminAuditLogResponseDto> logs = auditLogService.getAuditLogs(
                adminEmail, resourceType, action, startDate, endDate, search, pageable
        );
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminAuditLogResponseDto> getAuditLogById(@PathVariable UUID id) {
        AdminAuditLogResponseDto logDto = auditLogService.getAuditLogById(id);
        return ResponseEntity.ok(logDto);
    }
}

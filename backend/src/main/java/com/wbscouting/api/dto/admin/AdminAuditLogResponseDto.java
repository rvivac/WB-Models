package com.wbscouting.api.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminAuditLogResponseDto {
    private UUID id;
    private UUID adminId;
    private String adminEmail;
    private String action;
    private String resourceType;
    private String resourceId;
    private String description;
    private Map<String, Object> detailsJson;
    private String ipAddress;
    private String userAgent;
    private OffsetDateTime createdAt;
}

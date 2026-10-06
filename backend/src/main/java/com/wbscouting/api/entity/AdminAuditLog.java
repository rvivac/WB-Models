package com.wbscouting.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "admin_audit_logs", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "admin_id")
    private UUID adminId;

    @Column(name = "admin_email", nullable = false, length = 255)
    private String adminEmail;

    @Column(name = "action", nullable = false, length = 50)
    private String action;

    @Column(name = "resource_type", nullable = false, length = 100)
    private String resourceType;

    @Column(name = "resource_id", length = 255)
    private String resourceId;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "details_json", columnDefinition = "jsonb")
    private Map<String, Object> detailsJson;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "text")
    private String userAgent;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    // ============================================================
    // 🔥 GETTERS EXPLICITOS (Lombok @Getter nao processa no MavenWrapper 3.6.3 Render)
    // ============================================================
    public UUID getId() { return this.id; }
    public UUID getAdminId() { return this.adminId; }
    public String getAdminEmail() { return this.adminEmail; }
    public String getAction() { return this.action; }
    public String getResourceType() { return this.resourceType; }
    public String getResourceId() { return this.resourceId; }
    public String getDescription() { return this.description; }
    public Map<String, Object> getDetailsJson() { return this.detailsJson; }
    public String getIpAddress() { return this.ipAddress; }
    public String getUserAgent() { return this.userAgent; }
    public OffsetDateTime getCreatedAt() { return this.createdAt; }

    // ============================================================
    // 🔥 SETTERS EXPLICITOS complementares
    // ============================================================
    public void setId(UUID id) { this.id = id; }
    public void setAdminId(UUID adminId) { this.adminId = adminId; }
    public void setAdminEmail(String adminEmail) { this.adminEmail = adminEmail; }
    public void setAction(String action) { this.action = action; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }
    public void setResourceId(String resourceId) { this.resourceId = resourceId; }
    public void setDescription(String description) { this.description = description; }
    public void setDetailsJson(Map<String, Object> detailsJson) { this.detailsJson = detailsJson; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}

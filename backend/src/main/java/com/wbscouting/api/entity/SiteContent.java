package com.wbscouting.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "site_contents", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SiteContent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "section_key", length = 100, nullable = false, unique = true)
    private String sectionKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload_pt", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> payloadPt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload_en", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> payloadEn;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "media_urls", columnDefinition = "jsonb")
    private Map<String, Object> mediaUrls;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "updated_by")
    private UUID updatedBy;

    // ============================================================
    // 🔥 GETTERS EXPLICITOS (Lombok @Getter/@Setter NAO processa no mvnw 3.6.3)
    // ============================================================
    public UUID getId() { return this.id; }
    public String getSectionKey() { return this.sectionKey; }
    public Map<String, Object> getPayloadPt() { return this.payloadPt; }
    public Map<String, Object> getPayloadEn() { return this.payloadEn; }
    public Map<String, Object> getMediaUrls() { return this.mediaUrls; }
    public OffsetDateTime getCreatedAt() { return this.createdAt; }
    public OffsetDateTime getUpdatedAt() { return this.updatedAt; }
    public UUID getUpdatedBy() { return this.updatedBy; }

    // ============================================================
    // 🔥 SETTERS EXPLICITOS complementares
    // ============================================================
    public void setId(UUID id) { this.id = id; }
    public void setSectionKey(String sectionKey) { this.sectionKey = sectionKey; }
    public void setPayloadPt(Map<String, Object> payloadPt) { this.payloadPt = payloadPt; }
    public void setPayloadEn(Map<String, Object> payloadEn) { this.payloadEn = payloadEn; }
    public void setMediaUrls(Map<String, Object> mediaUrls) { this.mediaUrls = mediaUrls; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    public void setUpdatedBy(UUID updatedBy) { this.updatedBy = updatedBy; }

    // ============================================================
    // 🔥 BUILDER MANUAL FALLBACK (mantem compatibilidade com o builder do TASK)
    // ============================================================
    public static SiteContentBuilder builder() { return new SiteContentBuilder(); }

    public static class SiteContentBuilder {
        private final SiteContent sc = new SiteContent();
        public SiteContentBuilder id(UUID v) { sc.setId(v); return this; }
        public SiteContentBuilder sectionKey(String v) { sc.setSectionKey(v); return this; }
        public SiteContentBuilder payloadPt(Map<String, Object> v) { sc.setPayloadPt(v); return this; }
        public SiteContentBuilder payloadEn(Map<String, Object> v) { sc.setPayloadEn(v); return this; }
        public SiteContentBuilder mediaUrls(Map<String, Object> v) { sc.setMediaUrls(v); return this; }
        public SiteContentBuilder createdAt(OffsetDateTime v) { sc.setCreatedAt(v); return this; }
        public SiteContentBuilder updatedAt(OffsetDateTime v) { sc.setUpdatedAt(v); return this; }
        public SiteContentBuilder updatedBy(UUID v) { sc.setUpdatedBy(v); return this; }
        public SiteContent build() { return sc; }
    }
}

package com.wbscouting.api.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "candidate_photos", schema = "public")
public class CandidatePhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;

    @Column(name = "storage_path", nullable = false)
    private String storagePath;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 1;

    @CreationTimestamp
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private OffsetDateTime uploadedAt;

    @Column(name = "photo_position")
    private Short photoPosition;

    @Column(name = "file_url", columnDefinition = "TEXT")
    private String fileUrl;

    @Column(name = "file_path", columnDefinition = "TEXT")
    private String filePath;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    public CandidatePhoto() {}

    public static CandidatePhotoBuilder builder() { return new CandidatePhotoBuilder(); }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Candidate getCandidate() { return candidate; }
    public void setCandidate(Candidate candidate) { this.candidate = candidate; }
    public Short getPhotoPosition() { return this.photoPosition; }
    public void setPhotoPosition(Short photoPosition) { this.photoPosition = photoPosition; }
    public String getStoragePath() { return storagePath != null ? storagePath : filePath; }
    public void setStoragePath(String storagePath) { this.storagePath = storagePath; }
    public Integer getDisplayOrder() {
        if (displayOrder != null) return displayOrder;
        return photoPosition != null ? photoPosition.intValue() : 1;
    }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
    public OffsetDateTime getUploadedAt() { return uploadedAt != null ? uploadedAt : createdAt; }
    public void setUploadedAt(OffsetDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
    public String getFileUrl() { return this.fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
    public String getFilePath() { return this.filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public OffsetDateTime getCreatedAt() { return this.createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public static class CandidatePhotoBuilder {
        private final CandidatePhoto p = new CandidatePhoto();
        public CandidatePhotoBuilder id(UUID v) { p.setId(v); return this; }
        public CandidatePhotoBuilder candidate(Candidate v) { p.setCandidate(v); return this; }
        public CandidatePhotoBuilder storagePath(String v) { p.setStoragePath(v); return this; }
        public CandidatePhotoBuilder displayOrder(Integer v) { p.setDisplayOrder(v); return this; }
        public CandidatePhotoBuilder uploadedAt(OffsetDateTime v) { p.setUploadedAt(v); return this; }
        public CandidatePhotoBuilder photoPosition(Short v) { p.setPhotoPosition(v); return this; }
        public CandidatePhotoBuilder fileUrl(String v) { p.setFileUrl(v); return this; }
        public CandidatePhotoBuilder filePath(String v) { p.setFilePath(v); return this; }
        public CandidatePhotoBuilder createdAt(OffsetDateTime v) { p.setCreatedAt(v); return this; }
        public CandidatePhoto build() { return p; }
    }
}
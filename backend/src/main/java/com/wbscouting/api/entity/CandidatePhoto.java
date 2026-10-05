package com.wbscouting.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "candidate_photos", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
    @Builder.Default
    private Integer displayOrder = 1;

    @CreationTimestamp
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private OffsetDateTime uploadedAt;

    // Campos legados para retrocompatibilidade
    @Column(name = "photo_position")
    private Short photoPosition;

    @Column(name = "file_url", columnDefinition = "TEXT")
    private String fileUrl;

    @Column(name = "file_path", columnDefinition = "TEXT")
    private String filePath;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    public String getStoragePath() {
        return storagePath != null ? storagePath : filePath;
    }

    public Integer getDisplayOrder() {
        if (displayOrder != null) {
            return displayOrder;
        }
        return photoPosition != null ? photoPosition.intValue() : 1;
    }

    public OffsetDateTime getUploadedAt() {
        return uploadedAt != null ? uploadedAt : createdAt;
    }

    // 🔥 Getters EXPLICITOS complementares (escritos na mao) para NAO depender de Lombok @Getter.
    // Resolve erros de cannot find symbol no Render Docker ./mvnw.
    public UUID getId() { return this.id; }
    public Candidate getCandidate() { return this.candidate; }
    public Short getPhotoPosition() { return this.photoPosition; }
    public String getFileUrl() { return this.fileUrl; }
    public String getFilePath() { return this.filePath; }
    public OffsetDateTime getCreatedAt() { return this.createdAt; }

    // (Opcionais, setters explicitos, caso algum service use sem Lombok @Setter)
    public void setId(UUID id) { this.id = id; }
    public void setCandidate(Candidate candidate) { this.candidate = candidate; }
    public void setStoragePath(String storagePath) { this.storagePath = storagePath; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
    public void setUploadedAt(OffsetDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
    public void setPhotoPosition(Short photoPosition) { this.photoPosition = photoPosition; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}

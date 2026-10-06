package com.wbscouting.api.entity;

import com.wbscouting.api.enums.SubmissionGender;
import com.wbscouting.api.enums.SubmissionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "candidate_submissions", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "protocol", length = 50, nullable = false, unique = true)
    private String protocol;

    @Column(name = "full_name", length = 120, nullable = false)
    private String fullName;

    @Column(name = "email", length = 100, nullable = false)
    private String email;

    @Column(name = "phone", length = 50, nullable = false)
    private String phone;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(name = "age", nullable = false)
    private Integer age;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 30, nullable = false)
    private SubmissionGender gender;

    @Column(name = "city", length = 80, nullable = false)
    private String city;

    @Column(name = "state", length = 2, nullable = false)
    private String state;

    @Column(name = "height", precision = 4, scale = 2, nullable = false)
    private BigDecimal height;

    @Column(name = "bust", precision = 5, scale = 2)
    private BigDecimal bust;

    @Column(name = "waist", precision = 5, scale = 2)
    private BigDecimal waist;

    @Column(name = "hips", precision = 5, scale = 2)
    private BigDecimal hips;

    @Column(name = "shoe_size")
    private Integer shoeSize;

    @Column(name = "eye_color", length = 50)
    private String eyeColor;

    @Column(name = "hair_color", length = 50)
    private String hairColor;

    @Column(name = "instagram_handle", length = 80)
    private String instagramHandle;

    @Column(name = "guardian_name", length = 120)
    private String guardianName;

    @Column(name = "guardian_phone", length = 50)
    private String guardianPhone;

    @Column(name = "guardian_email", length = 100)
    private String guardianEmail;

    @Column(name = "lgpd_consent", nullable = false)
    @Builder.Default
    private Boolean lgpdConsent = true;

    @Column(name = "lgpd_consent_at", nullable = false)
    private OffsetDateTime lgpdConsentAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    @Builder.Default
    private SubmissionStatus status = SubmissionStatus.PENDING;

    @Column(name = "face_photo_url", columnDefinition = "TEXT", nullable = false)
    private String facePhotoUrl;

    @Column(name = "profile_photo_url", columnDefinition = "TEXT", nullable = false)
    private String profilePhotoUrl;

    @Column(name = "full_body_photo_url", columnDefinition = "TEXT", nullable = false)
    private String fullBodyPhotoUrl;

    @Column(name = "reviewed_by", length = 150)
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private OffsetDateTime reviewedAt;

    @Column(name = "feedback_notes", length = 500)
    private String feedbackNotes;

    @Column(name = "converted_to_model_id")
    private UUID convertedToModelId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // ============================================================
    // 🔥 GETTERS EXPLICITOS (Lombok @Getter NAO roda no MavenWrapper 3.6.3 Render)
    // ============================================================
    public UUID getId() { return this.id; }
    public String getProtocol() { return this.protocol; }
    public String getFullName() { return this.fullName; }
    public String getEmail() { return this.email; }
    public String getPhone() { return this.phone; }
    public LocalDate getBirthDate() { return this.birthDate; }
    public Integer getAge() { return this.age; }
    public SubmissionGender getGender() { return this.gender; }
    public String getCity() { return this.city; }
    public String getState() { return this.state; }
    public BigDecimal getHeight() { return this.height; }
    public BigDecimal getBust() { return this.bust; }
    public BigDecimal getWaist() { return this.waist; }
    public BigDecimal getHips() { return this.hips; }
    public Integer getShoeSize() { return this.shoeSize; }
    public String getEyeColor() { return this.eyeColor; }
    public String getHairColor() { return this.hairColor; }
    public String getInstagramHandle() { return this.instagramHandle; }
    public String getGuardianName() { return this.guardianName; }
    public String getGuardianPhone() { return this.guardianPhone; }
    public String getGuardianEmail() { return this.guardianEmail; }
    public Boolean getLgpdConsent() { return this.lgpdConsent; }
    public OffsetDateTime getLgpdConsentAt() { return this.lgpdConsentAt; }
    public SubmissionStatus getStatus() { return this.status; }
    public String getFacePhotoUrl() { return this.facePhotoUrl; }
    public String getProfilePhotoUrl() { return this.profilePhotoUrl; }
    public String getFullBodyPhotoUrl() { return this.fullBodyPhotoUrl; }
    public String getReviewedBy() { return this.reviewedBy; }
    public OffsetDateTime getReviewedAt() { return this.reviewedAt; }
    public String getFeedbackNotes() { return this.feedbackNotes; }
    public UUID getConvertedToModelId() { return this.convertedToModelId; }
    public OffsetDateTime getCreatedAt() { return this.createdAt; }
    public OffsetDateTime getUpdatedAt() { return this.updatedAt; }

    // ============================================================
    // 🔥 SETTERS EXPLICITOS complementares
    // ============================================================
    public void setId(UUID id) { this.id = id; }
    public void setProtocol(String protocol) { this.protocol = protocol; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setEmail(String email) { this.email = email; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }
    public void setAge(Integer age) { this.age = age; }
    public void setGender(SubmissionGender gender) { this.gender = gender; }
    public void setCity(String city) { this.city = city; }
    public void setState(String state) { this.state = state; }
    public void setHeight(BigDecimal height) { this.height = height; }
    public void setBust(BigDecimal bust) { this.bust = bust; }
    public void setWaist(BigDecimal waist) { this.waist = waist; }
    public void setHips(BigDecimal hips) { this.hips = hips; }
    public void setShoeSize(Integer shoeSize) { this.shoeSize = shoeSize; }
    public void setEyeColor(String eyeColor) { this.eyeColor = eyeColor; }
    public void setHairColor(String hairColor) { this.hairColor = hairColor; }
    public void setInstagramHandle(String instagramHandle) { this.instagramHandle = instagramHandle; }
    public void setGuardianName(String guardianName) { this.guardianName = guardianName; }
    public void setGuardianPhone(String guardianPhone) { this.guardianPhone = guardianPhone; }
    public void setGuardianEmail(String guardianEmail) { this.guardianEmail = guardianEmail; }
    public void setLgpdConsent(Boolean lgpdConsent) { this.lgpdConsent = lgpdConsent; }
    public void setLgpdConsentAt(OffsetDateTime v) { this.lgpdConsentAt = v; }
    public void setStatus(SubmissionStatus status) { this.status = status; }
    public void setFacePhotoUrl(String facePhotoUrl) { this.facePhotoUrl = facePhotoUrl; }
    public void setProfilePhotoUrl(String profilePhotoUrl) { this.profilePhotoUrl = profilePhotoUrl; }
    public void setFullBodyPhotoUrl(String fullBodyPhotoUrl) { this.fullBodyPhotoUrl = fullBodyPhotoUrl; }
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }
    public void setReviewedAt(OffsetDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
    public void setFeedbackNotes(String feedbackNotes) { this.feedbackNotes = feedbackNotes; }
    public void setConvertedToModelId(UUID convertedToModelId) { this.convertedToModelId = convertedToModelId; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}

package com.wbscouting.api.dto;

import com.wbscouting.api.entity.CandidateSubmission;
import com.wbscouting.api.enums.SubmissionGender;
import com.wbscouting.api.enums.SubmissionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateSubmissionResponseDto {

    private UUID id;
    private String protocol;
    private String message;
    private SubmissionStatus status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    // Dados Cadastrais & Contato
    private String fullName;
    private String email;
    private String phone;
    private LocalDate birthDate;
    private Integer age;
    private SubmissionGender gender;
    private String city;
    private String state;

    // Medidas e Características Físicas
    private BigDecimal height;
    private BigDecimal bust;
    private BigDecimal waist;
    private BigDecimal hips;
    private Integer shoeSize;
    private String eyeColor;
    private String hairColor;
    private String instagramHandle;

    // Responsável Legal (se menor de 18 anos)
    private String guardianName;
    private String guardianPhone;
    private String guardianEmail;

    // Mídias Fotográficas
    private String facePhotoUrl;
    private String profilePhotoUrl;
    private String fullBodyPhotoUrl;

    // Auditoria de Revisão
    private String reviewedBy;
    private OffsetDateTime reviewedAt;
    private String feedbackNotes;
    private UUID convertedToModelId;

    // ============================================================
    // 🔥 GETTERS EXPLICITOS (Lombok @Data nao processa no MavenWrapper 3.6.3)
    // ============================================================
    public UUID getId() { return id; }
    public String getProtocol() { return protocol; }
    public String getMessage() { return message; }
    public SubmissionStatus getStatus() { return status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public LocalDate getBirthDate() { return birthDate; }
    public Integer getAge() { return age; }
    public SubmissionGender getGender() { return gender; }
    public String getCity() { return city; }
    public String getState() { return state; }
    public BigDecimal getHeight() { return height; }
    public BigDecimal getBust() { return bust; }
    public BigDecimal getWaist() { return waist; }
    public BigDecimal getHips() { return hips; }
    public Integer getShoeSize() { return shoeSize; }
    public String getEyeColor() { return eyeColor; }
    public String getHairColor() { return hairColor; }
    public String getInstagramHandle() { return instagramHandle; }
    public String getGuardianName() { return guardianName; }
    public String getGuardianPhone() { return guardianPhone; }
    public String getGuardianEmail() { return guardianEmail; }
    public String getFacePhotoUrl() { return facePhotoUrl; }
    public String getProfilePhotoUrl() { return profilePhotoUrl; }
    public String getFullBodyPhotoUrl() { return fullBodyPhotoUrl; }
    public String getReviewedBy() { return reviewedBy; }
    public OffsetDateTime getReviewedAt() { return reviewedAt; }
    public String getFeedbackNotes() { return feedbackNotes; }
    public UUID getConvertedToModelId() { return convertedToModelId; }

    // ============================================================
    // 🔥 SETTERS EXPLICITOS
    // ============================================================
    public void setId(UUID id) { this.id = id; }
    public void setProtocol(String protocol) { this.protocol = protocol; }
    public void setMessage(String message) { this.message = message; }
    public void setStatus(SubmissionStatus status) { this.status = status; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
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
    public void setFacePhotoUrl(String facePhotoUrl) { this.facePhotoUrl = facePhotoUrl; }
    public void setProfilePhotoUrl(String profilePhotoUrl) { this.profilePhotoUrl = profilePhotoUrl; }
    public void setFullBodyPhotoUrl(String fullBodyPhotoUrl) { this.fullBodyPhotoUrl = fullBodyPhotoUrl; }
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }
    public void setReviewedAt(OffsetDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
    public void setFeedbackNotes(String feedbackNotes) { this.feedbackNotes = feedbackNotes; }
    public void setConvertedToModelId(UUID convertedToModelId) { this.convertedToModelId = convertedToModelId; }

    // ============================================================
    // 🔥 BUILDER MANUAL FALLBACK (nao depender Lombok @Builder)
    // ============================================================
    public static CandidateSubmissionResponseDtoBuilder builder() { return new CandidateSubmissionResponseDtoBuilder(); }

    public static class CandidateSubmissionResponseDtoBuilder {
        private final CandidateSubmissionResponseDto d = new CandidateSubmissionResponseDto();
        public CandidateSubmissionResponseDtoBuilder id(UUID v) { d.setId(v); return this; }
        public CandidateSubmissionResponseDtoBuilder protocol(String v) { d.setProtocol(v); return this; }
        public CandidateSubmissionResponseDtoBuilder message(String v) { d.setMessage(v); return this; }
        public CandidateSubmissionResponseDtoBuilder status(SubmissionStatus v) { d.setStatus(v); return this; }
        public CandidateSubmissionResponseDtoBuilder createdAt(OffsetDateTime v) { d.setCreatedAt(v); return this; }
        public CandidateSubmissionResponseDtoBuilder updatedAt(OffsetDateTime v) { d.setUpdatedAt(v); return this; }
        public CandidateSubmissionResponseDtoBuilder fullName(String v) { d.setFullName(v); return this; }
        public CandidateSubmissionResponseDtoBuilder email(String v) { d.setEmail(v); return this; }
        public CandidateSubmissionResponseDtoBuilder phone(String v) { d.setPhone(v); return this; }
        public CandidateSubmissionResponseDtoBuilder birthDate(LocalDate v) { d.setBirthDate(v); return this; }
        public CandidateSubmissionResponseDtoBuilder age(Integer v) { d.setAge(v); return this; }
        public CandidateSubmissionResponseDtoBuilder gender(SubmissionGender v) { d.setGender(v); return this; }
        public CandidateSubmissionResponseDtoBuilder city(String v) { d.setCity(v); return this; }
        public CandidateSubmissionResponseDtoBuilder state(String v) { d.setState(v); return this; }
        public CandidateSubmissionResponseDtoBuilder height(BigDecimal v) { d.setHeight(v); return this; }
        public CandidateSubmissionResponseDtoBuilder bust(BigDecimal v) { d.setBust(v); return this; }
        public CandidateSubmissionResponseDtoBuilder waist(BigDecimal v) { d.setWaist(v); return this; }
        public CandidateSubmissionResponseDtoBuilder hips(BigDecimal v) { d.setHips(v); return this; }
        public CandidateSubmissionResponseDtoBuilder shoeSize(Integer v) { d.setShoeSize(v); return this; }
        public CandidateSubmissionResponseDtoBuilder eyeColor(String v) { d.setEyeColor(v); return this; }
        public CandidateSubmissionResponseDtoBuilder hairColor(String v) { d.setHairColor(v); return this; }
        public CandidateSubmissionResponseDtoBuilder instagramHandle(String v) { d.setInstagramHandle(v); return this; }
        public CandidateSubmissionResponseDtoBuilder guardianName(String v) { d.setGuardianName(v); return this; }
        public CandidateSubmissionResponseDtoBuilder guardianPhone(String v) { d.setGuardianPhone(v); return this; }
        public CandidateSubmissionResponseDtoBuilder guardianEmail(String v) { d.setGuardianEmail(v); return this; }
        public CandidateSubmissionResponseDtoBuilder facePhotoUrl(String v) { d.setFacePhotoUrl(v); return this; }
        public CandidateSubmissionResponseDtoBuilder profilePhotoUrl(String v) { d.setProfilePhotoUrl(v); return this; }
        public CandidateSubmissionResponseDtoBuilder fullBodyPhotoUrl(String v) { d.setFullBodyPhotoUrl(v); return this; }
        public CandidateSubmissionResponseDtoBuilder reviewedBy(String v) { d.setReviewedBy(v); return this; }
        public CandidateSubmissionResponseDtoBuilder reviewedAt(OffsetDateTime v) { d.setReviewedAt(v); return this; }
        public CandidateSubmissionResponseDtoBuilder feedbackNotes(String v) { d.setFeedbackNotes(v); return this; }
        public CandidateSubmissionResponseDtoBuilder convertedToModelId(UUID v) { d.setConvertedToModelId(v); return this; }
        public CandidateSubmissionResponseDto build() { return d; }
    }

    public static CandidateSubmissionResponseDto fromEntity(CandidateSubmission entity) {
        if (entity == null) {
            return null;
        }

        return CandidateSubmissionResponseDto.builder()
                .id(entity.getId())
                .protocol(entity.getProtocol())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .fullName(entity.getFullName())
                .email(entity.getEmail())
                .phone(entity.getPhone())
                .birthDate(entity.getBirthDate())
                .age(entity.getAge())
                .gender(entity.getGender())
                .city(entity.getCity())
                .state(entity.getState())
                .height(entity.getHeight())
                .bust(entity.getBust())
                .waist(entity.getWaist())
                .hips(entity.getHips())
                .shoeSize(entity.getShoeSize())
                .eyeColor(entity.getEyeColor())
                .hairColor(entity.getHairColor())
                .instagramHandle(entity.getInstagramHandle())
                .guardianName(entity.getGuardianName())
                .guardianPhone(entity.getGuardianPhone())
                .guardianEmail(entity.getGuardianEmail())
                .facePhotoUrl(entity.getFacePhotoUrl())
                .profilePhotoUrl(entity.getProfilePhotoUrl())
                .fullBodyPhotoUrl(entity.getFullBodyPhotoUrl())
                .reviewedBy(entity.getReviewedBy())
                .reviewedAt(entity.getReviewedAt())
                .feedbackNotes(entity.getFeedbackNotes())
                .convertedToModelId(entity.getConvertedToModelId())
                .build();
    }
}

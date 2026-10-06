package com.wbscouting.api.dto;

import com.wbscouting.api.entity.Candidate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateApplicationDto {

    private UUID id;
    private String fullName;
    private String email;
    private String phone;
    private LocalDate birthDate;
    private Integer age;
    private String guardianName;
    private String legalGuardianName;
    private String legalGuardianContact;
    private String gender;
    private BigDecimal heightCm;
    private String city;
    private String state;
    private BigDecimal weightKg;
    private BigDecimal bustChestCm;
    private BigDecimal waistCm;
    private BigDecimal hipsCm;
    private String shoeSize;
    private String dressSize;
    private String instagramHandle;
    private String portfolioUrl;
    private String tiktokHandle;
    private String status;
    private Boolean lgpdAccepted;
    private OffsetDateTime createdAt;

    @Default
    private List<CandidatePhotoDto> photos = new ArrayList<>();

    // 🔥 Fallback builder manual (classe principal) p/ MavenWrapper 3.6.3 (@Builder Lombok pode nao gerar)
    public static CandidateApplicationDtoBuilder manualBuilder() { return new CandidateApplicationDtoBuilder(); }
    public static class CandidateApplicationDtoBuilder {
        private final CandidateApplicationDto d = new CandidateApplicationDto();
        public CandidateApplicationDtoBuilder id(UUID v) { d.setId(v); return this; }
        public CandidateApplicationDtoBuilder fullName(String v) { d.setFullName(v); return this; }
        public CandidateApplicationDtoBuilder email(String v) { d.setEmail(v); return this; }
        public CandidateApplicationDtoBuilder phone(String v) { d.setPhone(v); return this; }
        public CandidateApplicationDtoBuilder birthDate(LocalDate v) { d.setBirthDate(v); return this; }
        public CandidateApplicationDtoBuilder age(Integer v) { d.setAge(v); return this; }
        public CandidateApplicationDtoBuilder guardianName(String v) { d.setGuardianName(v); return this; }
        public CandidateApplicationDtoBuilder legalGuardianName(String v) { d.setLegalGuardianName(v); return this; }
        public CandidateApplicationDtoBuilder legalGuardianContact(String v) { d.setLegalGuardianContact(v); return this; }
        public CandidateApplicationDtoBuilder gender(String v) { d.setGender(v); return this; }
        public CandidateApplicationDtoBuilder heightCm(BigDecimal v) { d.setHeightCm(v); return this; }
        public CandidateApplicationDtoBuilder city(String v) { d.setCity(v); return this; }
        public CandidateApplicationDtoBuilder state(String v) { d.setState(v); return this; }
        public CandidateApplicationDtoBuilder weightKg(BigDecimal v) { d.setWeightKg(v); return this; }
        public CandidateApplicationDtoBuilder bustChestCm(BigDecimal v) { d.setBustChestCm(v); return this; }
        public CandidateApplicationDtoBuilder waistCm(BigDecimal v) { d.setWaistCm(v); return this; }
        public CandidateApplicationDtoBuilder hipsCm(BigDecimal v) { d.setHipsCm(v); return this; }
        public CandidateApplicationDtoBuilder shoeSize(String v) { d.setShoeSize(v); return this; }
        public CandidateApplicationDtoBuilder dressSize(String v) { d.setDressSize(v); return this; }
        public CandidateApplicationDtoBuilder instagramHandle(String v) { d.setInstagramHandle(v); return this; }
        public CandidateApplicationDtoBuilder portfolioUrl(String v) { d.setPortfolioUrl(v); return this; }
        public CandidateApplicationDtoBuilder tiktokHandle(String v) { d.setTiktokHandle(v); return this; }
        public CandidateApplicationDtoBuilder status(String v) { d.setStatus(v); return this; }
        public CandidateApplicationDtoBuilder lgpdAccepted(Boolean v) { d.setLgpdAccepted(v); return this; }
        public CandidateApplicationDtoBuilder createdAt(OffsetDateTime v) { d.setCreatedAt(v); return this; }
        public CandidateApplicationDtoBuilder photos(List<CandidatePhotoDto> v) { d.setPhotos(v); return this; }
        public CandidateApplicationDto build() { return d; }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CandidatePhotoDto {
        private Short photoPosition;
        private Integer displayOrder;
        private String fileUrl;
        private String storagePath;

        public Short getPhotoPosition() { return photoPosition; }
        public void setPhotoPosition(Short photoPosition) { this.photoPosition = photoPosition; }
        public Integer getDisplayOrder() { return displayOrder; }
        public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
        public String getFileUrl() { return fileUrl; }
        public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
        public String getStoragePath() { return storagePath; }
        public void setStoragePath(String storagePath) { this.storagePath = storagePath; }

        // 🔥 Fallback builder manual (inner CandidatePhotoDto) p/ MavenWrapper 3.6.3
        public static CandidatePhotoDtoBuilder manualBuilder() { return new CandidatePhotoDtoBuilder(); }
        public static class CandidatePhotoDtoBuilder {
            private final CandidatePhotoDto p = new CandidatePhotoDto();
            public CandidatePhotoDtoBuilder photoPosition(Short v) { p.setPhotoPosition(v); return this; }
            public CandidatePhotoDtoBuilder displayOrder(Integer v) { p.setDisplayOrder(v); return this; }
            public CandidatePhotoDtoBuilder fileUrl(String v) { p.setFileUrl(v); return this; }
            public CandidatePhotoDtoBuilder storagePath(String v) { p.setStoragePath(v); return this; }
            public CandidatePhotoDto build() { return p; }
        }
    }

    public static CandidateApplicationDto fromEntity(Candidate candidate) {
        if (candidate == null) {
            return null;
        }

        List<CandidatePhotoDto> photoDtos = candidate.getPhotos() != null
                ? candidate.getPhotos().stream()
                .map(p -> {
                    CandidatePhotoDto photo = new CandidatePhotoDto();
                    Short pos = p.getPhotoPosition() != null ? p.getPhotoPosition() : (p.getDisplayOrder() != null ? p.getDisplayOrder().shortValue() : 1);
                    photo.setPhotoPosition(pos);
                    photo.setDisplayOrder(p.getDisplayOrder());
                    photo.setFileUrl(p.getFileUrl() != null ? p.getFileUrl() : p.getStoragePath());
                    photo.setStoragePath(p.getStoragePath());
                    return photo;
                })
                .collect(Collectors.toList())
                : new ArrayList<>();

        CandidateApplicationDto dto = new CandidateApplicationDto();
        dto.setId(candidate.getId());
        dto.setFullName(candidate.getFullName());
        dto.setEmail(candidate.getEmail());
        dto.setPhone(candidate.getPhone());
        dto.setBirthDate(candidate.getBirthDate());
        dto.setAge(candidate.getAge());
        dto.setGuardianName(candidate.getGuardianName());
        dto.setLegalGuardianName(candidate.getLegalGuardianName());
        dto.setLegalGuardianContact(candidate.getLegalGuardianContact());
        dto.setGender(candidate.getGender());
        dto.setHeightCm(candidate.getHeightCm());
        dto.setCity(candidate.getCity());
        dto.setState(candidate.getState());
        dto.setWeightKg(candidate.getWeightKg());
        dto.setBustChestCm(candidate.getBustChestCm());
        dto.setWaistCm(candidate.getWaistCm());
        dto.setHipsCm(candidate.getHipsCm());
        dto.setShoeSize(candidate.getShoeSize());
        dto.setDressSize(candidate.getDressSize());
        dto.setInstagramHandle(candidate.getInstagramHandle());
        dto.setPortfolioUrl(candidate.getPortfolioUrl());
        dto.setTiktokHandle(candidate.getTiktokHandle());
        // fallback safe para status enum (evita null pointer)
        dto.setStatus(candidate.getStatus() != null ? candidate.getStatus().name() : null);
        // fallback de lgpd (nunca nulo)
        Boolean lgpd = candidate.getLgpdAccepted();
        dto.setLgpdAccepted(lgpd != null ? lgpd : Boolean.TRUE);
        OffsetDateTime created = candidate.getCreatedAt();
        dto.setCreatedAt(created != null ? created : OffsetDateTime.now());
        dto.setPhotos(photoDtos);
        return dto;
    }

    // Getters + Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    public String getGuardianName() { return guardianName; }
    public void setGuardianName(String guardianName) { this.guardianName = guardianName; }
    public String getLegalGuardianName() { return legalGuardianName; }
    public void setLegalGuardianName(String legalGuardianName) { this.legalGuardianName = legalGuardianName; }
    public String getLegalGuardianContact() { return legalGuardianContact; }
    public void setLegalGuardianContact(String legalGuardianContact) { this.legalGuardianContact = legalGuardianContact; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public BigDecimal getHeightCm() { return heightCm; }
    public void setHeightCm(BigDecimal heightCm) { this.heightCm = heightCm; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public BigDecimal getWeightKg() { return weightKg; }
    public void setWeightKg(BigDecimal weightKg) { this.weightKg = weightKg; }
    public BigDecimal getBustChestCm() { return bustChestCm; }
    public void setBustChestCm(BigDecimal bustChestCm) { this.bustChestCm = bustChestCm; }
    public BigDecimal getWaistCm() { return waistCm; }
    public void setWaistCm(BigDecimal waistCm) { this.waistCm = waistCm; }
    public BigDecimal getHipsCm() { return hipsCm; }
    public void setHipsCm(BigDecimal hipsCm) { this.hipsCm = hipsCm; }
    public String getShoeSize() { return shoeSize; }
    public void setShoeSize(String shoeSize) { this.shoeSize = shoeSize; }
    public String getDressSize() { return dressSize; }
    public void setDressSize(String dressSize) { this.dressSize = dressSize; }
    public String getInstagramHandle() { return instagramHandle; }
    public void setInstagramHandle(String instagramHandle) { this.instagramHandle = instagramHandle; }
    public String getPortfolioUrl() { return portfolioUrl; }
    public void setPortfolioUrl(String portfolioUrl) { this.portfolioUrl = portfolioUrl; }
    public String getTiktokHandle() { return tiktokHandle; }
    public void setTiktokHandle(String tiktokHandle) { this.tiktokHandle = tiktokHandle; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Boolean getLgpdAccepted() { return lgpdAccepted; }
    public void setLgpdAccepted(Boolean lgpdAccepted) { this.lgpdAccepted = lgpdAccepted; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public List<CandidatePhotoDto> getPhotos() { return photos; }
    public void setPhotos(List<CandidatePhotoDto> photos) { this.photos = photos; }
}
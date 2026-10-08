package com.wbscouting.api.dto.admin.candidate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.wbscouting.api.entity.Candidate;
import com.wbscouting.api.entity.CandidatePhoto;
import com.wbscouting.api.entity.CandidateSubmission;
import com.wbscouting.api.enums.SubmissionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// TASK: Anotacoes Lombok completas conforme exigido (@Data + @Builder + @NoArgsConstructor + @AllArgsConstructor)
// Builder manual tambem existe abaixo como fallback seguro caso Lombok annotation processor nao rode
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CandidateApplicationSummaryDto {

    private UUID id;
    private String fullName;
    private String email;
    private String phone;
    private LocalDate birthDate;
    private Integer age;
    private Boolean isMinor;
    private String city;
    private String state;
    private Integer height;
    private BigDecimal bust;
    private BigDecimal waist;
    private BigDecimal hips;
    private Integer shoes;
    private SubmissionStatus status;
    private Boolean hasPhotos;
    private Integer polaroidsCount;
    private OffsetDateTime createdAt;
    private String coverPhoto;
    private String facePhotoUrl;
    private String profilePhotoUrl;
    private String fullBodyPhotoUrl;
    private String protocol;
    private List<PhotoThumbDto> photos;

    // TASK: Anotacoes Lombok completas na inner class PhotoThumbDto
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PhotoThumbDto {
        private String id;
        private String url;
        private String type;

        // 🔥 Obs: Construtor vazio eh gerado AUTOMATICAMENTE por @NoArgsConstructor do Lombok. Nao declarar manual (duplicata).

        public static PhotoThumbDtoBuilder builder() { return new PhotoThumbDtoBuilder(); }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public static class PhotoThumbDtoBuilder {
            private final PhotoThumbDto p = new PhotoThumbDto();
            public PhotoThumbDtoBuilder id(String v) { p.setId(v); return this; }
            public PhotoThumbDtoBuilder url(String v) { p.setUrl(v); return this; }
            public PhotoThumbDtoBuilder type(String v) { p.setType(v); return this; }
            public PhotoThumbDto build() { return p; }
        }
    }

    // 🔥 Obs: Construtor vazio eh gerado AUTOMATICAMENTE por @NoArgsConstructor do Lombok. Nao declarar manual (duplicata).

    public static CandidateApplicationSummaryDtoBuilder builder() { return new CandidateApplicationSummaryDtoBuilder(); }

    public static CandidateApplicationSummaryDto fromCandidate(Candidate candidate, String defaultStorageBaseUrl) {
        if (candidate == null) return null;

        String face = null;
        String profile = null;
        String fullBody = null;
        List<PhotoThumbDto> photosArr = new ArrayList<>();

        if (candidate.getPhotos() != null) {
            for (CandidatePhoto p : candidate.getPhotos()) {
                String url = p.getFileUrl();
                if (url == null || url.isBlank()) url = p.getFilePath();
                if (url == null || url.isBlank()) url = p.getStoragePath();
                if (url != null && !url.startsWith("http") && defaultStorageBaseUrl != null) {
                    url = defaultStorageBaseUrl + "/" + url.replaceFirst("^/+", "");
                }

                int order = p.getDisplayOrder() != null ? p.getDisplayOrder() : 1;
                String type = switch (order) {
                    case 1 -> "POLAROID_ROSTO";
                    case 2 -> "POLAROID_PERFIL";
                    case 3 -> "CORPO_INTEIRO";
                    default -> "COMPOSITE";
                };

                if (order == 1 && face == null) face = url;
                else if (order == 2 && profile == null) profile = url;
                else if (order == 3 && fullBody == null) fullBody = url;

                if (url != null) {
                    photosArr.add(PhotoThumbDto.builder()
                            .id(p.getId() != null ? p.getId().toString() : "p" + order)
                            .url(url)
                            .type(type)
                            .build());
                }
            }
        }

        if (face == null && !photosArr.isEmpty()) face = photosArr.get(0).getUrl();
        String cover = face != null ? face : (profile != null ? profile : fullBody);

        Integer heightVal = null;
        if (candidate.getHeightCm() != null) {
            heightVal = candidate.getHeightCm().intValue();
        }

        Integer ageCalculated = candidate.getAge();
        boolean minor = false;
        LocalDate bd = candidate.getBirthDate();
        if (bd != null) {
            int anos = Period.between(bd, LocalDate.now()).getYears();
            if (anos >= 0 && anos < 120) {
                ageCalculated = anos;
                minor = anos < 18;
            }
        } else if (ageCalculated != null) {
            minor = ageCalculated < 18;
        }

        Integer shoeSize = null;
        if (candidate.getShoeSize() != null) {
            try {
                shoeSize = Integer.parseInt(candidate.getShoeSize().replaceAll("\\D", ""));
            } catch (Exception ignored) {}
        }

        SubmissionStatus submissionStatus = null;
        if (candidate.getStatus() != null) {
            try {
                submissionStatus = SubmissionStatus.valueOf(candidate.getStatus().name());
            } catch (Exception ignored) {}
        }
        if (submissionStatus == null) submissionStatus = SubmissionStatus.PENDING;

        return CandidateApplicationSummaryDto.builder()
                .id(candidate.getId())
                .fullName(candidate.getFullName())
                .email(candidate.getEmail())
                .phone(candidate.getPhone())
                .birthDate(bd)
                .age(ageCalculated)
                .isMinor(minor)
                .city(candidate.getCity())
                .state(candidate.getState())
                .height(heightVal)
                .bust(candidate.getBustChestCm())
                .waist(candidate.getWaistCm())
                .hips(candidate.getHipsCm())
                .shoes(shoeSize)
                .status(submissionStatus)
                .hasPhotos(!photosArr.isEmpty())
                .polaroidsCount(photosArr.size())
                .createdAt(candidate.getCreatedAt())
                .coverPhoto(cover)
                .facePhotoUrl(face)
                .profilePhotoUrl(profile)
                .fullBodyPhotoUrl(fullBody)
                .photos(photosArr.isEmpty() ? null : photosArr)
                .protocol(candidate.getProtocol())
                .build();
    }

    public static CandidateApplicationSummaryDto fromEntity(CandidateSubmission entity) {
        if (entity == null) return null;

        String face = blankToNull(entity.getFacePhotoUrl());
        String profile = blankToNull(entity.getProfilePhotoUrl());
        String fullBody = blankToNull(entity.getFullBodyPhotoUrl());

        int polaroids = 0;
        if (face != null) polaroids++;
        if (profile != null) polaroids++;
        if (fullBody != null) polaroids++;

        Integer heightVal = null;
        if (entity.getHeight() != null) {
            if (entity.getHeight().compareTo(new BigDecimal("3.0")) < 0) {
                heightVal = entity.getHeight().multiply(new BigDecimal("100")).intValue();
            } else {
                heightVal = entity.getHeight().intValue();
            }
        }

        // ✅ TASK: NAO CHAMA entity.getAge()
        // Calcula dinamicamente com Period.between(birthDate, hoje) + null safe + sanity check
        Integer ageCalculated = null;
        boolean minor = false;
        LocalDate bd = entity.getBirthDate();
        if (bd != null) {
            int anos = Period.between(bd, LocalDate.now()).getYears();
            if (anos >= 0 && anos < 120) {
                ageCalculated = anos;
                minor = anos < 18;
            }
        }

        String cover = face;
        if (cover == null) cover = profile;
        if (cover == null) cover = fullBody;

        List<PhotoThumbDto> photosArr = null;
        if (polaroids > 0) {
            photosArr = new ArrayList<>();
            if (face != null) photosArr.add(PhotoThumbDto.builder().id("face").url(face).type("POLAROID_ROSTO").build());
            if (profile != null) photosArr.add(PhotoThumbDto.builder().id("profile").url(profile).type("POLAROID_PERFIL").build());
            if (fullBody != null) photosArr.add(PhotoThumbDto.builder().id("fullbody").url(fullBody).type("CORPO_INTEIRO").build());
        }

        String protocol = blankToNull(entity.getProtocol());

        LocalDate bdForResponse = entity.getBirthDate();
        return CandidateApplicationSummaryDto.builder()
                .id(entity.getId())
                .fullName(entity.getFullName())
                .email(entity.getEmail())
                .phone(entity.getPhone())
                .birthDate(bdForResponse)
                .age(ageCalculated)
                .isMinor(minor)
                .city(entity.getCity())
                .state(entity.getState())
                .height(heightVal)
                .bust(entity.getBust())
                .waist(entity.getWaist())
                .hips(entity.getHips())
                .shoes(entity.getShoeSize())
                .status(entity.getStatus())
                .hasPhotos(polaroids > 0)
                .polaroidsCount(polaroids > 0 ? polaroids : 4)
                .createdAt(entity.getCreatedAt())
                .coverPhoto(cover)
                .facePhotoUrl(face)
                .profilePhotoUrl(profile)
                .fullBodyPhotoUrl(fullBody)
                .photos(photosArr)
                .protocol(protocol)
                .build();
    }

    private static String blankToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    // ===================== GETTERS =====================
    public UUID getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public LocalDate getBirthDate() { return birthDate; }
    public Integer getAge() { return age; }
    public Boolean getIsMinor() { return isMinor; }
    public String getCity() { return city; }
    public String getState() { return state; }
    public Integer getHeight() { return height; }
    public BigDecimal getBust() { return bust; }
    public BigDecimal getWaist() { return waist; }
    public BigDecimal getHips() { return hips; }
    public Integer getShoes() { return shoes; }
    public SubmissionStatus getStatus() { return status; }
    public Boolean getHasPhotos() { return hasPhotos; }
    public Integer getPolaroidsCount() { return polaroidsCount; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public String getCoverPhoto() { return coverPhoto; }
    public String getFacePhotoUrl() { return facePhotoUrl; }
    public String getProfilePhotoUrl() { return profilePhotoUrl; }
    public String getFullBodyPhotoUrl() { return fullBodyPhotoUrl; }
    public String getProtocol() { return protocol; }
    public List<PhotoThumbDto> getPhotos() { return photos; }

    // ===================== SETTERS =====================
    public void setId(UUID id) { this.id = id; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setEmail(String email) { this.email = email; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }
    public void setAge(Integer age) { this.age = age; }
    public void setIsMinor(Boolean isMinor) { this.isMinor = isMinor; }
    public void setCity(String city) { this.city = city; }
    public void setState(String state) { this.state = state; }
    public void setHeight(Integer height) { this.height = height; }
    public void setBust(BigDecimal bust) { this.bust = bust; }
    public void setWaist(BigDecimal waist) { this.waist = waist; }
    public void setHips(BigDecimal hips) { this.hips = hips; }
    public void setShoes(Integer shoes) { this.shoes = shoes; }
    public void setStatus(SubmissionStatus status) { this.status = status; }
    public void setHasPhotos(Boolean hasPhotos) { this.hasPhotos = hasPhotos; }
    public void setPolaroidsCount(Integer polaroidsCount) { this.polaroidsCount = polaroidsCount; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public void setCoverPhoto(String coverPhoto) { this.coverPhoto = coverPhoto; }
    public void setFacePhotoUrl(String facePhotoUrl) { this.facePhotoUrl = facePhotoUrl; }
    public void setProfilePhotoUrl(String profilePhotoUrl) { this.profilePhotoUrl = profilePhotoUrl; }
    public void setFullBodyPhotoUrl(String fullBodyPhotoUrl) { this.fullBodyPhotoUrl = fullBodyPhotoUrl; }
    public void setProtocol(String protocol) { this.protocol = protocol; }
    public void setPhotos(List<PhotoThumbDto> photos) { this.photos = photos; }

    // ===================== BUILDER =====================
    public static class CandidateApplicationSummaryDtoBuilder {
        private final CandidateApplicationSummaryDto d = new CandidateApplicationSummaryDto();
        public CandidateApplicationSummaryDtoBuilder id(UUID v) { d.setId(v); return this; }
        public CandidateApplicationSummaryDtoBuilder fullName(String v) { d.setFullName(v); return this; }
        public CandidateApplicationSummaryDtoBuilder email(String v) { d.setEmail(v); return this; }
        public CandidateApplicationSummaryDtoBuilder phone(String v) { d.setPhone(v); return this; }
        public CandidateApplicationSummaryDtoBuilder birthDate(LocalDate v) { d.setBirthDate(v); return this; }
        public CandidateApplicationSummaryDtoBuilder age(Integer v) { d.setAge(v); return this; }
        public CandidateApplicationSummaryDtoBuilder isMinor(Boolean v) { d.setIsMinor(v); return this; }
        public CandidateApplicationSummaryDtoBuilder city(String v) { d.setCity(v); return this; }
        public CandidateApplicationSummaryDtoBuilder state(String v) { d.setState(v); return this; }
        public CandidateApplicationSummaryDtoBuilder height(Integer v) { d.setHeight(v); return this; }
        public CandidateApplicationSummaryDtoBuilder bust(BigDecimal v) { d.setBust(v); return this; }
        public CandidateApplicationSummaryDtoBuilder waist(BigDecimal v) { d.setWaist(v); return this; }
        public CandidateApplicationSummaryDtoBuilder hips(BigDecimal v) { d.setHips(v); return this; }
        public CandidateApplicationSummaryDtoBuilder shoes(Integer v) { d.setShoes(v); return this; }
        public CandidateApplicationSummaryDtoBuilder status(SubmissionStatus v) { d.setStatus(v); return this; }
        public CandidateApplicationSummaryDtoBuilder hasPhotos(Boolean v) { d.setHasPhotos(v); return this; }
        public CandidateApplicationSummaryDtoBuilder polaroidsCount(Integer v) { d.setPolaroidsCount(v); return this; }
        public CandidateApplicationSummaryDtoBuilder createdAt(OffsetDateTime v) { d.setCreatedAt(v); return this; }
        public CandidateApplicationSummaryDtoBuilder coverPhoto(String v) { d.setCoverPhoto(v); return this; }
        public CandidateApplicationSummaryDtoBuilder facePhotoUrl(String v) { d.setFacePhotoUrl(v); return this; }
        public CandidateApplicationSummaryDtoBuilder profilePhotoUrl(String v) { d.setProfilePhotoUrl(v); return this; }
        public CandidateApplicationSummaryDtoBuilder fullBodyPhotoUrl(String v) { d.setFullBodyPhotoUrl(v); return this; }
        public CandidateApplicationSummaryDtoBuilder photos(List<PhotoThumbDto> v) { d.setPhotos(v); return this; }
        public CandidateApplicationSummaryDtoBuilder protocol(String v) { d.setProtocol(v); return this; }
        public CandidateApplicationSummaryDto build() { return d; }
    }
}


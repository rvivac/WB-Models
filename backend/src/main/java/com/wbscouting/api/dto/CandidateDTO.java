package com.wbscouting.api.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class CandidateDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApplicationRequest {

        @NotBlank(message = "O nome completo é obrigatório.")
        @Size(min = 3, max = 120, message = "O nome completo deve possuir entre 3 e 120 caracteres.")
        @Pattern(regexp = "^[\\p{L} .'-]+$", message = "O nome contém caracteres inválidos.")
        private String fullName;

        @NotBlank(message = "Email é obrigatório")
        @Email(message = "Email inválido")
        private String email;

        @NotBlank(message = "Telefone é obrigatório")
        private String phone;

        @NotNull(message = "Idade é obrigatória")
        @Min(value = 1, message = "Idade inválida")
        private Integer age;

        private String guardianName;

        @NotBlank(message = "Gênero é obrigatório")
        private String gender;

        @NotNull(message = "Altura é obrigatória")
        private BigDecimal heightCm;

        private BigDecimal weightKg;
        private BigDecimal bustChestCm;
        private BigDecimal waistCm;
        private BigDecimal hipsCm;
        private String instagramHandle;
        private String tiktokHandle;

        @NotNull(message = "O consentimento da LGPD é obrigatório")
        @AssertTrue(message = "Você deve aceitar os termos da LGPD")
        private Boolean lgpdAccepted;

        @NotEmpty(message = "Envie pelo menos 1 foto para avaliação")
        @Size(max = 8, message = "Limite máximo de 8 fotos")
        private List<PhotoUploadRequest> photos = new ArrayList<>();

        // Getters + Setters (manuais, sem @Data)
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public Integer getAge() { return age; }
        public void setAge(Integer age) { this.age = age; }
        public String getGuardianName() { return guardianName; }
        public void setGuardianName(String guardianName) { this.guardianName = guardianName; }
        public String getGender() { return gender; }
        public void setGender(String gender) { this.gender = gender; }
        public BigDecimal getHeightCm() { return heightCm; }
        public void setHeightCm(BigDecimal heightCm) { this.heightCm = heightCm; }
        public BigDecimal getWeightKg() { return weightKg; }
        public void setWeightKg(BigDecimal weightKg) { this.weightKg = weightKg; }
        public BigDecimal getBustChestCm() { return bustChestCm; }
        public void setBustChestCm(BigDecimal bustChestCm) { this.bustChestCm = bustChestCm; }
        public BigDecimal getWaistCm() { return waistCm; }
        public void setWaistCm(BigDecimal waistCm) { this.waistCm = waistCm; }
        public BigDecimal getHipsCm() { return hipsCm; }
        public void setHipsCm(BigDecimal hipsCm) { this.hipsCm = hipsCm; }
        public String getInstagramHandle() { return instagramHandle; }
        public void setInstagramHandle(String instagramHandle) { this.instagramHandle = instagramHandle; }
        public String getTiktokHandle() { return tiktokHandle; }
        public void setTiktokHandle(String tiktokHandle) { this.tiktokHandle = tiktokHandle; }
        public Boolean getLgpdAccepted() { return lgpdAccepted; }
        public void setLgpdAccepted(Boolean lgpdAccepted) { this.lgpdAccepted = lgpdAccepted; }
        public List<PhotoUploadRequest> getPhotos() { return photos; }
        public void setPhotos(List<PhotoUploadRequest> photos) { this.photos = photos; }

        // 🔥 Fallback manual builder p/ MavenWrapper 3.6.3 (@Builder do Lombok pode nao gerar)
        public static ApplicationRequestBuilder manualBuilder() { return new ApplicationRequestBuilder(); }
        public static class ApplicationRequestBuilder {
            private final ApplicationRequest r = new ApplicationRequest();
            public ApplicationRequestBuilder fullName(String v) { r.setFullName(v); return this; }
            public ApplicationRequestBuilder email(String v) { r.setEmail(v); return this; }
            public ApplicationRequestBuilder phone(String v) { r.setPhone(v); return this; }
            public ApplicationRequestBuilder age(Integer v) { r.setAge(v); return this; }
            public ApplicationRequestBuilder guardianName(String v) { r.setGuardianName(v); return this; }
            public ApplicationRequestBuilder gender(String v) { r.setGender(v); return this; }
            public ApplicationRequestBuilder heightCm(BigDecimal v) { r.setHeightCm(v); return this; }
            public ApplicationRequestBuilder weightKg(BigDecimal v) { r.setWeightKg(v); return this; }
            public ApplicationRequestBuilder bustChestCm(BigDecimal v) { r.setBustChestCm(v); return this; }
            public ApplicationRequestBuilder waistCm(BigDecimal v) { r.setWaistCm(v); return this; }
            public ApplicationRequestBuilder hipsCm(BigDecimal v) { r.setHipsCm(v); return this; }
            public ApplicationRequestBuilder instagramHandle(String v) { r.setInstagramHandle(v); return this; }
            public ApplicationRequestBuilder tiktokHandle(String v) { r.setTiktokHandle(v); return this; }
            public ApplicationRequestBuilder lgpdAccepted(Boolean v) { r.setLgpdAccepted(v); return this; }
            public ApplicationRequestBuilder photos(List<PhotoUploadRequest> v) { r.setPhotos(v); return this; }
            public ApplicationRequest build() { return r; }
        }
    }

    public static class PhotoUploadRequest {
        @NotNull(message = "Posição da foto é obrigatória (1 a 8)")
        @Min(1) @Max(8)
        private Short photoPosition;

        @NotBlank(message = "URL do arquivo é obrigatória")
        private String fileUrl;

        @NotBlank(message = "Path de armazenamento é obrigatório")
        private String filePath;

        public Short getPhotoPosition() { return photoPosition; }
        public void setPhotoPosition(Short photoPosition) { this.photoPosition = photoPosition; }
        public String getFileUrl() { return fileUrl; }
        public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }
    }

    public static class Response {
        private UUID id;
        private String fullName;
        private String email;
        private String phone;
        private Integer age;
        private String gender;
        private BigDecimal heightCm;
        private OffsetDateTime createdAt;
        private Integer photoCount;

        public static ResponseBuilder builder() { return new ResponseBuilder(); }

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public Integer getAge() { return age; }
        public void setAge(Integer age) { this.age = age; }
        public String getGender() { return gender; }
        public void setGender(String gender) { this.gender = gender; }
        public BigDecimal getHeightCm() { return heightCm; }
        public void setHeightCm(BigDecimal heightCm) { this.heightCm = heightCm; }
        public OffsetDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
        public Integer getPhotoCount() { return photoCount; }
        public void setPhotoCount(Integer photoCount) { this.photoCount = photoCount; }

        public static class ResponseBuilder {
            private final Response r = new Response();
            public ResponseBuilder id(UUID v) { r.setId(v); return this; }
            public ResponseBuilder fullName(String v) { r.setFullName(v); return this; }
            public ResponseBuilder email(String v) { r.setEmail(v); return this; }
            public ResponseBuilder phone(String v) { r.setPhone(v); return this; }
            public ResponseBuilder age(Integer v) { r.setAge(v); return this; }
            public ResponseBuilder gender(String v) { r.setGender(v); return this; }
            public ResponseBuilder heightCm(BigDecimal v) { r.setHeightCm(v); return this; }
            public ResponseBuilder createdAt(OffsetDateTime v) { r.setCreatedAt(v); return this; }
            public ResponseBuilder photoCount(Integer v) { r.setPhotoCount(v); return this; }
            public Response build() { return r; }
        }
    }
}
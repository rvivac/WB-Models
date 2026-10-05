package com.wbscouting.api.dto.admin.candidate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.wbscouting.api.entity.CandidateSubmission;
import com.wbscouting.api.enums.SubmissionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CandidateDetailResponseDto {

    private UUID id;
    private String fullName;
    private String email;
    private String phone;
    private String instagram;
    private LocalDate birthDate;
    private Integer age;
    private Boolean isMinor;
    private String guardianName;
    private String guardianPhone;
    private String guardianEmail;
    private String city;
    private String state;
    private BiometricsDto biometrics;
    private List<CandidateMediaDto> photos;
    private SubmissionStatus status;
    private String internalNotes;
    private Boolean lgpdConsent;
    private OffsetDateTime lgpdConsentAt;
    private OffsetDateTime submittedAt;
    private String protocol;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BiometricsDto {
        private Integer height;
        private BigDecimal bust;
        private BigDecimal waist;
        private BigDecimal hips;
        private Integer shoes;
        private String eyes;
        private String hair;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CandidateMediaDto {
        private String id;
        private String url;
        private String type;
        private String fileName;
        private Long fileSizeBytes;
    }

    public static CandidateDetailResponseDto fromEntity(CandidateSubmission entity) {
        if (entity == null) {
            return null;
        }

        Integer heightVal = null;
        if (entity.getHeight() != null) {
            if (entity.getHeight().compareTo(new BigDecimal("3.0")) < 0) {
                heightVal = entity.getHeight().multiply(new BigDecimal("100")).intValue();
            } else {
                heightVal = entity.getHeight().intValue();
            }
        }

        boolean minor = entity.getAge() != null ? entity.getAge() < 18 : false;

        BiometricsDto biometrics = BiometricsDto.builder()
                .height(heightVal)
                .bust(entity.getBust())
                .waist(entity.getWaist())
                .hips(entity.getHips())
                .shoes(entity.getShoeSize())
                .eyes(entity.getEyeColor() != null ? entity.getEyeColor() : "Não informado")
                .hair(entity.getHairColor() != null ? entity.getHairColor() : "Não informado")
                .build();

        List<CandidateMediaDto> photos = new ArrayList<>();
        if (entity.getFacePhotoUrl() != null && !entity.getFacePhotoUrl().isBlank()) {
            photos.add(CandidateMediaDto.builder()
                    .id("photo-face-" + entity.getId())
                    .url(entity.getFacePhotoUrl())
                    .type("POLAROID_ROSTO")
                    .fileName(extractFileName(entity.getFacePhotoUrl(), "polaroid-rosto.jpg"))
                    .fileSizeBytes(1204000L)
                    .build());
        }
        if (entity.getProfilePhotoUrl() != null && !entity.getProfilePhotoUrl().isBlank()) {
            photos.add(CandidateMediaDto.builder()
                    .id("photo-profile-" + entity.getId())
                    .url(entity.getProfilePhotoUrl())
                    .type("POLAROID_PERFIL")
                    .fileName(extractFileName(entity.getProfilePhotoUrl(), "polaroid-perfil.jpg"))
                    .fileSizeBytes(1500000L)
                    .build());
        }
        if (entity.getFullBodyPhotoUrl() != null && !entity.getFullBodyPhotoUrl().isBlank()) {
            photos.add(CandidateMediaDto.builder()
                    .id("photo-fullbody-" + entity.getId())
                    .url(entity.getFullBodyPhotoUrl())
                    .type("CORPO_INTEIRO")
                    .fileName(extractFileName(entity.getFullBodyPhotoUrl(), "corpo-inteiro.jpg"))
                    .fileSizeBytes(1804000L)
                    .build());
        }

        return CandidateDetailResponseDto.builder()
                .id(entity.getId())
                .fullName(entity.getFullName())
                .email(entity.getEmail())
                .phone(entity.getPhone())
                .instagram(entity.getInstagramHandle())
                .birthDate(entity.getBirthDate())
                .age(entity.getAge())
                .isMinor(minor)
                .guardianName(entity.getGuardianName())
                .guardianPhone(entity.getGuardianPhone())
                .guardianEmail(entity.getGuardianEmail())
                .city(entity.getCity())
                .state(entity.getState())
                .biometrics(biometrics)
                .photos(photos)
                .status(entity.getStatus())
                .internalNotes(entity.getFeedbackNotes())
                .lgpdConsent(entity.getLgpdConsent())
                .lgpdConsentAt(entity.getLgpdConsentAt())
                .submittedAt(entity.getCreatedAt())
                .protocol(entity.getProtocol())
                .build();
    }

    private static String extractFileName(String url, String fallback) {
        if (url == null || url.isBlank()) return fallback;
        int lastSlash = url.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash < url.length() - 1) {
            String name = url.substring(lastSlash + 1);
            int queryParam = name.indexOf('?');
            return queryParam > 0 ? name.substring(0, queryParam) : name;
        }
        return fallback;
    }
}

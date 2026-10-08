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

    public static CandidateDetailResponseDto fromCandidate(Candidate candidate, String defaultStorageBaseUrl) {
        if (candidate == null) return null;

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

        List<CandidateMediaDto> photosList = new ArrayList<>();
        try {
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

                    photosList.add(CandidateMediaDto.builder()
                            .id(p.getId() != null ? p.getId().toString() : UUID.randomUUID().toString())
                            .url(url)
                            .type(type)
                            .fileName(p.getStoragePath())
                            .fileSizeBytes(null)
                            .build());
                }
            }
        } catch (Exception ignored) {}

        SubmissionStatus submissionStatus = null;
        if (candidate.getStatus() != null) {
            try {
                submissionStatus = SubmissionStatus.valueOf(candidate.getStatus().name());
            } catch (Exception ignored) {}
        }
        if (submissionStatus == null) submissionStatus = SubmissionStatus.PENDING;

        return CandidateDetailResponseDto.builder()
                .id(candidate.getId())
                .fullName(candidate.getFullName())
                .email(candidate.getEmail())
                .phone(candidate.getPhone())
                .instagram(candidate.getInstagramHandle())
                .birthDate(bd)
                .age(ageCalculated)
                .isMinor(minor)
                .guardianName(candidate.getGuardianName())
                .guardianPhone(candidate.getLegalGuardianContact())
                .guardianEmail(null)
                .city(candidate.getCity())
                .state(candidate.getState())
                .biometrics(BiometricsDto.builder()
                        .height(heightVal)
                        .bust(candidate.getBustChestCm())
                        .waist(candidate.getWaistCm())
                        .hips(candidate.getHipsCm())
                        .shoes(shoeSize)
                        .eyes(null)
                        .hair(null)
                        .build())
                .photos(photosList)
                .status(submissionStatus)
                .internalNotes(candidate.getInternalNotes())
                .lgpdConsent(candidate.getLgpdAccepted())
                .lgpdConsentAt(candidate.getLgpdAcceptedAt())
                .submittedAt(candidate.getCreatedAt())
                .protocol(candidate.getProtocol())
                .build();
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

        // ================================
        // 🔥 REGRA OBRIGATÓRIA DO DOD: NÃO USAR entity.getAge()
        // Calcular dinamicamente com Period.between(birthDate, hoje)
        // ================================
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
                .age(ageCalculated)
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

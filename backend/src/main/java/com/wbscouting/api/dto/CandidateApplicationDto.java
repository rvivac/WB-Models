package com.wbscouting.api.dto;

import com.wbscouting.api.entity.Candidate;
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
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
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

    @Builder.Default
    private List<CandidatePhotoDto> photos = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CandidatePhotoDto {
        private Short photoPosition;
        private Integer displayOrder;
        private String fileUrl;
        private String storagePath;
    }

    public static CandidateApplicationDto fromEntity(Candidate candidate) {
        if (candidate == null) {
            return null;
        }

        List<CandidatePhotoDto> photoDtos = candidate.getPhotos() != null
                ? candidate.getPhotos().stream()
                .map(p -> CandidatePhotoDto.builder()
                        .photoPosition(p.getPhotoPosition() != null ? p.getPhotoPosition() : (p.getDisplayOrder() != null ? p.getDisplayOrder().shortValue() : 1))
                        .displayOrder(p.getDisplayOrder())
                        .fileUrl(p.getFileUrl() != null ? p.getFileUrl() : p.getStoragePath())
                        .storagePath(p.getStoragePath())
                        .build())
                .collect(Collectors.toList())
                : new ArrayList<>();

        return CandidateApplicationDto.builder()
                .id(candidate.getId())
                .fullName(candidate.getFullName())
                .email(candidate.getEmail())
                .phone(candidate.getPhone())
                .birthDate(candidate.getBirthDate())
                .age(candidate.getAge())
                .guardianName(candidate.getGuardianName())
                .legalGuardianName(candidate.getLegalGuardianName())
                .legalGuardianContact(candidate.getLegalGuardianContact())
                .gender(candidate.getGender())
                .heightCm(candidate.getHeightCm())
                .city(candidate.getCity())
                .state(candidate.getState())
                .weightKg(candidate.getWeightKg())
                .bustChestCm(candidate.getBustChestCm())
                .waistCm(candidate.getWaistCm())
                .hipsCm(candidate.getHipsCm())
                .shoeSize(candidate.getShoeSize())
                .dressSize(candidate.getDressSize())
                .instagramHandle(candidate.getInstagramHandle())
                .portfolioUrl(candidate.getPortfolioUrl())
                .tiktokHandle(candidate.getTiktokHandle())
                .status(candidate.getStatus() != null ? candidate.getStatus().name() : null)
                .lgpdAccepted(candidate.getLgpdAccepted())
                .createdAt(candidate.getCreatedAt() != null ? candidate.getCreatedAt() : OffsetDateTime.now())
                .photos(photoDtos)
                .build();
    }
}

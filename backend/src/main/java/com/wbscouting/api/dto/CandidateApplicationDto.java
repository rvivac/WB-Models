package com.wbscouting.api.dto;

import com.wbscouting.api.entity.Candidate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
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
    private Integer age;
    private String guardianName;
    private String gender;
    private BigDecimal heightCm;
    private BigDecimal weightKg;
    private BigDecimal bustChestCm;
    private BigDecimal waistCm;
    private BigDecimal hipsCm;
    private String instagramHandle;
    private String tiktokHandle;
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
        private String fileUrl;
    }

    public static CandidateApplicationDto fromEntity(Candidate candidate) {
        if (candidate == null) {
            return null;
        }

        List<CandidatePhotoDto> photoDtos = candidate.getPhotos() != null
                ? candidate.getPhotos().stream()
                .map(p -> CandidatePhotoDto.builder()
                        .photoPosition(p.getPhotoPosition())
                        .fileUrl(p.getFileUrl())
                        .build())
                .collect(Collectors.toList())
                : new ArrayList<>();

        return CandidateApplicationDto.builder()
                .id(candidate.getId())
                .fullName(candidate.getFullName())
                .email(candidate.getEmail())
                .phone(candidate.getPhone())
                .age(candidate.getAge())
                .guardianName(candidate.getGuardianName())
                .gender(candidate.getGender())
                .heightCm(candidate.getHeightCm())
                .weightKg(candidate.getWeightKg())
                .bustChestCm(candidate.getBustChestCm())
                .waistCm(candidate.getWaistCm())
                .hipsCm(candidate.getHipsCm())
                .instagramHandle(candidate.getInstagramHandle())
                .tiktokHandle(candidate.getTiktokHandle())
                .lgpdAccepted(candidate.getLgpdAccepted())
                .createdAt(candidate.getCreatedAt() != null ? candidate.getCreatedAt() : OffsetDateTime.now())
                .photos(photoDtos)
                .build();
    }
}

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
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
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

    public static CandidateApplicationSummaryDto fromEntity(CandidateSubmission entity) {
        if (entity == null) {
            return null;
        }

        int polaroids = 0;
        if (entity.getFacePhotoUrl() != null && !entity.getFacePhotoUrl().isBlank()) polaroids++;
        if (entity.getProfilePhotoUrl() != null && !entity.getProfilePhotoUrl().isBlank()) polaroids++;
        if (entity.getFullBodyPhotoUrl() != null && !entity.getFullBodyPhotoUrl().isBlank()) polaroids++;

        Integer heightVal = null;
        if (entity.getHeight() != null) {
            if (entity.getHeight().compareTo(new BigDecimal("3.0")) < 0) {
                heightVal = entity.getHeight().multiply(new BigDecimal("100")).intValue();
            } else {
                heightVal = entity.getHeight().intValue();
            }
        }

        boolean minor = entity.getAge() != null ? entity.getAge() < 18 : false;

        return CandidateApplicationSummaryDto.builder()
                .id(entity.getId())
                .fullName(entity.getFullName())
                .email(entity.getEmail())
                .phone(entity.getPhone())
                .birthDate(entity.getBirthDate())
                .age(entity.getAge())
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
                .build();
    }
}

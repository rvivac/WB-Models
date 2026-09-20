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

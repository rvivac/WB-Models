package com.wbscouting.api.dto.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.wbscouting.api.entity.Model;
import com.wbscouting.api.enums.GenderType;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ModelResponseDto {

    private UUID id;
    private String name;
    private String artisticName;
    private String stageName;
    private String fullName;
    private GenderType gender;
    private Boolean isStar;
    private Boolean isFeaturedHome;
    private Boolean isActive;
    private String primaryPhotoUrl;
    private String instagramUrl;
    private LocalDate birthDate;
    private Integer height;
    private Integer heightCm;
    private String city;
    private String state;
    private String nationality;
    private String shoeSize;
    private String shoes;
    private BigDecimal bust;
    private BigDecimal bustChestCm;
    private BigDecimal waist;
    private BigDecimal waistCm;
    private BigDecimal hips;
    private BigDecimal hipsCm;
    private String hair;
    private String hairColor;
    private String eyes;
    private String eyesColor;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static ModelResponseDto fromEntity(Model model) {
        if (model == null) {
            return null;
        }
        return ModelResponseDto.builder()
                .id(model.getId())
                .name(model.getStageName())
                .artisticName(model.getStageName())
                .stageName(model.getStageName())
                .fullName(model.getStageName())
                .gender(model.getGender())
                .isStar(model.getIsStar())
                .isFeaturedHome(model.getIsFeaturedHome())
                .isActive(model.getIsActive())
                .primaryPhotoUrl(model.getPrimaryPhotoUrl())
                .instagramUrl(model.getInstagramUrl())
                .birthDate(model.getBirthDate())
                .height(model.getHeightCm())
                .heightCm(model.getHeightCm())
                .city(model.getCity())
                .nationality(model.getNationality())
                .shoeSize(model.getShoeSize())
                .shoes(model.getShoeSize())
                .bust(model.getBustChestCm())
                .bustChestCm(model.getBustChestCm())
                .waist(model.getWaistCm())
                .waistCm(model.getWaistCm())
                .hips(model.getHipsCm())
                .hipsCm(model.getHipsCm())
                .hair(model.getHairColor())
                .hairColor(model.getHairColor())
                .eyes(model.getEyesColor())
                .eyesColor(model.getEyesColor())
                .createdAt(model.getCreatedAt())
                .updatedAt(model.getUpdatedAt())
                .build();
    }
}

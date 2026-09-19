package com.wbscouting.api.dto.model;

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
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModelAdminResponseDto {

    private UUID id;
    private String stageName;
    private GenderType gender;
    private Boolean isStar;
    private Boolean isFeaturedHome;
    private Integer featuredOrder;
    private Boolean isActive;
    private String primaryPhotoUrl;
    private String instagramUrl;
    private LocalDate birthDate;
    private Integer heightCm;
    private String city;
    private String nationality;
    private String dressSize;
    private String shoeSize;
    private BigDecimal bustChestCm;
    private BigDecimal waistCm;
    private BigDecimal hipsCm;
    private String hairColor;
    private String eyesColor;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}

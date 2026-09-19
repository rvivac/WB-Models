package com.wbscouting.api.dto;

import com.wbscouting.api.enums.GenderType;
import com.wbscouting.api.enums.MediaType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class ModelDTO {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SummaryResponse {
        private UUID id;
        private String stageName;
        private GenderType gender;
        private Boolean isStar;
        private String primaryPhotoUrl;
        private Integer heightCm;
        private String dressSize;
        private String shoeSize;
        private String city;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DetailResponse {
        private UUID id;
        private String stageName;
        private GenderType gender;
        private Boolean isStar;
        private Boolean isFeaturedHome;
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
        private List<MediaResponse> media;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MediaResponse {
        private UUID id;
        private MediaType mediaType;
        private String fileUrl;
        private Integer displayOrder;
    }
}

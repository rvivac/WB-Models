package com.wbscouting.api.dto.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeaturedModelResponseDto {

    private UUID id;
    private String artisticName;
    private String stageName;
    private String category;
    private Integer height;
    private Integer heightCm;
    private Boolean isStar;
    private String coverPhotoUrl;
    private String primaryPhotoUrl;
    private Integer displayOrder;
    private Integer featuredOrder;
}

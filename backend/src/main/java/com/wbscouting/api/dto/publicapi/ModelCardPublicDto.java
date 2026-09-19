package com.wbscouting.api.dto.publicapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.wbscouting.api.enums.GenderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ModelCardPublicDto {
    private UUID id;
    private String stageName;
    private GenderType gender;
    private String coverImageUrl;
    private Integer heightCm;
    private String city;
    private Boolean isStar;
}

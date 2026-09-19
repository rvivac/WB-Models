package com.wbscouting.api.dto.publicapi;

import com.fasterxml.jackson.annotation.JsonInclude;
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
public class ModelMediaPublicItemDto {

    private UUID id;
    private String fileUrl;
    private Integer displayOrder;
    private Boolean isCover;
}

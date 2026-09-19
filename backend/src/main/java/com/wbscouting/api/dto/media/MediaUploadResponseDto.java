package com.wbscouting.api.dto.media;

import com.wbscouting.api.enums.MediaType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaUploadResponseDto {
    private UUID id;
    private UUID modelId;
    private MediaType mediaType;
    private String fileUrl;
    private String storagePath;
    private Integer displayOrder;
    private Boolean isCover;
    private OffsetDateTime createdAt;
}

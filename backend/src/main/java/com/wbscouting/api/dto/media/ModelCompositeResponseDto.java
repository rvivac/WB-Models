package com.wbscouting.api.dto.media;

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
public class ModelCompositeResponseDto {
    private UUID id;
    private String fileUrl;
    private String fileName;
    private String fileType; // "PDF" | "IMAGE"
    private Long fileSizeBytes;
    private OffsetDateTime updatedAt;
}

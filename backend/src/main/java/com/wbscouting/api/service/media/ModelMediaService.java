package com.wbscouting.api.service.media;

import com.wbscouting.api.dto.media.MediaReorderRequestDto;
import com.wbscouting.api.dto.media.MediaUploadResponseDto;
import com.wbscouting.api.enums.MediaType;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface ModelMediaService {

    MediaUploadResponseDto uploadMedia(UUID modelId, MediaType mediaType, boolean isCover, MultipartFile file);

    void deleteMedia(UUID modelId, UUID mediaId);

    void reorderMedia(UUID modelId, MediaReorderRequestDto reorderDto);

    void setCoverMedia(UUID modelId, UUID mediaId);

    List<MediaUploadResponseDto> listModelMedia(UUID modelId);
}

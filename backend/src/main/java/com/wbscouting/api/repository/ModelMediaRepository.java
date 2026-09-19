package com.wbscouting.api.repository;

import com.wbscouting.api.entity.ModelMedia;
import com.wbscouting.api.enums.MediaType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ModelMediaRepository extends JpaRepository<ModelMedia, UUID> {

    List<ModelMedia> findByModelIdOrderByMediaTypeAscDisplayOrderAsc(UUID modelId);

    Optional<ModelMedia> findByModelIdAndIsCoverTrue(UUID modelId);

    Optional<ModelMedia> findByIdAndModelId(UUID mediaId, UUID modelId);

    @Query("SELECT COALESCE(MAX(m.displayOrder), 0) + 1 FROM ModelMedia m WHERE m.model.id = :modelId AND m.mediaType = :mediaType")
    Integer findNextDisplayOrder(@Param("modelId") UUID modelId, @Param("mediaType") MediaType mediaType);

    Optional<ModelMedia> findFirstByModelIdAndMediaTypeOrderByCreatedAtAsc(UUID modelId, MediaType mediaType);

    List<ModelMedia> findByModelIdAndModelIsActiveTrueOrderByDisplayOrderAsc(UUID modelId);

    List<ModelMedia> findByModelIdAndMediaTypeAndIsActiveTrueOrderByDisplayOrderAsc(UUID modelId, MediaType mediaType);

    Optional<ModelMedia> findByModelIdAndMediaTypeAndIsActiveTrue(UUID modelId, MediaType mediaType);

    List<ModelMedia> findByModelIdInAndIsActiveTrueOrderByDisplayOrderAsc(List<UUID> modelIds);

    List<ModelMedia> findByModelIdOrderByDisplayOrderAsc(UUID modelId);
}



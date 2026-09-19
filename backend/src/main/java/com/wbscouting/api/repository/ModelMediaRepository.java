package com.wbscouting.api.repository;

import com.wbscouting.api.entity.ModelMedia;
import com.wbscouting.api.enums.MediaType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ModelMediaRepository extends JpaRepository<ModelMedia, UUID> {

    List<ModelMedia> findByModelIdAndIsActiveTrueOrderByDisplayOrderAsc(UUID modelId);

    List<ModelMedia> findByModelIdAndMediaTypeAndIsActiveTrueOrderByDisplayOrderAsc(UUID modelId, MediaType mediaType);

    Optional<ModelMedia> findByModelIdAndMediaTypeAndIsActiveTrue(UUID modelId, MediaType mediaType);
}

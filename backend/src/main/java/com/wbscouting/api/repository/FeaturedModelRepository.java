package com.wbscouting.api.repository;

import com.wbscouting.api.entity.FeaturedModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FeaturedModelRepository extends JpaRepository<FeaturedModel, UUID> {

    List<FeaturedModel> findAllByOrderByDisplayOrderAsc();

    void deleteByModelId(UUID modelId);
}

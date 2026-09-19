package com.wbscouting.api.repository;

import com.wbscouting.api.entity.Model;
import com.wbscouting.api.enums.GenderType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ModelRepository extends JpaRepository<Model, UUID>, JpaSpecificationExecutor<Model> {

    Page<Model> findByIsActiveTrue(Pageable pageable);

    Page<Model> findByGenderAndIsActiveTrue(GenderType gender, Pageable pageable);

    Page<Model> findByIsStarTrueAndIsActiveTrue(Pageable pageable);

    @Query("SELECT m FROM Model m WHERE m.isFeaturedHome = true AND m.isActive = true ORDER BY m.featuredOrder ASC")
    List<Model> findFeaturedHomeModels();
}

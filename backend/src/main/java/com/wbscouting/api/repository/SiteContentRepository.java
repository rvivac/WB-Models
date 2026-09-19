package com.wbscouting.api.repository;

import com.wbscouting.api.entity.SiteContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SiteContentRepository extends JpaRepository<SiteContent, UUID> {

    Optional<SiteContent> findBySectionKey(String sectionKey);

    boolean existsBySectionKey(String sectionKey);
}

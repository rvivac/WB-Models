package com.wbscouting.api.repository;

import com.wbscouting.api.entity.ApplyFaq;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ApplyFaqRepository extends JpaRepository<ApplyFaq, UUID> {
    List<ApplyFaq> findByIsActiveTrueOrderByDisplayOrderAsc();
    List<ApplyFaq> findAllByOrderByDisplayOrderAsc();
}

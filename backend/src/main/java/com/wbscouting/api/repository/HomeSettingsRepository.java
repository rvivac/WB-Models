package com.wbscouting.api.repository;

import com.wbscouting.api.entity.HomeSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface HomeSettingsRepository extends JpaRepository<HomeSettings, UUID> {

    Optional<HomeSettings> findFirstByOrderByUpdatedAtDesc();
}

package com.wbscouting.api.repository;

import com.wbscouting.api.entity.InstitutionalSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InstitutionalSettingRepository extends JpaRepository<InstitutionalSetting, String> {
    Optional<InstitutionalSetting> findBySettingKey(String settingKey);
}

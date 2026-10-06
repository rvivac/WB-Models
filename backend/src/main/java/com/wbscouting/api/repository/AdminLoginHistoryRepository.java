package com.wbscouting.api.repository;

import com.wbscouting.api.entity.AdminLoginHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AdminLoginHistoryRepository extends JpaRepository<AdminLoginHistory, UUID> {

    Optional<AdminLoginHistory> findFirstByAdminIdOrderByLoggedAtDesc(UUID adminId);

    @Query(value = "SELECT logged_at FROM admin_login_history WHERE admin_id = :adminId ORDER BY logged_at DESC LIMIT 1", nativeQuery = true)
    Optional<OffsetDateTime> findLastLoginDateByAdminId(@Param("adminId") UUID adminId);
}
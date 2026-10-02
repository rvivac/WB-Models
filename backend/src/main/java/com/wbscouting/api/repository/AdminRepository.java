package com.wbscouting.api.repository;

import com.wbscouting.api.entity.Admin;
import com.wbscouting.api.enums.AdminRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AdminRepository extends JpaRepository<Admin, UUID> {

    Optional<Admin> findByEmail(String email);

    Optional<Admin> findByEmailAndIsActiveTrue(String email);

    boolean existsByEmail(String email);

    Optional<Admin> findByPasswordResetToken(String token);

    long countByRoleAndIsActiveTrue(AdminRole role);

    long countByRoleInAndIsActiveTrue(Collection<AdminRole> roles);
}

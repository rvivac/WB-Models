package com.wbscouting.api.entity;

import com.wbscouting.api.enums.AdminRole;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "admins", schema = "public")
// TASK PASSO 3: Anotacoes Lombok completas exigidas
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Admin implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name", length = 150, nullable = false)
    private String name;

    @Column(name = "email", length = 255, nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", length = 255, nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, columnDefinition = "admin_role")
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Builder.Default
    private AdminRole role = AdminRole.SUPER_ADMIN;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "totp_secret", length = 128)
    private String totpSecret;

    @Column(name = "is_2fa_enabled", nullable = false)
    @Builder.Default
    private Boolean is2faEnabled = false;

    @Column(name = "backup_codes", columnDefinition = "text[]")
    private List<String> backupCodes;

    @Column(name = "must_change_password", nullable = false)
    @Builder.Default
    private Boolean mustChangePassword = false;

    @Column(name = "last_login_at")
    private OffsetDateTime lastLoginAt;

    @Column(name = "password_reset_token", length = 255)
    private String passwordResetToken;

    @Column(name = "password_reset_expires_at")
    private OffsetDateTime passwordResetExpiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return this.passwordHash;
    }

    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return Boolean.TRUE.equals(this.isActive);
    }

    // ============================================================
    // 🔥 GETTERS EXPLICITOS (Lombok @Getter NAO roda no MavenWrapper 3.6.3 Render)
    // ============================================================
    public UUID getId() { return this.id; }
    public String getName() { return this.name; }
    public String getEmail() { return this.email; }
    public String getPasswordHash() { return this.passwordHash; }
    public AdminRole getRole() { return this.role; }
    public Boolean getIsActive() { return this.isActive; }
    public String getTotpSecret() { return this.totpSecret; }
    public Boolean getIs2faEnabled() { return this.is2faEnabled; }
    public List<String> getBackupCodes() { return this.backupCodes; }
    public Boolean getMustChangePassword() { return this.mustChangePassword; }
    public OffsetDateTime getLastLoginAt() { return this.lastLoginAt; }
    public String getPasswordResetToken() { return this.passwordResetToken; }
    public OffsetDateTime getPasswordResetExpiresAt() { return this.passwordResetExpiresAt; }
    public OffsetDateTime getCreatedAt() { return this.createdAt; }
    public OffsetDateTime getUpdatedAt() { return this.updatedAt; }

    // ============================================================
    // 🔥 SETTERS EXPLICITOS complementares
    // ============================================================
    public void setId(UUID id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setRole(AdminRole role) { this.role = role; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public void setTotpSecret(String totpSecret) { this.totpSecret = totpSecret; }
    public void setIs2faEnabled(Boolean is2faEnabled) { this.is2faEnabled = is2faEnabled; }
    public void setBackupCodes(List<String> backupCodes) { this.backupCodes = backupCodes; }
    public void setMustChangePassword(Boolean mustChangePassword) { this.mustChangePassword = mustChangePassword; }
    public void setLastLoginAt(OffsetDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }
    public void setPasswordResetToken(String passwordResetToken) { this.passwordResetToken = passwordResetToken; }
    public void setPasswordResetExpiresAt(OffsetDateTime passwordResetExpiresAt) { this.passwordResetExpiresAt = passwordResetExpiresAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}

package com.wbscouting.api.entity;

import com.wbscouting.api.enums.AdminRole;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.OffsetDateTime;
import java.util.ArrayList;
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
    @Column(name = "role", length = 50, nullable = false)
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

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "admin_backup_codes", schema = "public", joinColumns = @JoinColumn(name = "admin_id"))
    @Column(name = "code", length = 255, nullable = false)
    @OrderColumn
    @Builder.Default
    private List<String> backupCodes = new ArrayList<>();

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

    // ============================================================
    // 🔥 BUILDER MANUAL (FALLBACK Lombok NoClassDefFoundError: Admin$AdminBuilder)
    //    Quando o annotation processor do Lombok falha no Maven Wrapper, essa
    //    inner class estática garante que Admin.builder() exista em bytecode.
    // ============================================================
    public static AdminBuilder builder() {
        return new AdminBuilder();
    }

    public static class AdminBuilder {
        private UUID id;
        private String name;
        private String email;
        private String passwordHash;
        private AdminRole role = AdminRole.SUPER_ADMIN;
        private Boolean isActive = true;
        private String totpSecret;
        private Boolean is2faEnabled = false;
        private java.util.List<String> backupCodes;
        private Boolean mustChangePassword = false;
        private OffsetDateTime lastLoginAt;
        private String passwordResetToken;
        private OffsetDateTime passwordResetExpiresAt;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;

        AdminBuilder() {}

        public AdminBuilder id(UUID id) { this.id = id; return this; }
        public AdminBuilder name(String name) { this.name = name; return this; }
        public AdminBuilder email(String email) { this.email = email; return this; }
        public AdminBuilder passwordHash(String passwordHash) { this.passwordHash = passwordHash; return this; }
        public AdminBuilder role(AdminRole role) { this.role = role; return this; }
        public AdminBuilder isActive(Boolean isActive) { this.isActive = isActive; return this; }
        public AdminBuilder totpSecret(String totpSecret) { this.totpSecret = totpSecret; return this; }
        public AdminBuilder is2faEnabled(Boolean is2faEnabled) { this.is2faEnabled = is2faEnabled; return this; }
        public AdminBuilder backupCodes(java.util.List<String> backupCodes) { this.backupCodes = backupCodes; return this; }
        public AdminBuilder mustChangePassword(Boolean mustChangePassword) { this.mustChangePassword = mustChangePassword; return this; }
        public AdminBuilder lastLoginAt(OffsetDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; return this; }
        public AdminBuilder passwordResetToken(String passwordResetToken) { this.passwordResetToken = passwordResetToken; return this; }
        public AdminBuilder passwordResetExpiresAt(OffsetDateTime passwordResetExpiresAt) { this.passwordResetExpiresAt = passwordResetExpiresAt; return this; }
        public AdminBuilder createdAt(OffsetDateTime createdAt) { this.createdAt = createdAt; return this; }
        public AdminBuilder updatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public Admin build() {
            Admin a = new Admin();
            a.setId(this.id);
            a.setName(this.name);
            a.setEmail(this.email);
            a.setPasswordHash(this.passwordHash);
            a.setRole(this.role != null ? this.role : AdminRole.SUPER_ADMIN);
            a.setIsActive(this.isActive != null ? this.isActive : true);
            a.setTotpSecret(this.totpSecret);
            a.setIs2faEnabled(this.is2faEnabled != null ? this.is2faEnabled : false);
            a.setBackupCodes(this.backupCodes);
            a.setMustChangePassword(this.mustChangePassword != null ? this.mustChangePassword : false);
            a.setLastLoginAt(this.lastLoginAt);
            a.setPasswordResetToken(this.passwordResetToken);
            a.setPasswordResetExpiresAt(this.passwordResetExpiresAt);
            a.setCreatedAt(this.createdAt);
            a.setUpdatedAt(this.updatedAt);
            return a;
        }

        @Override
        public String toString() {
            return "AdminBuilder(email=" + this.email + ", role=" + this.role + ")";
        }
    }
}

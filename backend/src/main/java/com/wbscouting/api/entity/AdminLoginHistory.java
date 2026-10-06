package com.wbscouting.api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "admin_login_history", indexes = {
        @Index(name = "idx_admin_login_history_admin_id", columnList = "admin_id"),
        @Index(name = "idx_admin_login_history_logged_at", columnList = "logged_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminLoginHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "admin_id", nullable = false)
    private UUID adminId;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "logged_at", nullable = false)
    @Builder.Default
    private OffsetDateTime loggedAt = OffsetDateTime.now();
}
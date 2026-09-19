package com.wbscouting.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.AssertTrue;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "candidates", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Candidate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "full_name", length = 200, nullable = false)
    private String fullName;

    @Column(name = "email", length = 255, nullable = false)
    private String email;

    @Column(name = "phone", length = 50, nullable = false)
    private String phone;

    @Column(name = "age", nullable = false)
    private Integer age;

    @Column(name = "guardian_name", length = 200)
    private String guardianName;

    @Column(name = "gender", length = 50, nullable = false)
    private String gender;

    @Column(name = "height_cm", precision = 5, scale = 2, nullable = false)
    private BigDecimal heightCm;

    @Column(name = "weight_kg", precision = 5, scale = 2)
    private BigDecimal weightKg;

    @Column(name = "bust_chest_cm", precision = 5, scale = 2)
    private BigDecimal bustChestCm;

    @Column(name = "waist_cm", precision = 5, scale = 2)
    private BigDecimal waistCm;

    @Column(name = "hips_cm", precision = 5, scale = 2)
    private BigDecimal hipsCm;

    @Column(name = "instagram_handle", length = 100)
    private String instagramHandle;

    @Column(name = "tiktok_handle", length = 100)
    private String tiktokHandle;

    @AssertTrue
    @Column(name = "lgpd_accepted", nullable = false)
    @Builder.Default
    private Boolean lgpdAccepted = true;

    @CreationTimestamp
    @Column(name = "lgpd_accepted_at", nullable = false, updatable = false)
    private OffsetDateTime lgpdAcceptedAt;

    @OneToMany(mappedBy = "candidate", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<CandidatePhoto> photos = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}

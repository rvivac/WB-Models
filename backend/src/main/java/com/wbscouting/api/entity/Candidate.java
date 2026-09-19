package com.wbscouting.api.entity;

import com.wbscouting.api.enums.CandidateStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.AssertTrue;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
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

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "age")
    private Integer age;

    @Column(name = "guardian_name", length = 200)
    private String guardianName;

    @Column(name = "legal_guardian_name", length = 200)
    private String legalGuardianName;

    @Column(name = "legal_guardian_contact", length = 50)
    private String legalGuardianContact;

    @Column(name = "gender", length = 50, nullable = false)
    private String gender;

    @Column(name = "height_cm", precision = 5, scale = 2, nullable = false)
    private BigDecimal heightCm;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state", length = 50)
    private String state;

    @Column(name = "weight_kg", precision = 5, scale = 2)
    private BigDecimal weightKg;

    @Column(name = "bust_chest_cm", precision = 5, scale = 2)
    private BigDecimal bustChestCm;

    @Column(name = "waist_cm", precision = 5, scale = 2)
    private BigDecimal waistCm;

    @Column(name = "hips_cm", precision = 5, scale = 2)
    private BigDecimal hipsCm;

    @Column(name = "shoe_size", length = 20)
    private String shoeSize;

    @Column(name = "dress_size", length = 20)
    private String dressSize;

    @Column(name = "instagram_handle", length = 100)
    private String instagramHandle;

    @Column(name = "portfolio_url", length = 255)
    private String portfolioUrl;

    @Column(name = "tiktok_handle", length = 100)
    private String tiktokHandle;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50, nullable = false)
    @Builder.Default
    private CandidateStatus status = CandidateStatus.PENDING;

    @Column(name = "internal_notes", columnDefinition = "TEXT")
    private String internalNotes;

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

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public String getGuardianName() {
        return guardianName != null ? guardianName : legalGuardianName;
    }
}

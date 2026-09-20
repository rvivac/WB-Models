package com.wbscouting.api.entity;

import com.wbscouting.api.enums.GenderType;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "models", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Model {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "stage_name", length = 150, nullable = false)
    private String stageName;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false, columnDefinition = "gender_type")
    @JdbcType(PostgreSQLEnumJdbcType.class)
    private GenderType gender;

    @Column(name = "is_star", nullable = false)
    @Builder.Default
    private Boolean isStar = false;

    @Column(name = "is_featured_home", nullable = false)
    @Builder.Default
    private Boolean isFeaturedHome = false;

    @Column(name = "featured_order")
    private Integer featuredOrder;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "primary_photo_url", columnDefinition = "TEXT")
    private String primaryPhotoUrl;

    @Column(name = "instagram_url", length = 255)
    private String instagramUrl;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Min(50)
    @Max(250)
    @Column(name = "height_cm")
    private Integer heightCm;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "nationality", length = 100)
    private String nationality;

    @Column(name = "dress_size", length = 20)
    private String dressSize;

    @Column(name = "shoe_size", length = 20)
    private String shoeSize;

    @Column(name = "bust_chest_cm", precision = 5, scale = 2)
    private BigDecimal bustChestCm;

    @Column(name = "waist_cm", precision = 5, scale = 2)
    private BigDecimal waistCm;

    @Column(name = "hips_cm", precision = 5, scale = 2)
    private BigDecimal hipsCm;

    @Column(name = "hair_color", length = 50)
    private String hairColor;

    @Column(name = "eyes_color", length = 50)
    private String eyesColor;

    @OneToMany(mappedBy = "model", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ModelMedia> media = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}

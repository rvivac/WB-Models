package com.wbscouting.api.entity;

import com.wbscouting.api.enums.GenderType;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
    @Column(name = "gender", length = 30, nullable = false)
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

    // ============================================================
    // 🔥 GETTERS EXPLICITOS (Lombok @Getter NAO processa no MavenWrapper 3.6.3 Render)
    // ============================================================
    public UUID getId() { return this.id; }
    public String getStageName() { return this.stageName; }
    public GenderType getGender() { return this.gender; }
    public Boolean getIsStar() { return this.isStar; }
    public Boolean getIsFeaturedHome() { return this.isFeaturedHome; }
    public Integer getFeaturedOrder() { return this.featuredOrder; }
    public Boolean getIsActive() { return this.isActive; }
    public String getPrimaryPhotoUrl() { return this.primaryPhotoUrl; }
    public String getInstagramUrl() { return this.instagramUrl; }
    public LocalDate getBirthDate() { return this.birthDate; }
    public Integer getHeightCm() { return this.heightCm; }
    public String getCity() { return this.city; }
    public String getNationality() { return this.nationality; }
    public String getDressSize() { return this.dressSize; }
    public String getShoeSize() { return this.shoeSize; }
    public BigDecimal getBustChestCm() { return this.bustChestCm; }
    public BigDecimal getWaistCm() { return this.waistCm; }
    public BigDecimal getHipsCm() { return this.hipsCm; }
    public String getHairColor() { return this.hairColor; }
    // Entity tem campo eyes_color (PLURAL) na tabela. Nome do getter confere com o campo.
    public String getEyesColor() { return this.eyesColor; }
    public List<ModelMedia> getMedia() { return this.media; }
    public OffsetDateTime getCreatedAt() { return this.createdAt; }
    public OffsetDateTime getUpdatedAt() { return this.updatedAt; }

    // ============================================================
    // 🔥 SETTERS EXPLICITOS complementares
    // ============================================================
    public void setId(UUID id) { this.id = id; }
    public void setStageName(String stageName) { this.stageName = stageName; }
    public void setGender(GenderType gender) { this.gender = gender; }
    public void setIsStar(Boolean isStar) { this.isStar = isStar; }
    public void setIsFeaturedHome(Boolean isFeaturedHome) { this.isFeaturedHome = isFeaturedHome; }
    public void setFeaturedOrder(Integer featuredOrder) { this.featuredOrder = featuredOrder; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public void setPrimaryPhotoUrl(String primaryPhotoUrl) { this.primaryPhotoUrl = primaryPhotoUrl; }
    public void setInstagramUrl(String instagramUrl) { this.instagramUrl = instagramUrl; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }
    public void setHeightCm(Integer heightCm) { this.heightCm = heightCm; }
    public void setCity(String city) { this.city = city; }
    public void setNationality(String nationality) { this.nationality = nationality; }
    public void setDressSize(String dressSize) { this.dressSize = dressSize; }
    public void setShoeSize(String shoeSize) { this.shoeSize = shoeSize; }
    public void setBustChestCm(BigDecimal bustChestCm) { this.bustChestCm = bustChestCm; }
    public void setWaistCm(BigDecimal waistCm) { this.waistCm = waistCm; }
    public void setHipsCm(BigDecimal hipsCm) { this.hipsCm = hipsCm; }
    public void setHairColor(String hairColor) { this.hairColor = hairColor; }
    public void setEyesColor(String eyesColor) { this.eyesColor = eyesColor; }
    public void setMedia(List<ModelMedia> media) { this.media = media; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}

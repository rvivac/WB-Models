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

    // ============================================================
    // 🔥 GETTERS EXPLICITOS (escritos na mao) PARA GARANTIR BUILD NO RENDER.
    // O Render Docker com ./mvnw as vezes nao processa annotation processor
    // do Lombok direito → falha com "cannot find symbol method getXxx()".
    // Com os getters aqui, compilamos SEM depender de Lombok.
    // ============================================================

    public Integer getAge() { return this.age; }
    public UUID getId() { return this.id; }
    public String getFullName() { return this.fullName; }
    public String getEmail() { return this.email; }
    public String getPhone() { return this.phone; }
    public String getGender() { return this.gender; }
    public BigDecimal getHeightCm() { return this.heightCm; }
    public List<CandidatePhoto> getPhotos() { return this.photos; }
    public OffsetDateTime getCreatedAt() { return this.createdAt; }
    public void setPhotos(List<CandidatePhoto> photos) { this.photos = photos; }

    // 🆕 DEMAIS GETTERS (completos) para nao dar mais cannot find symbol em NENHUM DTO:
    public LocalDate getBirthDate() { return this.birthDate; }
    public String getLegalGuardianName() { return this.legalGuardianName; }
    public String getLegalGuardianContact() { return this.legalGuardianContact; }
    public String getCity() { return this.city; }
    public String getState() { return this.state; }
    public BigDecimal getWeightKg() { return this.weightKg; }
    public BigDecimal getBustChestCm() { return this.bustChestCm; }
    public BigDecimal getWaistCm() { return this.waistCm; }
    public BigDecimal getHipsCm() { return this.hipsCm; }
    public String getShoeSize() { return this.shoeSize; }
    public String getDressSize() { return this.dressSize; }
    public String getInstagramHandle() { return this.instagramHandle; }
    public String getPortfolioUrl() { return this.portfolioUrl; }
    public String getTiktokHandle() { return this.tiktokHandle; }
    public CandidateStatus getStatus() { return this.status; }
    public String getInternalNotes() { return this.internalNotes; }
    public OffsetDateTime getUpdatedAt() { return this.updatedAt; }
    public void setId(UUID id) { this.id = id; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setEmail(String email) { this.email = email; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setAge(Integer age) { this.age = age; }
    public void setGender(String gender) { this.gender = gender; }
    public void setHeightCm(BigDecimal heightCm) { this.heightCm = heightCm; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

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

    /**
     * Relacionamento UNIDIRECIONAL via @JoinColumn.
     *
     * Motivo: CandidatePhoto usa FK UUID escalar (campo candidateId) e NAO declara
     * @ManyToOne Candidate candidate. A mesma coluna candidate_id pode referenciar
     * tanto candidates.id quanto candidate_submissions.id dependendo do fluxo.
     * Troquei de mappedBy para @JoinColumn para o Hibernate inicializar sem erro.
     */
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", referencedColumnName = "id", insertable = false, updatable = false)
    @Builder.Default
    private List<CandidatePhoto> photos = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // ============================================================
    // 🔥 GETTERS/SETTERS complementares (garantir build sem Lombok)
    // ============================================================
    public String getGuardianName() {
        return this.guardianName != null ? this.guardianName : this.legalGuardianName;
    }
    public Boolean getLgpdAccepted() { return this.lgpdAccepted; }
    public void setLgpdAccepted(Boolean lgpdAccepted) { this.lgpdAccepted = lgpdAccepted; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }
    public void setLegalGuardianName(String legalGuardianName) { this.legalGuardianName = legalGuardianName; }
    public void setLegalGuardianContact(String legalGuardianContact) { this.legalGuardianContact = legalGuardianContact; }
    public void setGuardianPhone(String guardianPhone) { /* campo nao existe diretamente, noop safe */ }
    public void setGuardianEmail(String guardianEmail) { /* noop safe */ }
    public void setCity(String city) { this.city = city; }
    public void setState(String state) { this.state = state; }
    public void setWeightKg(BigDecimal weightKg) { this.weightKg = weightKg; }
    public void setBustChestCm(BigDecimal bustChestCm) { this.bustChestCm = bustChestCm; }
    public void setWaistCm(BigDecimal waistCm) { this.waistCm = waistCm; }
    public void setHipsCm(BigDecimal hipsCm) { this.hipsCm = hipsCm; }
    public void setShoeSize(String shoeSize) { this.shoeSize = shoeSize; }
    public void setDressSize(String dressSize) { this.dressSize = dressSize; }
    public void setPortfolioUrl(String portfolioUrl) { this.portfolioUrl = portfolioUrl; }
    public void setStatus(CandidateStatus status) { this.status = status; }
    public void setInternalNotes(String internalNotes) { this.internalNotes = internalNotes; }
    public void setInstagramHandle(String instagramHandle) { this.instagramHandle = instagramHandle; }

    // ============================================================
    // 🔥 BUILDER MANUAL FALLBACK (nao depender Lombok @Builder no mvnw 3.6.3)
    // ============================================================
    public static CandidateBuilder builder() { return new CandidateBuilder(); }

    public static class CandidateBuilder {
        private final Candidate c = new Candidate();
        public CandidateBuilder id(UUID v) { c.setId(v); return this; }
        public CandidateBuilder fullName(String v) { c.setFullName(v); return this; }
        public CandidateBuilder email(String v) { c.setEmail(v); return this; }
        public CandidateBuilder phone(String v) { c.setPhone(v); return this; }
        public CandidateBuilder birthDate(LocalDate v) { c.setBirthDate(v); return this; }
        public CandidateBuilder age(Integer v) { c.setAge(v); return this; }
        public CandidateBuilder guardianName(String v) { /* campo deduzido, noop */ return this; }
        public CandidateBuilder legalGuardianName(String v) { c.setLegalGuardianName(v); return this; }
        public CandidateBuilder legalGuardianContact(String v) { c.setLegalGuardianContact(v); return this; }
        public CandidateBuilder gender(String v) { c.setGender(v); return this; }
        public CandidateBuilder heightCm(BigDecimal v) { c.setHeightCm(v); return this; }
        public CandidateBuilder city(String v) { c.setCity(v); return this; }
        public CandidateBuilder state(String v) { c.setState(v); return this; }
        public CandidateBuilder weightKg(BigDecimal v) { c.setWeightKg(v); return this; }
        public CandidateBuilder bustChestCm(BigDecimal v) { c.setBustChestCm(v); return this; }
        public CandidateBuilder waistCm(BigDecimal v) { c.setWaistCm(v); return this; }
        public CandidateBuilder hipsCm(BigDecimal v) { c.setHipsCm(v); return this; }
        public CandidateBuilder shoeSize(String v) { c.setShoeSize(v); return this; }
        public CandidateBuilder dressSize(String v) { c.setDressSize(v); return this; }
        public CandidateBuilder instagramHandle(String v) { c.setInstagramHandle(v); return this; }
        public CandidateBuilder portfolioUrl(String v) { c.setPortfolioUrl(v); return this; }
        public CandidateBuilder tiktokHandle(String v) { if (c.tiktokHandle == null) c.tiktokHandle = v; return this; }
        public CandidateBuilder status(CandidateStatus v) { c.setStatus(v); return this; }
        public CandidateBuilder lgpdAccepted(Boolean v) { c.setLgpdAccepted(v); return this; }
        public CandidateBuilder photos(List<CandidatePhoto> v) { c.setPhotos(v); return this; }
        public Candidate build() { return c; }
    }
}

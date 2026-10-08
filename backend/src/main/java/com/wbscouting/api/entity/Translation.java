package com.wbscouting.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "translations", uniqueConstraints = {
        @UniqueConstraint(name = "uk_translations_locale_key", columnNames = {"locale", "translation_key"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Translation {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "locale", nullable = false, length = 10)
    private String locale; // e.g. "pt", "en"

    @Column(name = "translation_key", nullable = false, length = 250)
    private String key;

    @Column(name = "translation_value", columnDefinition = "TEXT", nullable = false)
    private String value;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}

package com.wbscouting.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "contact_channels")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContactChannel {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "type", nullable = false, length = 50)
    private String type; // e.g. PHONE, WHATSAPP, EMAIL, INSTAGRAM, ADDRESS, OFFICE_HOURS, LINK

    @Column(name = "value", nullable = false, length = 500)
    private String value; // Phone number, email, address, URL

    @Column(name = "label", nullable = false, length = 100)
    private String label; // "WhatsApp Oficial", "E-mail Geral", "Instagram", etc.

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}

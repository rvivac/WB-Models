package com.wbscouting.api.event;

import com.wbscouting.api.entity.CandidateSubmission;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Evento publicado APENAS quando uma candidatura do Scouting Desk
 * é PROMOVIDA com SUCESSO para o Casting & Stars Oficial
 * E a ficha original do Scouting Desk foi APAGADA (migração concluída).
 *
 * Utilizado para:
 * 1. Auditoria / Logs / LGPD de dados migrados.
 * 2. Notificações internas para equipe de casting (webhook, email, etc).
 * 3. Integrações futuras (CRM, marketing, etc).
 */
@Getter
public class CandidatePromotedToCastingEvent extends ApplicationEvent {

    /** ID da candidatura original no Scouting Desk (APAGADO da tabela). */
    private final UUID submissionId;
    /** ID do Model NOVO criado no Casting. */
    private final UUID modelId;
    /** Protocolo WB-YYYYMMDD-XXXX (mantido para rastreio). */
    private final String protocol;
    /** Snapshot dos dados ANTES da exclusão (auditoria). */
    private final CandidateSubmission submissionSnapshot;
    /** Booker/admin que promoveu (responsável). */
    private final String reviewedBy;
    /** Data/hora exata da promoção e remoção do Scouting Desk. */
    private final OffsetDateTime promotedAt;
    /** Quantidade de fotos (3 normalmente: rosto, perfil, corpo inteiro) migradas para ModelMedia. */
    private final int migratedPhotosCount;
    /** Nome completo do perfil migrado. */
    private final String fullName;

    public CandidatePromotedToCastingEvent(
            Object source,
            CandidateSubmission submissionBeforeDelete,
            UUID modelId,
            String reviewedBy,
            int migratedPhotosCount
    ) {
        super(source);
        this.submissionId = submissionBeforeDelete.getId();
        this.modelId = modelId;
        this.protocol = submissionBeforeDelete.getProtocol();
        this.submissionSnapshot = submissionBeforeDelete;
        this.reviewedBy = reviewedBy;
        this.promotedAt = OffsetDateTime.now();
        this.migratedPhotosCount = migratedPhotosCount;
        this.fullName = submissionBeforeDelete.getFullName();
    }
}
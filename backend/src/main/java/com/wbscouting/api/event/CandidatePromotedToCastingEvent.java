package com.wbscouting.api.event;

import com.wbscouting.api.entity.CandidateSubmission;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
public class CandidatePromotedToCastingEvent extends ApplicationEvent {

    private final UUID submissionId;
    private final UUID modelId;
    private final String protocol;
    private final CandidateSubmission submissionSnapshot;
    private final String reviewedBy;
    private final OffsetDateTime promotedAt;
    private final int migratedPhotosCount;
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
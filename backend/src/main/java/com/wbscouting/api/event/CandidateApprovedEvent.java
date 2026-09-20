package com.wbscouting.api.event;

import com.wbscouting.api.entity.CandidateSubmission;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

@Getter
public class CandidateApprovedEvent extends ApplicationEvent {

    private final UUID submissionId;
    private final CandidateSubmission submission;
    private final String reviewedBy;

    public CandidateApprovedEvent(Object source, CandidateSubmission submission, String reviewedBy) {
        super(source);
        this.submission = submission;
        this.submissionId = submission.getId();
        this.reviewedBy = reviewedBy;
    }
}

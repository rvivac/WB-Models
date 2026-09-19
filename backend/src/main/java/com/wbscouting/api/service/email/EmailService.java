package com.wbscouting.api.service.email;

import com.wbscouting.api.dto.CandidateApplicationDto;

public interface EmailService {

    void sendPasswordResetEmail(String recipientEmail, String recipientName, String resetToken);

    void sendCandidateApplicationNotification(String recipientEmail, CandidateApplicationDto candidateData);
}

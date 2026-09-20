package com.wbscouting.api.service.submission;

import com.wbscouting.api.dto.CandidateSubmissionRequestDto;
import com.wbscouting.api.dto.CandidateSubmissionResponseDto;
import org.springframework.web.multipart.MultipartFile;

public interface CandidateSubmissionService {

    CandidateSubmissionResponseDto submit(
            CandidateSubmissionRequestDto request,
            MultipartFile facePhoto,
            MultipartFile profilePhoto,
            MultipartFile fullBodyPhoto
    );
}

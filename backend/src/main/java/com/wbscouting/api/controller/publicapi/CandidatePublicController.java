package com.wbscouting.api.controller.publicapi;

import com.wbscouting.api.dto.CandidateSubmissionRequestDto;
import com.wbscouting.api.dto.CandidateSubmissionResponseDto;
import com.wbscouting.api.service.submission.CandidateSubmissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping({"/submissions", "/api/v1/submissions"})
@RequiredArgsConstructor
public class CandidatePublicController {

    private final CandidateSubmissionService submissionService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CandidateSubmissionResponseDto> submit(
            @RequestPart("data") @Valid CandidateSubmissionRequestDto data,
            @RequestPart("facePhoto") MultipartFile facePhoto,
            @RequestPart("profilePhoto") MultipartFile profilePhoto,
            @RequestPart("fullBodyPhoto") MultipartFile fullBodyPhoto
    ) {
        log.info("Recebida requisição de candidatura pública para: {}", data.getFullName());
        CandidateSubmissionResponseDto response = submissionService.submit(
                data,
                facePhoto,
                profilePhoto,
                fullBodyPhoto
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}

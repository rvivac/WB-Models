package com.wbscouting.api.controller.publicapi;

import com.wbscouting.api.dto.candidate.CandidateApplyRequestDto;
import com.wbscouting.api.dto.candidate.CandidateApplyResponseDto;
import com.wbscouting.api.service.candidate.CandidateApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping({"/public/candidates", "/api/v1/public/candidates"})
@RequiredArgsConstructor
public class PublicCandidateController {

    private final CandidateApplicationService candidateApplicationService;

    @PostMapping(value = "/apply", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CandidateApplyResponseDto> apply(
            @RequestPart("candidate") @Valid CandidateApplyRequestDto candidateDto,
            @RequestPart("photos") List<MultipartFile> photos
    ) {
        log.info("Recebida requisição multipart pública de candidatura para: {}", candidateDto.getFullName());
        CandidateApplyResponseDto response = candidateApplicationService.apply(candidateDto, photos);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}

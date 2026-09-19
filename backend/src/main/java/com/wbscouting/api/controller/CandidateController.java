package com.wbscouting.api.controller;

import com.wbscouting.api.dto.CandidateDTO;
import com.wbscouting.api.service.CandidateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class CandidateController {

    private final CandidateService candidateService;

    // Endpoint público para envio de ficha de novos talentos (Quero ser modelo)
    @PostMapping("/candidates")
    public ResponseEntity<CandidateDTO.Response> submitApplication(
            @Valid @RequestBody CandidateDTO.ApplicationRequest request) {
        CandidateDTO.Response response = candidateService.submitApplication(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Endpoint restrito aos administradores do Backoffice
    @GetMapping("/admin/candidates")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CONTENT_ADMIN')")
    public ResponseEntity<Page<CandidateDTO.Response>> listCandidates(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(candidateService.listCandidates(pageable));
    }
}

package com.wbscouting.api.service;

import com.wbscouting.api.config.MailProperties;
import com.wbscouting.api.dto.CandidateApplicationDto;
import com.wbscouting.api.dto.CandidateDTO;
import com.wbscouting.api.entity.Candidate;
import com.wbscouting.api.entity.CandidatePhoto;
import com.wbscouting.api.repository.CandidateRepository;
import com.wbscouting.api.service.email.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CandidateService {

    private final CandidateRepository candidateRepository;
    private final EmailService emailService;
    private final MailProperties mailProperties;

    @Transactional
    public CandidateDTO.Response submitApplication(CandidateDTO.ApplicationRequest request) {
        Candidate candidate = Candidate.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .age(request.getAge())
                .guardianName(request.getGuardianName())
                .gender(request.getGender())
                .heightCm(request.getHeightCm())
                .weightKg(request.getWeightKg())
                .bustChestCm(request.getBustChestCm())
                .waistCm(request.getWaistCm())
                .hipsCm(request.getHipsCm())
                .instagramHandle(request.getInstagramHandle())
                .tiktokHandle(request.getTiktokHandle())
                .lgpdAccepted(request.getLgpdAccepted())
                .build();

        if (request.getPhotos() != null && !request.getPhotos().isEmpty()) {
            List<CandidatePhoto> photos = request.getPhotos().stream()
                    .map(p -> CandidatePhoto.builder()
                            .candidate(candidate)
                            .photoPosition(p.getPhotoPosition())
                            .fileUrl(p.getFileUrl())
                            .filePath(p.getFilePath())
                            .build())
                    .collect(Collectors.toList());
            candidate.setPhotos(photos);
        }

        Candidate saved = candidateRepository.save(candidate);

        // Despacho assíncrono de notificações de e-mail (não bloqueia a resposta HTTP)
        CandidateApplicationDto applicationDto = CandidateApplicationDto.fromEntity(saved);
        if (mailProperties.getAgencyNotificationEmail() != null && !mailProperties.getAgencyNotificationEmail().isBlank()) {
            emailService.sendCandidateApplicationNotification(mailProperties.getAgencyNotificationEmail(), applicationDto);
        }
        if (saved.getEmail() != null && !saved.getEmail().isBlank()) {
            emailService.sendCandidateApplicationNotification(saved.getEmail(), applicationDto);
        }

        return CandidateDTO.Response.builder()
                .id(saved.getId())
                .fullName(saved.getFullName())
                .email(saved.getEmail())
                .phone(saved.getPhone())
                .age(saved.getAge())
                .gender(saved.getGender())
                .heightCm(saved.getHeightCm())
                .createdAt(saved.getCreatedAt())
                .photoCount(saved.getPhotos().size())
                .build();
    }

    @Transactional(readOnly = true)
    public Page<CandidateDTO.Response> listCandidates(Pageable pageable) {
        return candidateRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(c -> CandidateDTO.Response.builder()
                        .id(c.getId())
                        .fullName(c.getFullName())
                        .email(c.getEmail())
                        .phone(c.getPhone())
                        .age(c.getAge())
                        .gender(c.getGender())
                        .heightCm(c.getHeightCm())
                        .createdAt(c.getCreatedAt())
                        .photoCount(c.getPhotos().size())
                        .build());
    }
}

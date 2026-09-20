package com.wbscouting.api.service.submission;

import com.wbscouting.api.dto.CandidateSubmissionResponseDto;
import com.wbscouting.api.dto.UpdateSubmissionStatusDto;
import com.wbscouting.api.dto.submission.CandidateStatusUpdateDto;
import com.wbscouting.api.entity.CandidateSubmission;
import com.wbscouting.api.entity.Model;
import com.wbscouting.api.entity.ModelMedia;
import com.wbscouting.api.enums.GenderType;
import com.wbscouting.api.enums.MediaType;
import com.wbscouting.api.enums.SubmissionGender;
import com.wbscouting.api.enums.SubmissionStatus;
import com.wbscouting.api.event.CandidateApprovedEvent;
import com.wbscouting.api.exception.BusinessException;
import com.wbscouting.api.exception.DuplicatePromotionException;
import com.wbscouting.api.exception.ResourceNotFoundException;
import com.wbscouting.api.repository.CandidateSubmissionRepository;
import com.wbscouting.api.repository.ModelMediaRepository;
import com.wbscouting.api.repository.ModelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CandidateSubmissionAdminServiceImpl implements CandidateSubmissionAdminService {

    private final CandidateSubmissionRepository repository;
    private final ModelRepository modelRepository;
    private final ModelMediaRepository modelMediaRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(readOnly = true)
    public Page<CandidateSubmissionResponseDto> listSubmissions(Specification<CandidateSubmission> spec, Pageable pageable) {
        log.debug("Listando candidaturas com paginação e filtros dinâmicos. Pageable: {}", pageable);
        return repository.findAll(spec, pageable)
                .map(CandidateSubmissionResponseDto::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public CandidateSubmissionResponseDto getSubmissionById(UUID id) {
        log.debug("Buscando detalhes da candidatura ID: {}", id);
        CandidateSubmission submission = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidatura não encontrada com ID: " + id));
        return CandidateSubmissionResponseDto.fromEntity(submission);
    }

    @Override
    @Transactional
    public CandidateSubmissionResponseDto updateSubmissionStatus(UUID id, CandidateStatusUpdateDto updateDto, String reviewer) {
        UpdateSubmissionStatusDto dto = UpdateSubmissionStatusDto.builder()
                .status(updateDto.getStatus())
                .adminNotes(updateDto.getFeedbackNotes())
                .build();
        return updateSubmissionStatus(id, dto, reviewer);
    }

    @Override
    @Transactional
    public CandidateSubmissionResponseDto updateSubmissionStatus(UUID id, UpdateSubmissionStatusDto updateDto, String reviewer) {
        log.info("Atualizando status da candidatura ID: {} para {} por {}", id, updateDto.getStatus(), reviewer);

        CandidateSubmission submission = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidatura não encontrada com ID: " + id));

        submission.setStatus(updateDto.getStatus());
        submission.setReviewedBy(StringUtils.hasText(reviewer) ? reviewer : "Sistema");
        submission.setReviewedAt(OffsetDateTime.now());

        if (StringUtils.hasText(updateDto.getAdminNotes())) {
            submission.setFeedbackNotes(updateDto.getAdminNotes().trim());
        }

        CandidateSubmission saved = repository.save(submission);

        if (saved.getStatus() == SubmissionStatus.APPROVED) {
            log.info("Disparando CandidateApprovedEvent para a candidatura ID: {}", saved.getId());
            eventPublisher.publishEvent(new CandidateApprovedEvent(this, saved, reviewer));
        }

        return CandidateSubmissionResponseDto.fromEntity(saved);
    }

    @Override
    @Transactional
    public CandidateSubmissionResponseDto promoteToModel(UUID submissionId, String reviewer, Boolean activateImmediately) {
        log.info("Iniciando promoção da candidatura ID: {} para Modelo Oficial por {}", submissionId, reviewer);

        CandidateSubmission submission = repository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidatura não encontrada com ID: " + submissionId));

        if (submission.getConvertedToModelId() != null) {
            throw new DuplicatePromotionException("Esta candidatura já foi promovida ao elenco oficial de modelos (Modelo ID: " + submission.getConvertedToModelId() + ").");
        }

        // Determina gênero compatível com catálogo de modelos
        GenderType gender = (submission.getGender() == SubmissionGender.FEMALE) ? GenderType.FEMALE : GenderType.MALE;

        // Normalização de altura em cm (ex.: 1.78 -> 178)
        Integer heightCm = null;
        if (submission.getHeight() != null) {
            if (submission.getHeight().compareTo(BigDecimal.valueOf(3)) < 0) {
                heightCm = submission.getHeight().multiply(BigDecimal.valueOf(100)).intValue();
            } else {
                heightCm = submission.getHeight().intValue();
            }
        }

        Model model = Model.builder()
                .stageName(submission.getFullName().trim())
                .gender(gender)
                .isStar(false)
                .isFeaturedHome(false)
                .isActive(Boolean.TRUE.equals(activateImmediately))
                .primaryPhotoUrl(submission.getFacePhotoUrl())
                .instagramUrl(submission.getInstagramHandle())
                .birthDate(submission.getBirthDate())
                .heightCm(heightCm)
                .city(submission.getCity())
                .nationality("Brasileira")
                .shoeSize(submission.getShoeSize() != null ? submission.getShoeSize().toString() : null)
                .bustChestCm(submission.getBust())
                .waistCm(submission.getWaist())
                .hipsCm(submission.getHips())
                .hairColor(submission.getHairColor())
                .eyesColor(submission.getEyeColor())
                .build();

        Model savedModel = modelRepository.save(model);

        // Criação dos 3 registros de mídia associados ao modelo
        if (StringUtils.hasText(submission.getFacePhotoUrl())) {
            ModelMedia faceMedia = ModelMedia.builder()
                    .model(savedModel)
                    .mediaType(MediaType.BOOK)
                    .fileUrl(submission.getFacePhotoUrl())
                    .storagePath(submission.getFacePhotoUrl())
                    .displayOrder(1)
                    .isCover(true)
                    .isActive(true)
                    .build();
            modelMediaRepository.save(faceMedia);
        }

        if (StringUtils.hasText(submission.getProfilePhotoUrl())) {
            ModelMedia profileMedia = ModelMedia.builder()
                    .model(savedModel)
                    .mediaType(MediaType.POLAROID)
                    .fileUrl(submission.getProfilePhotoUrl())
                    .storagePath(submission.getProfilePhotoUrl())
                    .displayOrder(2)
                    .isCover(false)
                    .isActive(true)
                    .build();
            modelMediaRepository.save(profileMedia);
        }

        if (StringUtils.hasText(submission.getFullBodyPhotoUrl())) {
            ModelMedia fullBodyMedia = ModelMedia.builder()
                    .model(savedModel)
                    .mediaType(MediaType.POLAROID)
                    .fileUrl(submission.getFullBodyPhotoUrl())
                    .storagePath(submission.getFullBodyPhotoUrl())
                    .displayOrder(3)
                    .isCover(false)
                    .isActive(true)
                    .build();
            modelMediaRepository.save(fullBodyMedia);
        }

        // Atualização da candidatura para APPROVED e vínculo do ID do modelo
        submission.setStatus(SubmissionStatus.APPROVED);
        submission.setConvertedToModelId(savedModel.getId());
        submission.setReviewedBy(StringUtils.hasText(reviewer) ? reviewer : "Sistema");
        submission.setReviewedAt(OffsetDateTime.now());
        if (!StringUtils.hasText(submission.getFeedbackNotes())) {
            submission.setFeedbackNotes("Promovido para o elenco oficial de modelos em " + LocalDate.now());
        }

        CandidateSubmission savedSubmission = repository.save(submission);

        eventPublisher.publishEvent(new CandidateApprovedEvent(this, savedSubmission, reviewer));

        log.info("Candidatura ID: {} promovida com sucesso ao modelo ID: {} ({})",
                submissionId, savedModel.getId(), savedModel.getStageName());

        return CandidateSubmissionResponseDto.fromEntity(savedSubmission);
    }
}

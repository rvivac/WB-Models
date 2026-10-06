package com.wbscouting.api.service.submission;

import com.wbscouting.api.dto.CandidateSubmissionResponseDto;
import com.wbscouting.api.dto.UpdateSubmissionStatusDto;
import com.wbscouting.api.dto.model.ModelResponseDto;
import com.wbscouting.api.dto.submission.CandidateStatusUpdateDto;
import com.wbscouting.api.entity.CandidatePhoto;
import com.wbscouting.api.entity.CandidateSubmission;
import com.wbscouting.api.entity.Model;
import com.wbscouting.api.entity.ModelMedia;
import com.wbscouting.api.enums.GenderType;
import com.wbscouting.api.enums.MediaType;
import com.wbscouting.api.enums.SubmissionGender;
import com.wbscouting.api.enums.SubmissionStatus;
import com.wbscouting.api.event.CandidateApprovedEvent;
import com.wbscouting.api.event.CandidatePromotedToCastingEvent;
import com.wbscouting.api.exception.BusinessException;
import com.wbscouting.api.exception.DuplicatePromotionException;
import com.wbscouting.api.exception.ResourceNotFoundException;
import com.wbscouting.api.repository.CandidatePhotoRepository;
import com.wbscouting.api.repository.CandidateSubmissionRepository;
import com.wbscouting.api.repository.ModelMediaRepository;
import com.wbscouting.api.repository.ModelRepository;
import com.wbscouting.api.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CandidateSubmissionAdminServiceImpl implements CandidateSubmissionAdminService {

    private final CandidateSubmissionRepository repository;
    private final ModelRepository modelRepository;
    private final ModelMediaRepository modelMediaRepository;
    private final CandidatePhotoRepository candidatePhotoRepository;
    private final StorageService storageService;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${supabase.buckets.candidates-uploads:candidates-uploads}")
    private String candidatesBucketName;

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
    public ModelResponseDto promoteCandidateToModel(UUID submissionId) {
        return promoteCandidateToModel(submissionId, "Scouting Desk / Admin", true);
    }

    @Override
    @Transactional
    public ModelResponseDto promoteCandidateToModel(UUID submissionId, String reviewer, Boolean activateImmediately) {
        log.info("Iniciando promoção da candidatura ID: {} para Modelo Oficial por {}", submissionId, reviewer);

        CandidateSubmission submission = repository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidatura não encontrada com ID: " + submissionId));

        // Regra ADM-018: PROMOÇÃO SÓ É PERMITIDA A PARTIR DE STATUS APPROVED
        if (submission.getStatus() != SubmissionStatus.APPROVED) {
            throw new BusinessException(
                    "Apenas candidaturas APROVADAS podem ser promovidas ao elenco oficial. " +
                    "Status atual: '" + submission.getStatus() + "'. Utilize a ação Aprovar primeiro."
            );
        }

        if (submission.getConvertedToModelId() != null || submission.getStatus() == SubmissionStatus.PROMOTED) {
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
                .isActive(activateImmediately == null || Boolean.TRUE.equals(activateImmediately))
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

        // ================================
        // Criação dos registros de mídia associados ao modelo
        // Contador migratedPhotosCount exigido pela assinatura do evento.
        // ================================
        int migratedPhotosCount = 0;

        if (StringUtils.hasText(submission.getFacePhotoUrl())) {
            ModelMedia faceMedia = ModelMedia.builder()
                    .model(savedModel)
                    .mediaType(MediaType.BOOK)
                    .fileUrl(submission.getFacePhotoUrl())
                    .filePath(submission.getFacePhotoUrl())
                    .displayOrder(1)
                    .isCover(true)
                    .isActive(true)
                    .build();
            modelMediaRepository.save(faceMedia);
            migratedPhotosCount++;
        }

        if (StringUtils.hasText(submission.getProfilePhotoUrl())) {
            ModelMedia profileMedia = ModelMedia.builder()
                    .model(savedModel)
                    .mediaType(MediaType.POLAROID)
                    .fileUrl(submission.getProfilePhotoUrl())
                    .filePath(submission.getProfilePhotoUrl())
                    .displayOrder(2)
                    .isCover(false)
                    .isActive(true)
                    .build();
            modelMediaRepository.save(profileMedia);
            migratedPhotosCount++;
        }

        if (StringUtils.hasText(submission.getFullBodyPhotoUrl())) {
            ModelMedia fullBodyMedia = ModelMedia.builder()
                    .model(savedModel)
                    .mediaType(MediaType.POLAROID)
                    .fileUrl(submission.getFullBodyPhotoUrl())
                    .filePath(submission.getFullBodyPhotoUrl())
                    .displayOrder(3)
                    .isCover(false)
                    .isActive(true)
                    .build();
            modelMediaRepository.save(fullBodyMedia);
            migratedPhotosCount++;
        }

        // Atualização da candidatura para PROMOTED (sai imediatamente da fila de triagem)
        submission.setStatus(SubmissionStatus.PROMOTED);
        submission.setConvertedToModelId(savedModel.getId());
        submission.setReviewedBy(StringUtils.hasText(reviewer) ? reviewer : "Sistema");
        submission.setReviewedAt(OffsetDateTime.now());
        if (!StringUtils.hasText(submission.getFeedbackNotes())) {
            submission.setFeedbackNotes("Promovido para o elenco oficial de modelos em " + LocalDate.now());
        }

        CandidateSubmission savedSubmission = repository.save(submission);

        // Assinatura correta do evento: (source, submission, UUID modelId, reviewedBy, int migratedPhotosCount)
        eventPublisher.publishEvent(new CandidatePromotedToCastingEvent(
                this,
                savedSubmission,
                savedModel.getId(),
                reviewer,
                migratedPhotosCount
        ));

        log.info("Candidatura ID: {} promovida com sucesso ao modelo ID: {} ({})",
                submissionId, savedModel.getId(), savedModel.getStageName());

        return ModelResponseDto.fromEntity(savedModel);
    }

    @Override
    @Transactional
    public CandidateSubmissionResponseDto promoteToModel(UUID submissionId, String reviewer, Boolean activateImmediately) {
        promoteCandidateToModel(submissionId, reviewer, activateImmediately);
        CandidateSubmission updated = repository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidatura não encontrada com ID: " + submissionId));
        return CandidateSubmissionResponseDto.fromEntity(updated);
    }

    @Override
    @Transactional
    public void deleteSubmission(UUID submissionId, String operator) {
        log.warn("[HARD-DELETE] Operador '{}' solicitou exclusão definitiva da candidatura ID: {}", operator, submissionId);

        CandidateSubmission submission = repository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidatura não encontrada com ID: " + submissionId));

        // ================================
        // REGRA ADM-018: SÓ EXCLUI SE DECLINED
        // ================================
        // Também aceitamos REJECTED como alias LEGADO para não travar registros antigos.
        SubmissionStatus status = submission.getStatus();
        boolean isDeclinedOrLegacyRejected =
                status == SubmissionStatus.DECLINED || status == SubmissionStatus.REJECTED;

        if (!isDeclinedOrLegacyRejected) {
            throw new BusinessException(
                    "Apenas candidaturas com status DECLINADO podem ser excluídas definitivamente. " +
                    "Status atual: '" + status + "'. Decline a candidatura primeiro."
            );
        }

        // ================================
        // PASSO 1: Remover arquivos físicos no Supabase Storage
        // ================================
        try {
            List<CandidatePhoto> photos = candidatePhotoRepository
                    .findByCandidateIdOrderByDisplayOrderAsc(submissionId);

            for (CandidatePhoto photo : photos) {
                String path = resolveStoragePath(photo);
                if (StringUtils.hasText(path)) {
                    try {
                        storageService.deleteFile(candidatesBucketName, path);
                        log.info("[HARD-DELETE][STORAGE] Arquivo removido do bucket {}: {}", candidatesBucketName, path);
                    } catch (Exception ex) {
                        log.warn("[HARD-DELETE][STORAGE] Falha ao remover arquivo {} (continuando por causa do cascade DB). Motivo: {}",
                                path, ex.getMessage());
                    }
                }
            }

            // Também tenta apagar as 3 URLs de foto canonizadas na submission
            // (Algumas submissões antigas não tem linha em candidate_photos e só usam face/profile/body URL)
            tryDeleteRawPhotoUrl(submission.getFacePhotoUrl(), "face");
            tryDeleteRawPhotoUrl(submission.getProfilePhotoUrl(), "profile");
            tryDeleteRawPhotoUrl(submission.getFullBodyPhotoUrl(), "body");

        } catch (Exception storageEx) {
            log.error("[HARD-DELETE][STORAGE] Erro na rotina de exclusão de arquivos. Continuando para o cascade do DB. Motivo: {}",
                    storageEx.getMessage(), storageEx);
        }

        // ================================
        // PASSO 2: Exclui a submission do banco
        //         (ON DELETE CASCADE remove automaticamente as linhas em candidate_photos)
        // ================================
        repository.delete(submission);

        log.info("[HARD-DELETE][COMMIT] Candidatura ID: {} (protocolo={}) excluída definitivamente por '{}'. Fotos em cascade e arquivos de storage removidos.",
                submission.getId(), submission.getProtocol(), operator);
    }

    // ============================================================
    // Helpers internos
    // ============================================================
    private static String resolveStoragePath(CandidatePhoto photo) {
        if (photo == null) return null;
        // Ordem de precedência: storagePath > filePath > fileUrl (tirando o prefixo público)
        if (StringUtils.hasText(photo.getStoragePath())) return photo.getStoragePath().trim();
        if (StringUtils.hasText(photo.getFilePath())) return photo.getFilePath().trim();
        if (StringUtils.hasText(photo.getFileUrl())) return extractStoragePathFromUrl(photo.getFileUrl());
        return null;
    }

    private static String extractStoragePathFromUrl(String url) {
        if (!StringUtils.hasText(url)) return null;
        int p = url.indexOf("/storage/v1/object/public/");
        if (p > 0) {
            String tail = url.substring(p + "/storage/v1/object/public/".length());
            int firstSlash = tail.indexOf('/');
            if (firstSlash > 0 && firstSlash < tail.length() - 1) return tail.substring(firstSlash + 1);
        }
        // Fallback: retorna a URL mesmo (o StorageService normaliza internamente quando não encontra prefixo)
        return url;
    }

    private void tryDeleteRawPhotoUrl(String url, String slotName) {
        if (!StringUtils.hasText(url)) return;
        String path = extractStoragePathFromUrl(url);
        if (path == null || path.equals(url)) return; // Não extraiu nada, evita duplicidade
        try {
            storageService.deleteFile(candidatesBucketName, path);
            log.debug("[HARD-DELETE][STORAGE][{}] Extra URL removida: {}", slotName, path);
        } catch (Exception ex) {
            log.debug("[HARD-DELETE][STORAGE][{}] Nada removido (provável duplicidade vs candidate_photos): {}", slotName, ex.getMessage());
        }
    }
}

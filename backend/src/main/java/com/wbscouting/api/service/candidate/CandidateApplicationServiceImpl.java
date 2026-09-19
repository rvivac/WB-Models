package com.wbscouting.api.service.candidate;

import com.wbscouting.api.config.MailProperties;
import com.wbscouting.api.dto.CandidateApplicationDto;
import com.wbscouting.api.dto.candidate.CandidateApplyRequestDto;
import com.wbscouting.api.dto.candidate.CandidateApplyResponseDto;
import com.wbscouting.api.entity.Candidate;
import com.wbscouting.api.entity.CandidatePhoto;
import com.wbscouting.api.enums.CandidateStatus;
import com.wbscouting.api.exception.FileSizeExceededException;
import com.wbscouting.api.exception.InvalidApplicationException;
import com.wbscouting.api.exception.InvalidFileException;
import com.wbscouting.api.exception.StorageException;
import com.wbscouting.api.repository.CandidatePhotoRepository;
import com.wbscouting.api.repository.CandidateRepository;
import com.wbscouting.api.service.email.EmailService;
import com.wbscouting.api.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
@RequiredArgsConstructor
public class CandidateApplicationServiceImpl implements CandidateApplicationService {

    public static final String BUCKET_CANDIDATES_UPLOADS = "candidates-uploads";
    public static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024; // 5 MB
    public static final int MIN_PHOTOS = 3;
    public static final int MAX_PHOTOS = 6;

    public static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final CandidateRepository candidateRepository;
    private final CandidatePhotoRepository candidatePhotoRepository;
    private final StorageService storageService;
    private final EmailService emailService;
    private final MailProperties mailProperties;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public CandidateApplyResponseDto apply(CandidateApplyRequestDto requestDto, List<MultipartFile> photos) {
        log.info("Iniciando processamento de nova candidatura pública para: {}", requestDto.getFullName());

        // 1. Validação estrutural de fotos (fail-fast antes de qualquer gravação ou upload)
        validatePhotos(photos);

        // 2. Validação de data de nascimento e menoridade
        int age = calculateAndValidateAge(requestDto);

        // Lista concorrente para controle de compensação (rollback de storage)
        List<String> uploadedPaths = new CopyOnWriteArrayList<>();
        List<UploadedPhoto> uploadedPhotos = new ArrayList<>();

        try {
            // 3. Persistência atômica da ficha cadastral do candidato
            Candidate candidate = mapToCandidateEntity(requestDto, age);
            candidate = candidateRepository.save(candidate);
            final UUID candidateId = candidate.getId();
            log.info("Ficha de candidato salva com ID={}. Iniciando upload concorrente de {} fotos...",
                    candidateId, photos.size());

            // 4. Upload concorrente das fotografias para o bucket privado 'candidates-uploads'
            List<CompletableFuture<UploadedPhoto>> uploadFutures = new ArrayList<>();
            for (int i = 0; i < photos.size(); i++) {
                final int displayOrder = i + 1;
                final MultipartFile file = photos.get(i);
                final String sanitizedFilename = sanitizeFilename(file.getOriginalFilename());
                final String storagePath = String.format("%s/%s-%s", candidateId, UUID.randomUUID(), sanitizedFilename);

                uploadFutures.add(CompletableFuture.supplyAsync(() -> {
                    log.debug("Executando upload de foto ordem={} para path='{}'", displayOrder, storagePath);
                    storageService.uploadFile(BUCKET_CANDIDATES_UPLOADS, storagePath, file);
                    uploadedPaths.add(storagePath);
                    return new UploadedPhoto(displayOrder, storagePath);
                }));
            }

            // Aguarda a conclusão de todos os uploads assíncronos
            CompletableFuture.allOf(uploadFutures.toArray(new CompletableFuture[0])).join();

            for (CompletableFuture<UploadedPhoto> future : uploadFutures) {
                uploadedPhotos.add(future.join());
            }

            // Ordena as fotos pelo índice de exibição
            uploadedPhotos.sort(Comparator.comparingInt(UploadedPhoto::displayOrder));

            // 5. Persistência dos registros das fotos na tabela 'candidate_photos'
            List<CandidatePhoto> photoEntities = new ArrayList<>();
            for (UploadedPhoto upload : uploadedPhotos) {
                CandidatePhoto photoEntity = CandidatePhoto.builder()
                        .candidate(candidate)
                        .storagePath(upload.storagePath())
                        .displayOrder(upload.displayOrder())
                        .photoPosition((short) upload.displayOrder())
                        .filePath(upload.storagePath())
                        .fileUrl(storageService.getPublicUrl(BUCKET_CANDIDATES_UPLOADS, upload.storagePath()))
                        .build();
                photoEntities.add(photoEntity);
            }
            candidatePhotoRepository.saveAll(photoEntities);
            candidate.setPhotos(photoEntities);

            // 6. Registro de envio de e-mail assíncrono pós-commit da transação
            CandidateApplicationDto applicationDto = CandidateApplicationDto.fromEntity(candidate);
            String agencyEmail = mailProperties.getAgencyNotificationEmail();
            schedulePostCommitEmail(agencyEmail, applicationDto);

            log.info("Candidatura concluída com sucesso para ID={}. Protocolo emitido.", candidateId);

            return CandidateApplyResponseDto.builder()
                    .candidateId(candidateId)
                    .message("Candidatura recebida com sucesso.")
                    .submittedAt(Instant.now())
                    .build();

        } catch (Exception ex) {
            log.error("Erro detectado durante submissão da candidatura. Iniciando compensação de arquivos no storage...", ex);
            compensateUploadedFiles(uploadedPaths);

            Throwable root = (ex instanceof CompletionException && ex.getCause() != null) ? ex.getCause() : ex;
            if (root instanceof InvalidApplicationException iae) {
                throw iae;
            }
            if (root instanceof InvalidFileException ife) {
                throw ife;
            }
            if (root instanceof FileSizeExceededException fse) {
                throw fse;
            }
            if (root instanceof StorageException se) {
                throw se;
            }
            throw new StorageException("Falha na submissão da candidatura: " + root.getMessage(), root);
        }
    }

    private void validatePhotos(List<MultipartFile> photos) {
        if (photos == null || photos.size() < MIN_PHOTOS || photos.size() > MAX_PHOTOS) {
            throw new InvalidApplicationException(String.format(
                    "A submissão de candidatura requer o envio de no mínimo %d e no máximo %d fotos. Quantidade enviada: %d",
                    MIN_PHOTOS, MAX_PHOTOS, (photos != null ? photos.size() : 0)
            ));
        }

        for (int i = 0; i < photos.size(); i++) {
            MultipartFile file = photos.get(i);
            if (file == null || file.isEmpty()) {
                throw new InvalidApplicationException(String.format("A foto na posição %d está vazia.", i + 1));
            }

            String contentType = file.getContentType();
            if (!StringUtils.hasText(contentType) || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase().trim())) {
                throw new InvalidApplicationException(String.format(
                        "Tipo de arquivo não permitido para a foto '%s': '%s'. Permitidos apenas JPEG, PNG e WEBP.",
                        file.getOriginalFilename(), contentType
                ));
            }

            if (file.getSize() > MAX_FILE_SIZE_BYTES) {
                throw new InvalidApplicationException(String.format(
                        "A foto '%s' excede o limite máximo permitido de 5 MB. Tamanho enviado: %.2f MB",
                        file.getOriginalFilename(), file.getSize() / (1024.0 * 1024.0)
                ));
            }
        }
    }

    private int calculateAndValidateAge(CandidateApplyRequestDto dto) {
        LocalDate birthDate = dto.getBirthDate();
        LocalDate now = LocalDate.now();

        if (birthDate.isAfter(now)) {
            throw new InvalidApplicationException("A data de nascimento deve ser uma data no passado.");
        }

        int age = Period.between(birthDate, now).getYears();
        if (age < 0) {
            throw new InvalidApplicationException("Idade calculada inválida para a data de nascimento informada.");
        }

        if (age < 18) {
            boolean hasGuardianName = StringUtils.hasText(dto.getLegalGuardianName());
            boolean hasGuardianContact = StringUtils.hasText(dto.getLegalGuardianContact());

            if (!hasGuardianName || !hasGuardianContact) {
                throw new InvalidApplicationException(
                        "Candidatos menores de 18 anos devem informar obrigatoriamente o nome e contato do responsável legal."
                );
            }
        }

        return age;
    }

    private Candidate mapToCandidateEntity(CandidateApplyRequestDto dto, int age) {
        return Candidate.builder()
                .fullName(dto.getFullName().trim())
                .email(dto.getEmail().trim().toLowerCase())
                .phone(dto.getPhone().trim())
                .birthDate(dto.getBirthDate())
                .age(age)
                .gender(dto.getGender().name())
                .heightCm(BigDecimal.valueOf(dto.getHeightCm()))
                .city(dto.getCity().trim())
                .state(dto.getState().trim())
                .legalGuardianName(StringUtils.hasText(dto.getLegalGuardianName()) ? dto.getLegalGuardianName().trim() : null)
                .legalGuardianContact(StringUtils.hasText(dto.getLegalGuardianContact()) ? dto.getLegalGuardianContact().trim() : null)
                .guardianName(StringUtils.hasText(dto.getLegalGuardianName()) ? dto.getLegalGuardianName().trim() : null)
                .bustChestCm(dto.getBustChestCm())
                .waistCm(dto.getWaistCm())
                .hipsCm(dto.getHipsCm())
                .shoeSize(dto.getShoeSize())
                .dressSize(dto.getDressSize())
                .instagramHandle(dto.getInstagramHandle())
                .portfolioUrl(dto.getPortfolioUrl())
                .status(CandidateStatus.PENDING)
                .lgpdAccepted(true)
                .build();
    }

    private void schedulePostCommitEmail(String agencyEmail, CandidateApplicationDto applicationDto) {
        if (!StringUtils.hasText(agencyEmail)) {
            log.warn("E-mail de notificação da agência não configurado. Notificação assíncrona ignorada.");
            return;
        }

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    log.info("Transação confirmada com sucesso no banco. Disparando notificação assíncrona para: {}", agencyEmail);
                    emailService.sendCandidateApplicationNotification(agencyEmail, applicationDto);
                }
            });
        } else {
            emailService.sendCandidateApplicationNotification(agencyEmail, applicationDto);
        }
    }

    private void compensateUploadedFiles(List<String> uploadedPaths) {
        for (String path : uploadedPaths) {
            try {
                log.info("Compensação: deletando arquivo órfão do bucket '{}', path='{}'", BUCKET_CANDIDATES_UPLOADS, path);
                storageService.deleteFile(BUCKET_CANDIDATES_UPLOADS, path);
            } catch (Exception ex) {
                log.warn("Falha na compensação de deleção do arquivo '{}' no bucket '{}': {}",
                        path, BUCKET_CANDIDATES_UPLOADS, ex.getMessage());
            }
        }
    }

    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "photo.jpg";
        }
        String name = filename.contains("/") ? filename.substring(filename.lastIndexOf('/') + 1) : filename;
        name = name.contains("\\") ? name.substring(name.lastIndexOf('\\') + 1) : name;
        String sanitized = name.trim().replaceAll("[^a-zA-Z0-9._-]", "-").replaceAll("-+", "-");
        return sanitized.isEmpty() ? "photo.jpg" : sanitized;
    }

    private record UploadedPhoto(int displayOrder, String storagePath) {}
}

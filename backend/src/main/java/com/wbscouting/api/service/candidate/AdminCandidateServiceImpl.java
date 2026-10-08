package com.wbscouting.api.service.candidate;

import com.wbscouting.api.config.SupabaseProperties;
import com.wbscouting.api.dto.admin.candidate.*;
import com.wbscouting.api.entity.Candidate;
import com.wbscouting.api.entity.CandidatePhoto;
import com.wbscouting.api.enums.CandidateStatus;
import com.wbscouting.api.exception.ResourceNotFoundException;
import com.wbscouting.api.repository.CandidatePhotoRepository;
import com.wbscouting.api.repository.CandidateRepository;
import com.wbscouting.api.service.storage.StorageService;
import com.wbscouting.api.specification.AdminCandidateSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.Period;
import com.wbscouting.api.entity.Model;
import com.wbscouting.api.entity.ModelMedia;
import com.wbscouting.api.entity.CandidateSubmission;
import com.wbscouting.api.enums.GenderType;
import com.wbscouting.api.enums.MediaType;
import com.wbscouting.api.enums.SubmissionStatus;
import com.wbscouting.api.repository.ModelMediaRepository;
import com.wbscouting.api.repository.ModelRepository;
import com.wbscouting.api.repository.CandidateSubmissionRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminCandidateServiceImpl implements AdminCandidateService {

    public static final int SIGNED_URL_EXPIRES_IN_SECONDS = 900;

    private final CandidateRepository candidateRepository;
    private final CandidatePhotoRepository candidatePhotoRepository;
    private final ModelRepository modelRepository;
    private final ModelMediaRepository modelMediaRepository;
    private final CandidateSubmissionRepository candidateSubmissionRepository;
    private final StorageService storageService;
    private final SupabaseProperties supabaseProperties;

    @Override
    @Transactional(readOnly = true)
    public Page<CandidateListItemAdminDto> listCandidates(
            CandidateStatus status,
            String search,
            String gender,
            Boolean isMinor,
            Pageable pageable) {

        log.info("Listando candidaturas com filtros administrativos: status={}, search='{}', gender={}, isMinor={}, pageable={}",
                status, search, gender, isMinor, pageable);

        Specification<Candidate> spec = AdminCandidateSpecification.filter(status, search, gender, isMinor);
        Page<Candidate> candidatePage = candidateRepository.findAll(spec, pageable);

        if (candidatePage.isEmpty()) {
            return Page.empty(pageable);
        }

        // Agregação de contagem de fotos em batch para prevenir N+1
        List<UUID> candidateIds = candidatePage.getContent().stream()
                .map(Candidate::getId)
                .collect(Collectors.toList());

        Map<UUID, Integer> photoCounts = fetchPhotoCounts(candidateIds);

        List<CandidateListItemAdminDto> dtoList = candidatePage.getContent().stream()
                .map(candidate -> toListItemDto(candidate, photoCounts.getOrDefault(candidate.getId(), 0)))
                .collect(Collectors.toList());

        return new PageImpl<>(dtoList, pageable, candidatePage.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public CandidateDetailAdminDto getCandidateDetail(UUID id) {
        log.info("Buscando detalhes da candidatura ID: {}", id);

        Candidate candidate = candidateRepository.findWithPhotosById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidatura não encontrada com ID: " + id));

        return toDetailDtoWithSignedUrls(candidate);
    }

    @Override
    @Transactional
    public CandidateDetailAdminDto updateStatus(UUID id, CandidateStatusUpdateDto dto) {
        log.info("Atualizando status da candidatura ID: {} para {}", id, dto.getStatus());

        Candidate candidate = candidateRepository.findWithPhotosById(id).orElse(null);
        if (candidate == null) {
            candidate = candidateRepository.findById(id).orElse(null);
        }

        String protocol = null;
        Candidate saved = null;
        if (candidate != null) {
            candidate.setStatus(dto.getStatus());
            candidate.setUpdatedAt(OffsetDateTime.now());
            saved = candidateRepository.saveAndFlush(candidate);
            protocol = candidate.getProtocol();
        }

        if (candidateSubmissionRepository != null) {
            try {
                SubmissionStatus subStatus;
                if (dto.getStatus() == CandidateStatus.REJECTED) {
                    subStatus = SubmissionStatus.REJECTED;
                } else if (dto.getStatus() == CandidateStatus.APPROVED) {
                    subStatus = SubmissionStatus.APPROVED;
                } else if (dto.getStatus() == CandidateStatus.PENDING) {
                    subStatus = SubmissionStatus.PENDING;
                } else if (dto.getStatus() == CandidateStatus.ARCHIVED) {
                    subStatus = SubmissionStatus.ARCHIVED;
                } else {
                    subStatus = SubmissionStatus.valueOf(dto.getStatus().name());
                }

                final SubmissionStatus finalSubStatus = subStatus;
                candidateSubmissionRepository.findById(id).ifPresent(s -> {
                    s.setStatus(finalSubStatus);
                    s.setReviewedAt(OffsetDateTime.now());
                    candidateSubmissionRepository.saveAndFlush(s);
                });
                if (protocol != null) {
                    candidateSubmissionRepository.findByProtocol(protocol).ifPresent(s -> {
                        s.setStatus(finalSubStatus);
                        s.setReviewedAt(OffsetDateTime.now());
                        candidateSubmissionRepository.saveAndFlush(s);
                    });
                } else {
                    candidateSubmissionRepository.findById(id).ifPresent(s -> {
                        if (s.getProtocol() != null) {
                            candidateRepository.findByProtocol(s.getProtocol()).ifPresent(c -> {
                                c.setStatus(dto.getStatus());
                                c.setUpdatedAt(OffsetDateTime.now());
                                candidateRepository.saveAndFlush(c);
                            });
                        }
                    });
                }
            } catch (Exception ignored) {}
        }

        if (saved != null) {
            return toDetailDtoWithSignedUrls(saved);
        }

        CandidateSubmission sub = candidateSubmissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidatura não encontrada com ID: " + id));
        if (sub.getProtocol() != null) {
            Optional<Candidate> cByProt = candidateRepository.findByProtocol(sub.getProtocol());
            if (cByProt.isPresent()) {
                return toDetailDtoWithSignedUrls(cByProt.get());
            }
        }
        return toDetailDto(sub);
    }

    @Override
    @Transactional
    public CandidateDetailAdminDto updateNotes(UUID id, CandidateNotesUpdateDto dto) {
        log.info("Atualizando anotações internas da candidatura ID: {}", id);

        Candidate candidate = candidateRepository.findWithPhotosById(id).orElse(null);
        if (candidate == null) {
            candidate = candidateRepository.findById(id).orElse(null);
        }

        String protocol = null;
        Candidate saved = null;
        if (candidate != null) {
            candidate.setInternalNotes(dto.getInternalNotes());
            candidate.setUpdatedAt(OffsetDateTime.now());
            saved = candidateRepository.saveAndFlush(candidate);
            protocol = candidate.getProtocol();
        }

        if (candidateSubmissionRepository != null) {
            candidateSubmissionRepository.findById(id).ifPresent(s -> {
                s.setFeedbackNotes(dto.getInternalNotes());
                candidateSubmissionRepository.saveAndFlush(s);
            });
            if (protocol != null) {
                candidateSubmissionRepository.findByProtocol(protocol).ifPresent(s -> {
                    s.setFeedbackNotes(dto.getInternalNotes());
                    candidateSubmissionRepository.saveAndFlush(s);
                });
            } else {
                candidateSubmissionRepository.findById(id).ifPresent(s -> {
                    if (s.getProtocol() != null) {
                        candidateRepository.findByProtocol(s.getProtocol()).ifPresent(c -> {
                            c.setInternalNotes(dto.getInternalNotes());
                            c.setUpdatedAt(OffsetDateTime.now());
                            candidateRepository.saveAndFlush(c);
                        });
                    }
                });
            }
        }

        if (saved != null) {
            return toDetailDtoWithSignedUrls(saved);
        }

        CandidateSubmission sub = candidateSubmissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidatura não encontrada com ID: " + id));
        if (sub.getProtocol() != null) {
            Optional<Candidate> cByProt = candidateRepository.findByProtocol(sub.getProtocol());
            if (cByProt.isPresent()) {
                return toDetailDtoWithSignedUrls(cByProt.get());
            }
        }
        return toDetailDto(sub);
    }

    @Override
    @Transactional
    public void deleteCandidate(UUID id) {
        log.info("Iniciando arquivamento (soft delete) da candidatura ID: {}", id);

        Candidate candidate = candidateRepository.findById(id).orElse(null);
        if (candidate != null) {
            candidate.setStatus(CandidateStatus.ARCHIVED);
            candidate.setArchivedAt(OffsetDateTime.now());
            candidateRepository.saveAndFlush(candidate);

            if (candidateSubmissionRepository != null) {
                candidateSubmissionRepository.findById(id).ifPresent(s -> {
                    s.setStatus(SubmissionStatus.ARCHIVED);
                    s.setArchivedAt(OffsetDateTime.now());
                    candidateSubmissionRepository.saveAndFlush(s);
                });
                if (candidate.getProtocol() != null) {
                    candidateSubmissionRepository.findByProtocol(candidate.getProtocol()).ifPresent(s -> {
                        s.setStatus(SubmissionStatus.ARCHIVED);
                        s.setArchivedAt(OffsetDateTime.now());
                        candidateSubmissionRepository.saveAndFlush(s);
                    });
                }
            }
            log.info("Candidatura ID: {} movida para Arquivo Morto com sucesso.", id);
            return;
        }

        if (candidateSubmissionRepository != null) {
            CandidateSubmission submission = candidateSubmissionRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Candidatura não encontrada com ID: " + id));
            submission.setStatus(SubmissionStatus.ARCHIVED);
            submission.setArchivedAt(OffsetDateTime.now());
            candidateSubmissionRepository.saveAndFlush(submission);

            if (submission.getProtocol() != null) {
                candidateRepository.findByProtocol(submission.getProtocol()).ifPresent(c -> {
                    c.setStatus(CandidateStatus.ARCHIVED);
                    c.setArchivedAt(OffsetDateTime.now());
                    candidateRepository.saveAndFlush(c);
                });
            }
            log.info("Candidatura submission ID: {} movida para Arquivo Morto com sucesso.", id);
        } else {
            throw new ResourceNotFoundException("Candidatura não encontrada com ID: " + id);
        }
    }

    @Override
    @Transactional
    public UUID promoteToModel(UUID id, Boolean activateImmediately) {
        log.info("Iniciando promoção da candidatura ID: {} para Modelo Oficial", id);

        Candidate candidate = candidateRepository.findWithPhotosById(id).orElse(null);
        if (candidate != null) {
            // Determina gênero
            GenderType gender = GenderType.MALE;
            if (candidate.getGender() != null) {
                String g = candidate.getGender().trim().toLowerCase();
                if (g.contains("fem") || g.equals("f")) {
                    gender = GenderType.FEMALE;
                }
            }

            // Normaliza altura em cm
            Integer heightCm = null;
            if (candidate.getHeightCm() != null) {
                if (candidate.getHeightCm().compareTo(BigDecimal.valueOf(3)) < 0) {
                    heightCm = candidate.getHeightCm().multiply(BigDecimal.valueOf(100)).intValue();
                } else {
                    heightCm = candidate.getHeightCm().intValue();
                }
            }

            // Foto principal de capa
            String primaryPhotoUrl = null;
            if (candidate.getPhotos() != null && !candidate.getPhotos().isEmpty()) {
                CandidatePhoto firstPhoto = candidate.getPhotos().stream()
                        .sorted(Comparator.comparing(CandidatePhoto::getDisplayOrder, Comparator.nullsLast(Comparator.naturalOrder())))
                        .findFirst().orElse(null);
                if (firstPhoto != null) {
                    primaryPhotoUrl = firstPhoto.getFileUrl() != null ? firstPhoto.getFileUrl() : firstPhoto.getFilePath();
                    if (primaryPhotoUrl == null) primaryPhotoUrl = firstPhoto.getStoragePath();
                }
            }

            // 1. Cria a nova entidade Model com os dados do Candidate
            Model model = Model.builder()
                    .stageName(StringUtils.hasText(candidate.getFullName()) ? candidate.getFullName().trim() : "Novo Talento")
                    .gender(gender)
                    .isStar(false)
                    .isFeaturedHome(false)
                    .isActive(activateImmediately == null || Boolean.TRUE.equals(activateImmediately))
                    .primaryPhotoUrl(primaryPhotoUrl)
                    .instagramUrl(candidate.getInstagramHandle())
                    .birthDate(candidate.getBirthDate())
                    .heightCm(heightCm)
                    .city(candidate.getCity())
                    .nationality("Brasileira")
                    .dressSize(candidate.getDressSize())
                    .shoeSize(candidate.getShoeSize())
                    .bustChestCm(candidate.getBustChestCm())
                    .waistCm(candidate.getWaistCm())
                    .hipsCm(candidate.getHipsCm())
                    .build();

            Model savedModel = modelRepository.save(model);

            // 2. Migra as URLs das fotos de candidate_photos para a galeria do Model (ModelMedia)
            if (candidate.getPhotos() != null && !candidate.getPhotos().isEmpty()) {
                int order = 1;
                for (CandidatePhoto p : candidate.getPhotos()) {
                    String photoUrl = p.getFileUrl() != null ? p.getFileUrl() : p.getFilePath();
                    if (photoUrl == null) photoUrl = p.getStoragePath();
                    if (StringUtils.hasText(photoUrl)) {
                        ModelMedia media = ModelMedia.builder()
                                .model(savedModel)
                                .mediaType(order == 1 ? MediaType.BOOK : MediaType.POLAROID)
                                .fileUrl(photoUrl)
                                .filePath(p.getStoragePath() != null ? p.getStoragePath() : photoUrl)
                                .displayOrder(p.getDisplayOrder() != null ? p.getDisplayOrder() : order)
                                .isCover(order == 1)
                                .isActive(true)
                                .build();
                        modelMediaRepository.save(media);
                        order++;
                    }
                }
            }

            // 3. Remove fisicamente o registro original de candidates e candidate_photos
            candidateRepository.delete(candidate);
            candidateRepository.flush();

            log.info("Candidato ID: {} promovido para Modelo ID: {} e removido da esteira com sucesso.", id, savedModel.getId());
            return savedModel.getId();
        }

        // Fallback: caso o ID seja de CandidateSubmission
        if (candidateSubmissionRepository != null) {
            CandidateSubmission submission = candidateSubmissionRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Candidatura não encontrada com ID: " + id));

            GenderType gender = (submission.getGender() != null && submission.getGender().name().contains("FEM"))
                    ? GenderType.FEMALE : GenderType.MALE;

            Integer heightCm = null;
            if (submission.getHeight() != null) {
                if (submission.getHeight().compareTo(BigDecimal.valueOf(3)) < 0) {
                    heightCm = submission.getHeight().multiply(BigDecimal.valueOf(100)).intValue();
                } else {
                    heightCm = submission.getHeight().intValue();
                }
            }

            Model model = Model.builder()
                    .stageName(StringUtils.hasText(submission.getFullName()) ? submission.getFullName().trim() : "Novo Talento")
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

            if (StringUtils.hasText(submission.getFacePhotoUrl())) {
                modelMediaRepository.save(ModelMedia.builder()
                        .model(savedModel)
                        .mediaType(MediaType.BOOK)
                        .fileUrl(submission.getFacePhotoUrl())
                        .filePath(submission.getFacePhotoUrl())
                        .displayOrder(1)
                        .isCover(true)
                        .isActive(true)
                        .build());
            }

            if (StringUtils.hasText(submission.getProfilePhotoUrl())) {
                modelMediaRepository.save(ModelMedia.builder()
                        .model(savedModel)
                        .mediaType(MediaType.POLAROID)
                        .fileUrl(submission.getProfilePhotoUrl())
                        .filePath(submission.getProfilePhotoUrl())
                        .displayOrder(2)
                        .isCover(false)
                        .isActive(true)
                        .build());
            }

            if (StringUtils.hasText(submission.getFullBodyPhotoUrl())) {
                modelMediaRepository.save(ModelMedia.builder()
                        .model(savedModel)
                        .mediaType(MediaType.POLAROID)
                        .fileUrl(submission.getFullBodyPhotoUrl())
                        .filePath(submission.getFullBodyPhotoUrl())
                        .displayOrder(3)
                        .isCover(false)
                        .isActive(true)
                        .build());
            }

            submission.setStatus(SubmissionStatus.PROMOTED);
            submission.setConvertedToModelId(savedModel.getId());
            candidateSubmissionRepository.save(submission);

            log.info("Candidatura submission ID: {} promovida para Modelo ID: {}", id, savedModel.getId());
            return savedModel.getId();
        }

        throw new ResourceNotFoundException("Candidatura não encontrada com ID: " + id);
    }

    private Map<UUID, Integer> fetchPhotoCounts(List<UUID> candidateIds) {
        try {
            List<Object[]> results = candidatePhotoRepository.countPhotosByCandidateIds(candidateIds);
            Map<UUID, Integer> map = new HashMap<>();
            for (Object[] row : results) {
                UUID candidateId = (UUID) row[0];
                Long count = (Long) row[1];
                map.put(candidateId, count != null ? count.intValue() : 0);
            }
            return map;
        } catch (Exception ex) {
            log.warn("Falha ao agregar contagem de fotos em batch. Utilizando fallback: {}", ex.getMessage());
            return Collections.emptyMap();
        }
    }

    private CandidateListItemAdminDto toListItemDto(Candidate candidate, int photoCount) {
        Integer age = resolveAge(candidate.getBirthDate(), candidate.getAge());
        boolean isMinor = isMinorCheck(candidate.getBirthDate(), age);

        return CandidateListItemAdminDto.builder()
                .id(candidate.getId())
                .fullName(candidate.getFullName())
                .email(candidate.getEmail())
                .phone(candidate.getPhone())
                .gender(candidate.getGender())
                .birthDate(candidate.getBirthDate())
                .age(age)
                .isMinor(isMinor)
                .city(candidate.getCity())
                .state(candidate.getState())
                .heightCm(candidate.getHeightCm())
                .status(candidate.getStatus())
                .photoCount(photoCount)
                .createdAt(candidate.getCreatedAt())
                .build();
    }

    private CandidateDetailAdminDto toDetailDtoWithSignedUrls(Candidate candidate) {
        Integer age = resolveAge(candidate.getBirthDate(), candidate.getAge());
        boolean isMinor = isMinorCheck(candidate.getBirthDate(), age);
        String bucket = resolveBucketName();

        List<CandidatePhotoSignedDto> signedPhotos = new ArrayList<>();
        if (candidate.getPhotos() != null) {
            signedPhotos = candidate.getPhotos().stream()
                    .sorted(Comparator.comparing(CandidatePhoto::getDisplayOrder, Comparator.nullsLast(Comparator.naturalOrder())))
                    .map(photo -> generateSignedPhotoDto(photo, bucket))
                    .collect(Collectors.toList());
        }

        return CandidateDetailAdminDto.builder()
                .id(candidate.getId())
                .fullName(candidate.getFullName())
                .email(candidate.getEmail())
                .phone(candidate.getPhone())
                .birthDate(candidate.getBirthDate())
                .age(age)
                .isMinor(isMinor)
                .guardianName(candidate.getGuardianName())
                .legalGuardianContact(candidate.getLegalGuardianContact())
                .gender(candidate.getGender())
                .heightCm(candidate.getHeightCm())
                .weightKg(candidate.getWeightKg())
                .bustChestCm(candidate.getBustChestCm())
                .waistCm(candidate.getWaistCm())
                .hipsCm(candidate.getHipsCm())
                .shoeSize(candidate.getShoeSize())
                .dressSize(candidate.getDressSize())
                .city(candidate.getCity())
                .state(candidate.getState())
                .instagramHandle(candidate.getInstagramHandle())
                .tiktokHandle(candidate.getTiktokHandle())
                .portfolioUrl(candidate.getPortfolioUrl())
                .status(candidate.getStatus())
                .internalNotes(candidate.getInternalNotes())
                .lgpdAccepted(candidate.getLgpdAccepted())
                .lgpdAcceptedAt(candidate.getLgpdAcceptedAt())
                .photos(signedPhotos)
                .createdAt(candidate.getCreatedAt())
                .updatedAt(candidate.getUpdatedAt())
                .build();
    }

    private CandidatePhotoSignedDto generateSignedPhotoDto(CandidatePhoto photo, String bucket) {
        String signedUrl = null;
        String path = photo.getStoragePath();

        if (StringUtils.hasText(path)) {
            try {
                signedUrl = storageService.createSignedUrl(bucket, path, SIGNED_URL_EXPIRES_IN_SECONDS);
            } catch (Exception ex) {
                log.error("Erro ao gerar signed URL para a foto ID={} no caminho '{}': {}",
                        photo.getId(), path, ex.getMessage());
            }
        }

        return CandidatePhotoSignedDto.builder()
                .id(photo.getId())
                .displayOrder(photo.getDisplayOrder())
                .signedUrl(signedUrl)
                .expiresInSeconds(SIGNED_URL_EXPIRES_IN_SECONDS)
                .build();
    }

    private Integer resolveAge(LocalDate birthDate, Integer storedAge) {
        if (storedAge != null) {
            return storedAge;
        }
        if (birthDate != null) {
            return Period.between(birthDate, LocalDate.now()).getYears();
        }
        return null;
    }

    private boolean isMinorCheck(LocalDate birthDate, Integer age) {
        if (age != null) {
            return age < 18;
        }
        if (birthDate != null) {
            return Period.between(birthDate, LocalDate.now()).getYears() < 18;
        }
        return false;
    }

    private String resolveBucketName() {
        return supabaseProperties.resolveBucketCandidates();
    }

    private CandidateDetailAdminDto toDetailDto(CandidateSubmission submission) {
        CandidateStatus candidateStatus = CandidateStatus.PENDING;
        try {
            if (submission.getStatus() != null) {
                if (submission.getStatus() == SubmissionStatus.DECLINED || submission.getStatus() == SubmissionStatus.REJECTED) {
                    candidateStatus = CandidateStatus.REJECTED;
                } else if (submission.getStatus() == SubmissionStatus.APPROVED) {
                    candidateStatus = CandidateStatus.APPROVED;
                } else if (submission.getStatus() == SubmissionStatus.ARCHIVED) {
                    candidateStatus = CandidateStatus.ARCHIVED;
                }
            }
        } catch (Exception ignored) {}

        List<CandidatePhotoSignedDto> photos = new ArrayList<>();
        if (StringUtils.hasText(submission.getFacePhotoUrl())) {
            photos.add(CandidatePhotoSignedDto.builder().signedUrl(submission.getFacePhotoUrl()).displayOrder(1).build());
        }
        if (StringUtils.hasText(submission.getProfilePhotoUrl())) {
            photos.add(CandidatePhotoSignedDto.builder().signedUrl(submission.getProfilePhotoUrl()).displayOrder(2).build());
        }
        if (StringUtils.hasText(submission.getFullBodyPhotoUrl())) {
            photos.add(CandidatePhotoSignedDto.builder().signedUrl(submission.getFullBodyPhotoUrl()).displayOrder(3).build());
        }
        boolean isMinor = submission.getAge() != null ? submission.getAge() < 18 : false;
        String shoeSizeStr = submission.getShoeSize() != null ? String.valueOf(submission.getShoeSize()) : null;

        return CandidateDetailAdminDto.builder()
                .id(submission.getId())
                .fullName(submission.getFullName())
                .email(submission.getEmail())
                .phone(submission.getPhone())
                .birthDate(submission.getBirthDate())
                .age(submission.getAge())
                .isMinor(isMinor)
                .guardianName(submission.getGuardianName())
                .legalGuardianContact(submission.getGuardianPhone())
                .gender(submission.getGender() != null ? submission.getGender().name() : null)
                .heightCm(submission.getHeight())
                .bustChestCm(submission.getBust())
                .waistCm(submission.getWaist())
                .hipsCm(submission.getHips())
                .shoeSize(shoeSizeStr)
                .city(submission.getCity())
                .state(submission.getState())
                .instagramHandle(submission.getInstagramHandle())
                .status(candidateStatus)
                .internalNotes(submission.getFeedbackNotes())
                .lgpdAccepted(submission.getLgpdConsent())
                .photos(photos)
                .createdAt(submission.getCreatedAt() != null ? submission.getCreatedAt() : OffsetDateTime.now())
                .updatedAt(submission.getUpdatedAt() != null ? submission.getUpdatedAt() : OffsetDateTime.now())
                .build();
    }
}

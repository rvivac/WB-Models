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
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminCandidateServiceImpl implements AdminCandidateService {

    public static final String DEFAULT_BUCKET_CANDIDATES = "candidates-uploads";
    public static final int SIGNED_URL_EXPIRES_IN_SECONDS = 900; // 15 minutos

    private final CandidateRepository candidateRepository;
    private final CandidatePhotoRepository candidatePhotoRepository;
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

        Candidate candidate = candidateRepository.findWithPhotosById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidatura não encontrada com ID: " + id));

        candidate.setStatus(dto.getStatus());
        Candidate saved = candidateRepository.save(candidate);

        return toDetailDtoWithSignedUrls(saved);
    }

    @Override
    @Transactional
    public CandidateDetailAdminDto updateNotes(UUID id, CandidateNotesUpdateDto dto) {
        log.info("Atualizando anotações internas da candidatura ID: {}", id);

        Candidate candidate = candidateRepository.findWithPhotosById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidatura não encontrada com ID: " + id));

        candidate.setInternalNotes(dto.getInternalNotes());
        Candidate saved = candidateRepository.save(candidate);

        return toDetailDtoWithSignedUrls(saved);
    }

    @Override
    @Transactional
    public void deleteCandidate(UUID id) {
        log.info("Iniciando exclusão segura da candidatura ID: {}", id);

        Candidate candidate = candidateRepository.findWithPhotosById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidatura não encontrada com ID: " + id));

        String bucket = resolveBucketName();

        // 1. Exclusão física das fotos no Supabase Storage
        if (candidate.getPhotos() != null && !candidate.getPhotos().isEmpty()) {
            for (CandidatePhoto photo : candidate.getPhotos()) {
                String storagePath = photo.getStoragePath();
                if (StringUtils.hasText(storagePath)) {
                    try {
                        log.debug("Removendo arquivo físico da foto no storage: bucket={}, path={}", bucket, storagePath);
                        storageService.deleteFile(bucket, storagePath);
                    } catch (Exception ex) {
                        log.warn("Erro não impeditivo ao remover foto do storage durante exclusão: bucket={}, path={}, erro={}",
                                bucket, storagePath, ex.getMessage());
                    }
                }
            }
        }

        // 2. Exclusão relacional no banco de dados
        candidateRepository.delete(candidate);
        log.info("Candidatura ID: {} e arquivos associados removidos com sucesso.", id);
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
        if (supabaseProperties != null && supabaseProperties.getBuckets() != null
                && StringUtils.hasText(supabaseProperties.getBuckets().getCandidatesUploads())) {
            return supabaseProperties.getBuckets().getCandidatesUploads();
        }
        return DEFAULT_BUCKET_CANDIDATES;
    }
}

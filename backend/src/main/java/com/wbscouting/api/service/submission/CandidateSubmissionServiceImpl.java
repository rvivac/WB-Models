package com.wbscouting.api.service.submission;

import com.wbscouting.api.config.SupabaseProperties;
import com.wbscouting.api.dto.CandidateSubmissionRequestDto;
import com.wbscouting.api.dto.CandidateSubmissionResponseDto;
import com.wbscouting.api.entity.CandidateSubmission;
import com.wbscouting.api.enums.SubmissionStatus;
import com.wbscouting.api.exception.BusinessException;
import com.wbscouting.api.entity.CandidatePhoto;
import com.wbscouting.api.repository.CandidatePhotoRepository;
import com.wbscouting.api.repository.CandidateSubmissionRepository;
import com.wbscouting.api.service.storage.StorageService;
import com.wbscouting.api.service.storage.SupabaseStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Service
public class CandidateSubmissionServiceImpl implements CandidateSubmissionService {

    private static final Pattern HTML_PATTERN = Pattern.compile("<[^>]*>");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$");

    private static final byte[] JPEG_MAGIC = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_MAGIC = new byte[]{(byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

    private final CandidateSubmissionRepository repository;
    private final CandidatePhotoRepository candidatePhotoRepository;
    private final StorageService storageService;
    private final SupabaseProperties supabaseProperties;

    public CandidateSubmissionServiceImpl(CandidateSubmissionRepository repository,
                                          CandidatePhotoRepository candidatePhotoRepository,
                                          StorageService storageService,
                                          SupabaseProperties supabaseProperties) {
        this.repository = repository;
        this.candidatePhotoRepository = candidatePhotoRepository;
        this.storageService = storageService;
        this.supabaseProperties = supabaseProperties;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CandidateSubmissionResponseDto submit(
            CandidateSubmissionRequestDto request,
            MultipartFile facePhoto,
            MultipartFile profilePhoto,
            MultipartFile fullBodyPhoto
    ) {
        log.info("Processando submissão pública de candidatura para: {}", request.getFullName());

        // 1. Validação de Idade e Responsável Legal
        int age = calculateAndValidateAge(request.getBirthDate());
        if (age < 18) {
            validateGuardianInfo(request);
        }

        // 2. Validação Estrita dos Arquivos de Foto
        validateImageFile(facePhoto, "Rosto (facePhoto)");
        validateImageFile(profilePhoto, "Perfil (profilePhoto)");
        validateImageFile(fullBodyPhoto, "Corpo Inteiro (fullBodyPhoto)");

        // 3. Sanitização de Entradas de Texto
        String sanitizedFullName = sanitizeString(request.getFullName());
        String sanitizedCity = sanitizeString(request.getCity());
        String sanitizedState = request.getState().trim().toUpperCase();
        String sanitizedInstagram = sanitizeInstagram(request.getInstagramHandle());
        String sanitizedGuardianName = sanitizeString(request.getGuardianName());
        String sanitizedEyeColor = sanitizeString(request.getEyeColor());
        String sanitizedHairColor = sanitizeString(request.getHairColor());

        // 4. Geração de Identificadores e Protocolo
        // 🆕 storageId = UUID SEPARADO apenas para paths no bucket (nao precisa = ID da entidade!)
        // 🆕 REMOVIDO: uso de submissionId como ID da entidade, pois ela tem @GeneratedValue(strategy = GenerationType.UUID)
        //    (setar ID manualmente antes do save() faz Hibernate tentar UPDATE e dar: unsaved-value mapping was incorrect)
        UUID storageId = UUID.randomUUID();
        String protocol = generateProtocol();
        String bucket = supabaseProperties.resolveBucketCandidates();

        // 5. Upload dos Arquivos para o Storage com Transação Compensatória Defensiva
        java.util.List<String> uploadedPaths = new java.util.ArrayList<>();
        try {
            String facePath = String.format("submissions/%s/face_%s", storageId, cleanFileName(facePhoto.getOriginalFilename()));
            String profilePath = String.format("submissions/%s/profile_%s", storageId, cleanFileName(profilePhoto.getOriginalFilename()));
            String fullBodyPath = String.format("submissions/%s/fullbody_%s", storageId, cleanFileName(fullBodyPhoto.getOriginalFilename()));

            storageService.uploadFile(bucket, facePath, facePhoto);
            uploadedPaths.add(facePath);
            storageService.uploadFile(bucket, profilePath, profilePhoto);
            uploadedPaths.add(profilePath);
            storageService.uploadFile(bucket, fullBodyPath, fullBodyPhoto);
            uploadedPaths.add(fullBodyPath);

            String faceUrl = storageService.getPublicUrl(bucket, facePath);
            String profileUrl = storageService.getPublicUrl(bucket, profilePath);
            String fullBodyUrl = storageService.getPublicUrl(bucket, fullBodyPath);

            // 6. Construção e Persistência da Entidade
            // 🆕 OMITIMOS o campo .id() — Hibernate gera o UUID sozinho no INSERT (obedecendo @GeneratedValue(strategy = GenerationType.UUID))
            CandidateSubmission submission = CandidateSubmission.builder()
                    .protocol(protocol)
                    .fullName(sanitizedFullName)
                    .email(request.getEmail().trim().toLowerCase())
                    .phone(request.getPhone().trim())
                    .birthDate(request.getBirthDate())
                    .age(age)
                    .gender(request.getGender())
                    .city(sanitizedCity)
                    .state(sanitizedState)
                    .height(request.getHeight())
                    .bust(request.getBust())
                    .waist(request.getWaist())
                    .hips(request.getHips())
                    .shoeSize(request.getShoeSize())
                    .eyeColor(sanitizedEyeColor)
                    .hairColor(sanitizedHairColor)
                    .instagramHandle(sanitizedInstagram)
                    .guardianName(sanitizedGuardianName)
                    .guardianPhone(StringUtils.hasText(request.getGuardianPhone()) ? request.getGuardianPhone().trim() : null)
                    .guardianEmail(StringUtils.hasText(request.getGuardianEmail()) ? request.getGuardianEmail().trim().toLowerCase() : null)
                    .lgpdConsent(Boolean.TRUE.equals(request.getLgpdConsent()))
                    .lgpdConsentAt(OffsetDateTime.now())
                    .status(SubmissionStatus.PENDING)
                    .facePhotoUrl(faceUrl)
                    .profilePhotoUrl(profileUrl)
                    .fullBodyPhotoUrl(fullBodyUrl)
                    .build();

            CandidateSubmission saved = repository.save(submission);
            log.info("Candidatura gravada com sucesso. ID: {}, Protocolo: {}", saved.getId(), saved.getProtocol());

            // ================================
            // 🔥 REGRA OBRIGATÓRIA: Persistir relação filha em candidate_photos
            //     Tabela: candidate_photos.candidate_id (FK) → candidate_submissions.id
            // ================================
            persistirFotoFilha(saved, uploadedPaths.get(0), faceUrl, "POLAROID_ROSTO", 1, cleanFileName(facePhoto.getOriginalFilename()));
            persistirFotoFilha(saved, uploadedPaths.get(1), profileUrl, "POLAROID_PERFIL", 2, cleanFileName(profilePhoto.getOriginalFilename()));
            persistirFotoFilha(saved, uploadedPaths.get(2), fullBodyUrl, "CORPO_INTEIRO", 3, cleanFileName(fullBodyPhoto.getOriginalFilename()));
            log.info("[CANDIDATE_PHOTOS] 3 fotos filhas vinculadas via candidate_id = {}", saved.getId());

            // ================================
            // Response: protocolo + id + fullName + email + status (DoD)
            // ================================
            return CandidateSubmissionResponseDto.builder()
                    .id(saved.getId())
                    .protocol(saved.getProtocol())
                    .message("Candidatura enviada com sucesso! Nossa equipe de scouting analisará seu material.")
                    .status(saved.getStatus())
                    .fullName(saved.getFullName())
                    .email(saved.getEmail())
                    .createdAt(saved.getCreatedAt() != null ? saved.getCreatedAt() : OffsetDateTime.now())
                    .build();
        } catch (Exception ex) {
            log.error("Erro detectado durante submissão da candidatura (storageId={}). Executando rollback compensatório no bucket '{}'...",
                    storageId, bucket, ex);
            for (String uploadedPath : uploadedPaths) {
                try {
                    storageService.deleteFile(bucket, uploadedPath);
                    log.info("Arquivo órfão purgado via rollback compensatório: {}/{}", bucket, uploadedPath);
                } catch (Exception delEx) {
                    log.warn("Falha ao purgar arquivo órfão no storage: {}/{} - {}", bucket, uploadedPath, delEx.getMessage());
                }
            }
            throw ex;
        }
    }

    private int calculateAndValidateAge(LocalDate birthDate) {
        if (birthDate == null) {
            throw new BusinessException("Data de nascimento é obrigatória.");
        }
        if (birthDate.isAfter(LocalDate.now())) {
            throw new BusinessException("A data de nascimento deve ser uma data passada.");
        }
        int age = Period.between(birthDate, LocalDate.now()).getYears();
        if (age < 2 || age > 99) {
            throw new BusinessException("Idade fora dos limites operacionais de agenciamento.");
        }
        return age;
    }

    private void validateGuardianInfo(CandidateSubmissionRequestDto request) {
        if (!StringUtils.hasText(request.getGuardianName())) {
            throw new BusinessException("Para candidatos menores de 18 anos, o nome do responsável legal é obrigatório.");
        }
        if (!StringUtils.hasText(request.getGuardianPhone())) {
            throw new BusinessException("Para candidatos menores de 18 anos, o telefone de contato do responsável legal é obrigatório.");
        }
        if (!StringUtils.hasText(request.getGuardianEmail())) {
            throw new BusinessException("Para candidatos menores de 18 anos, o e-mail do responsável legal é obrigatório.");
        }
        if (!EMAIL_PATTERN.matcher(request.getGuardianEmail().trim()).matches()) {
            throw new BusinessException("O e-mail do responsável legal possui formato inválido.");
        }
    }

    private static final byte[] WEBP_RIFF_MAGIC = new byte[]{'R', 'I', 'F', 'F'};
    private static final byte[] WEBP_FORMAT_SIG = new byte[]{'W', 'E', 'B', 'P'};

    private void validateImageFile(MultipartFile file, String photoRole) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("O arquivo de foto " + photoRole + " é obrigatório e não pode estar vazio.");
        }

        long maxSizeBytes = SupabaseStorageService.MAX_SIZE_CANDIDATES_UPLOADS;
        if (file.getSize() > maxSizeBytes) {
            long maxMb = maxSizeBytes / (1024 * 1024);
            throw new BusinessException(String.format(
                    "O arquivo enviado excede o limite máximo permitido de %d MB para %s.", maxMb, photoRole));
        }

        // Validação de integridade do cabeçalho (Magic Bytes): JPEG, PNG e WEBP
        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[12];
            int read = is.read(header);
            if (read < 4) {
                throw new BusinessException("Arquivo corrompido ou inválido para " + photoRole + ".");
            }

            boolean isJpeg = header[0] == JPEG_MAGIC[0] && header[1] == JPEG_MAGIC[1] && header[2] == JPEG_MAGIC[2];
            boolean isPng = read >= 8 && Arrays.equals(java.util.Arrays.copyOf(header, 8), PNG_MAGIC);
            boolean isWebp = read >= 12
                    && header[0] == WEBP_RIFF_MAGIC[0] && header[1] == WEBP_RIFF_MAGIC[1]
                    && header[2] == WEBP_RIFF_MAGIC[2] && header[3] == WEBP_RIFF_MAGIC[3]
                    && header[8] == WEBP_FORMAT_SIG[0] && header[9] == WEBP_FORMAT_SIG[1]
                    && header[10] == WEBP_FORMAT_SIG[2] && header[11] == WEBP_FORMAT_SIG[3];

            if (!isJpeg && !isPng && !isWebp) {
                log.warn("Tentativa de upload com cabeçalho adulterado para {}. Bytes recebidos: {}", photoRole, Arrays.toString(header));
                throw new BusinessException("Cabeçalho de arquivo adulterado ou inválido para " + photoRole + ". Envie uma imagem válida (JPEG, PNG ou WEBP).");
            }
        } catch (IOException e) {
            log.error("Erro ao ler cabeçalho do arquivo de foto {}", photoRole, e);
            throw new BusinessException("Falha ao processar arquivo de imagem para " + photoRole + ".");
        }
    }

    private String sanitizeString(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        // Remove tags HTML e espaços sobressalentes
        return HTML_PATTERN.matcher(value).replaceAll("").trim();
    }

    private String sanitizeInstagram(String handle) {
        if (!StringUtils.hasText(handle)) {
            return null;
        }
        String cleaned = HTML_PATTERN.matcher(handle).replaceAll("").trim();
        if (cleaned.startsWith("@")) {
            cleaned = cleaned.substring(1);
        }
        return cleaned.toLowerCase();
    }

    private String cleanFileName(String originalFilename) {
        if (!StringUtils.hasText(originalFilename)) {
            return "photo.jpg";
        }
        return originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    /**
     * Gera protocolo no formato WB-YYYYMMDD-XXXX (4 dígitos hex aleatórios).
     * Ex.: WB-20261006-7A2F
     */
    private String generateProtocol() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomPart = UUID.randomUUID().toString().replace("-", "").substring(0, 4).toUpperCase();
        return String.format("WB-%s-%s", datePart, randomPart);
    }

    /**
     * Persiste UMA foto filha em candidate_photos com a FK candidate_id correta.
     * OBS: Não usamos .candidate(saved) no builder porque CandidatePhoto agora grava a coluna
     *      candidate_id diretamente (UUID simples), evitando conflitos de @ManyToOne.
     */
    private void persistirFotoFilha(CandidateSubmission submission,
                                     String storagePath,
                                     String publicUrl,
                                     String photoType,
                                     int displayOrder,
                                     String originalFileName) {
        CandidatePhoto photo = CandidatePhoto.builder()
                .candidateId(submission.getId())
                .storagePath(storagePath)
                .filePath(storagePath)
                .fileUrl(publicUrl)
                .displayOrder(displayOrder)
                .photoPosition((short) displayOrder)
                .build();
        // photoType não é coluna própria do JPA? A entidade não tem photo_type.
        // A coluna mais próxima que existe no schema é photo_position. Os DTOs do admin usam
        // POLAROID_ROSTO / POLAROID_PERFIL / CORPO_INTEIRO via displayOrder, então tá tudo OK.
        try {
            candidatePhotoRepository.save(photo);
        } catch (Exception e) {
            log.error("[CANDIDATE_PHOTOS] Falha ao salvar foto filha (type={}, order={}, submissionId={}). {}",
                    photoType, displayOrder, submission.getId(), e.getMessage(), e);
            throw e; // força rollback transacional (rollbackFor=Exception.class)
        }
        // originalFileName não persiste (a entidade não tem coluna), mas o log facilita debug.
        log.debug("[CANDIDATE_PHOTOS] Foto persistida: submission={} | type={} | order={} | file={}",
                submission.getId(), photoType, displayOrder, originalFileName);
    }
}

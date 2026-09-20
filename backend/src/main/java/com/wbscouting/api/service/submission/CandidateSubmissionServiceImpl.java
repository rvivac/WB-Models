package com.wbscouting.api.service.submission;

import com.wbscouting.api.dto.CandidateSubmissionRequestDto;
import com.wbscouting.api.dto.CandidateSubmissionResponseDto;
import com.wbscouting.api.entity.CandidateSubmission;
import com.wbscouting.api.enums.SubmissionStatus;
import com.wbscouting.api.exception.BusinessException;
import com.wbscouting.api.repository.CandidateSubmissionRepository;
import com.wbscouting.api.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
@RequiredArgsConstructor
public class CandidateSubmissionServiceImpl implements CandidateSubmissionService {

    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024; // 5 MB
    private static final String DEFAULT_BUCKET = "candidates-uploads";
    private static final Pattern HTML_PATTERN = Pattern.compile("<[^>]*>");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$");

    // Assinaturas mágicas de bytes
    private static final byte[] JPEG_MAGIC = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_MAGIC = new byte[]{(byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

    private final CandidateSubmissionRepository repository;
    private final StorageService storageService;

    @Value("${supabase.buckets.candidates-uploads:candidates-uploads}")
    private String candidatesBucket;

    @Override
    @Transactional
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
        UUID submissionId = UUID.randomUUID();
        String protocol = generateProtocol();
        String bucket = StringUtils.hasText(candidatesBucket) ? candidatesBucket : DEFAULT_BUCKET;

        // 5. Upload dos Arquivos para o Storage
        String facePath = String.format("submissions/%s/face_%s", submissionId, cleanFileName(facePhoto.getOriginalFilename()));
        String profilePath = String.format("submissions/%s/profile_%s", submissionId, cleanFileName(profilePhoto.getOriginalFilename()));
        String fullBodyPath = String.format("submissions/%s/fullbody_%s", submissionId, cleanFileName(fullBodyPhoto.getOriginalFilename()));

        storageService.uploadFile(bucket, facePath, facePhoto);
        storageService.uploadFile(bucket, profilePath, profilePhoto);
        storageService.uploadFile(bucket, fullBodyPath, fullBodyPhoto);

        String faceUrl = storageService.getPublicUrl(bucket, facePath);
        String profileUrl = storageService.getPublicUrl(bucket, profilePath);
        String fullBodyUrl = storageService.getPublicUrl(bucket, fullBodyPath);

        // 6. Construção e Persistência da Entidade
        CandidateSubmission submission = CandidateSubmission.builder()
                .id(submissionId)
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

        return CandidateSubmissionResponseDto.builder()
                .id(saved.getId())
                .protocol(saved.getProtocol())
                .message("Candidatura enviada com sucesso! Nossa equipe de scouting analisará seu material.")
                .status(saved.getStatus())
                .createdAt(saved.getCreatedAt() != null ? saved.getCreatedAt() : OffsetDateTime.now())
                .build();
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

    private void validateImageFile(MultipartFile file, String photoRole) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("O arquivo de foto " + photoRole + " é obrigatório e não pode estar vazio.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("O arquivo de foto " + photoRole + " excede o limite máximo permitido de 5 MB.");
        }

        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equalsIgnoreCase("image/jpeg")
                && !contentType.equalsIgnoreCase("image/jpg")
                && !contentType.equalsIgnoreCase("image/png"))) {
            throw new BusinessException("Tipo de arquivo não permitido para " + photoRole + ". Formatos aceitos: JPEG e PNG.");
        }

        // Validação de integridade do cabeçalho (Magic Bytes)
        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[8];
            int read = is.read(header);
            if (read < 4) {
                throw new BusinessException("Arquivo corrompido ou inválido para " + photoRole + ".");
            }

            boolean isJpeg = header[0] == JPEG_MAGIC[0] && header[1] == JPEG_MAGIC[1] && header[2] == JPEG_MAGIC[2];
            boolean isPng = read >= 8 && Arrays.equals(header, PNG_MAGIC);

            if (!isJpeg && !isPng) {
                log.warn("Tentativa de upload com cabeçalho adulterado para {}. Bytes recebidos: {}", photoRole, Arrays.toString(header));
                throw new BusinessException("Cabeçalho de arquivo adulterado ou inválido para " + photoRole + ". Envie uma imagem válida.");
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

    private String generateProtocol() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomPart = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return String.format("WB-%s-%s", datePart, randomPart);
    }
}

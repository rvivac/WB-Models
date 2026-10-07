package com.wbscouting.api.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wbscouting.api.config.SupabaseProperties;
import com.wbscouting.api.controller.AuthController;
import com.wbscouting.api.controller.admin.AdminApplicationController;
import com.wbscouting.api.controller.publicapi.CandidatePublicController;
import com.wbscouting.api.controller.publicapi.PublicModelController;
import com.wbscouting.api.dto.CandidateSubmissionRequestDto;
import com.wbscouting.api.dto.CandidateSubmissionResponseDto;
import com.wbscouting.api.dto.admin.candidate.CandidateDecisionRequestDto;
import com.wbscouting.api.dto.auth.LoginRequestDto;
import com.wbscouting.api.dto.auth.LoginResponseDto;
import com.wbscouting.api.dto.model.ModelResponseDto;
import com.wbscouting.api.entity.CandidateSubmission;
import com.wbscouting.api.enums.GenderType;
import com.wbscouting.api.enums.SubmissionGender;
import com.wbscouting.api.enums.SubmissionStatus;
import com.wbscouting.api.exception.GlobalExceptionHandler;
import com.wbscouting.api.exception.InvalidFileException;
import com.wbscouting.api.repository.CandidateSubmissionRepository;
import com.wbscouting.api.security.JwtAuthenticationEntryPoint;
import com.wbscouting.api.security.JwtAuthenticationFilter;
import com.wbscouting.api.security.JwtService;
import com.wbscouting.api.security.JwtTokenProvider;
import com.wbscouting.api.service.auth.AuthService;
import com.wbscouting.api.service.model.ModelService;
import com.wbscouting.api.service.storage.StorageService;
import com.wbscouting.api.service.submission.CandidateSubmissionAdminService;
import com.wbscouting.api.service.submission.CandidateSubmissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {
        CandidatePublicController.class,
        AdminApplicationController.class,
        AuthController.class,
        PublicModelController.class
})
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@DisplayName("QA-001 (5.1.1): Teste E2E do Ciclo Completo de Submissão, Triagem, Decisão e Promoção de Candidaturas")
class CandidateSubmissionLifecycleE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // Mocks dos serviços e dependências da aplicação
    @MockitoBean
    private CandidateSubmissionService candidateSubmissionService;

    @MockitoBean
    private CandidateSubmissionAdminService candidateSubmissionAdminService;

    @MockitoBean
    private CandidateSubmissionRepository submissionRepository;

    @MockitoBean
    private StorageService storageService;

    @MockitoBean
    private SupabaseProperties supabaseProperties;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private com.wbscouting.api.service.publicapi.PublicModelService publicModelService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockitoBean
    private com.wbscouting.api.service.audit.AuditLogService auditLogService;

    @MockitoBean
    private com.wbscouting.api.repository.AdminLoginHistoryRepository adminLoginHistoryRepository;

    @MockitoBean
    private com.wbscouting.api.repository.AdminRepository adminRepository;

    // Fixtures de Teste
    private CandidateSubmissionRequestDto validSubmissionDto;
    private CandidateSubmission candidateEntity;
    private CandidateSubmissionResponseDto submissionResponseDto;
    private UUID candidateId;
    private String protocol;
    private byte[] validJpegBytes;

    @BeforeEach
    void setUp() {
        candidateId = UUID.randomUUID();
        protocol = "WB-20260930-A1B2C3";
        validJpegBytes = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 16, 'J', 'F', 'I', 'F'};

        validSubmissionDto = CandidateSubmissionRequestDto.builder()
                .fullName("Isabella Fontana")
                .email("isabella.fontana@models.com")
                .phone("+5511999998888")
                .birthDate(LocalDate.of(2002, 3, 23))
                .gender(SubmissionGender.FEMALE)
                .city("Curitiba")
                .state("PR")
                .height(new BigDecimal("1.77"))
                .bust(new BigDecimal("84.0"))
                .waist(new BigDecimal("60.0"))
                .hips(new BigDecimal("89.0"))
                .shoeSize(38)
                .eyeColor("Verdes")
                .hairColor("Castanho Claro")
                .instagramHandle("@isabellafontana")
                .lgpdConsent(true)
                .build();

        candidateEntity = CandidateSubmission.builder()
                .id(candidateId)
                .protocol(protocol)
                .fullName(validSubmissionDto.getFullName())
                .email(validSubmissionDto.getEmail())
                .phone(validSubmissionDto.getPhone())
                .birthDate(validSubmissionDto.getBirthDate())
                .age(24)
                .gender(validSubmissionDto.getGender())
                .city(validSubmissionDto.getCity())
                .state(validSubmissionDto.getState())
                .height(validSubmissionDto.getHeight())
                .bust(validSubmissionDto.getBust())
                .waist(validSubmissionDto.getWaist())
                .hips(validSubmissionDto.getHips())
                .shoeSize(validSubmissionDto.getShoeSize())
                .eyeColor(validSubmissionDto.getEyeColor())
                .hairColor(validSubmissionDto.getHairColor())
                .instagramHandle(validSubmissionDto.getInstagramHandle())
                .lgpdConsent(true)
                .lgpdConsentAt(OffsetDateTime.now())
                .status(SubmissionStatus.PENDING)
                .facePhotoUrl("https://example.com/storage/face.jpg")
                .profilePhotoUrl("https://example.com/storage/profile.jpg")
                .fullBodyPhotoUrl("https://example.com/storage/body.jpg")
                .createdAt(OffsetDateTime.now())
                .build();

        submissionResponseDto = CandidateSubmissionResponseDto.builder()
                .id(candidateId)
                .protocol(protocol)
                .fullName(validSubmissionDto.getFullName())
                .email(validSubmissionDto.getEmail())
                .phone(validSubmissionDto.getPhone())
                .birthDate(validSubmissionDto.getBirthDate())
                .age(24)
                .gender(validSubmissionDto.getGender())
                .city(validSubmissionDto.getCity())
                .state(validSubmissionDto.getState())
                .height(validSubmissionDto.getHeight())
                .bust(validSubmissionDto.getBust())
                .waist(validSubmissionDto.getWaist())
                .hips(validSubmissionDto.getHips())
                .shoeSize(validSubmissionDto.getShoeSize())
                .eyeColor(validSubmissionDto.getEyeColor())
                .hairColor(validSubmissionDto.getHairColor())
                .instagramHandle(validSubmissionDto.getInstagramHandle())
                .status(SubmissionStatus.PENDING)
                .facePhotoUrl("https://example.com/storage/face.jpg")
                .profilePhotoUrl("https://example.com/storage/profile.jpg")
                .fullBodyPhotoUrl("https://example.com/storage/body.jpg")
                .createdAt(OffsetDateTime.now())
                .message("Candidatura submetida com sucesso.")
                .build();

        SupabaseProperties.Buckets buckets = new SupabaseProperties.Buckets();
        buckets.setCandidatesUploads("candidates-uploads");
        when(supabaseProperties.getBuckets()).thenReturn(buckets);
    }

    // =========================================================================
    // FASE 1: SUBMISSÃO PÚBLICA (PORTAL DO CANDIDATO / SCOUTING DESK)
    // =========================================================================

    @Test
    @DisplayName("TC-01 [Happy Path]: Deve submeter candidatura com dados válidos e 3 fotos, retornando 201 Created e Protocolo")
    void shouldSubmitCandidateSuccessfully() throws Exception {
        MockMultipartFile dataPart = new MockMultipartFile(
                "data", "", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(validSubmissionDto)
        );
        MockMultipartFile facePhoto = new MockMultipartFile("facePhoto", "face.jpg", "image/jpeg", validJpegBytes);
        MockMultipartFile profilePhoto = new MockMultipartFile("profilePhoto", "profile.jpg", "image/jpeg", validJpegBytes);
        MockMultipartFile fullBodyPhoto = new MockMultipartFile("fullBodyPhoto", "body.jpg", "image/jpeg", validJpegBytes);

        when(candidateSubmissionService.submit(any(), any(), any(), any())).thenReturn(submissionResponseDto);

        mockMvc.perform(multipart("/api/v1/submissions")
                        .file(dataPart)
                        .file(facePhoto)
                        .file(profilePhoto)
                        .file(fullBodyPhoto))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(candidateId.toString()))
                .andExpect(jsonPath("$.protocol").value(protocol))
                .andExpect(jsonPath("$.fullName").value("Isabella Fontana"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.facePhotoUrl").isNotEmpty())
                .andExpect(jsonPath("$.profilePhotoUrl").isNotEmpty())
                .andExpect(jsonPath("$.fullBodyPhotoUrl").isNotEmpty());

        verify(candidateSubmissionService, times(1)).submit(any(), any(), any(), any());
    }

    @Test
    @DisplayName("TC-02 [LGPD Compliance]: Deve rejeitar submissão com lgpdConsent = false com 400 Bad Request")
    void shouldRejectSubmissionWithoutLgpdConsent() throws Exception {
        validSubmissionDto.setLgpdConsent(false);
        MockMultipartFile dataPart = new MockMultipartFile(
                "data", "", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(validSubmissionDto)
        );
        MockMultipartFile facePhoto = new MockMultipartFile("facePhoto", "face.jpg", "image/jpeg", validJpegBytes);
        MockMultipartFile profilePhoto = new MockMultipartFile("profilePhoto", "profile.jpg", "image/jpeg", validJpegBytes);
        MockMultipartFile fullBodyPhoto = new MockMultipartFile("fullBodyPhoto", "body.jpg", "image/jpeg", validJpegBytes);

        mockMvc.perform(multipart("/api/v1/submissions")
                        .file(dataPart)
                        .file(facePhoto)
                        .file(profilePhoto)
                        .file(fullBodyPhoto))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Erro de Validação"));

        verifyNoInteractions(candidateSubmissionService);
    }

    @Test
    @DisplayName("TC-03 [Security - CRLF Injection]: Deve rejeitar fullName contendo quebras de linha com 400 Bad Request")
    void shouldRejectSubmissionWithCrlfInFullName() throws Exception {
        validSubmissionDto.setFullName("Isabella\r\nBcc: attacker@evil.com");
        MockMultipartFile dataPart = new MockMultipartFile(
                "data", "", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(validSubmissionDto)
        );
        MockMultipartFile facePhoto = new MockMultipartFile("facePhoto", "face.jpg", "image/jpeg", validJpegBytes);
        MockMultipartFile profilePhoto = new MockMultipartFile("profilePhoto", "profile.jpg", "image/jpeg", validJpegBytes);
        MockMultipartFile fullBodyPhoto = new MockMultipartFile("fullBodyPhoto", "body.jpg", "image/jpeg", validJpegBytes);

        mockMvc.perform(multipart("/api/v1/submissions")
                        .file(dataPart)
                        .file(facePhoto)
                        .file(profilePhoto)
                        .file(fullBodyPhoto))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Erro de Validação"));

        verifyNoInteractions(candidateSubmissionService);
    }

    @Test
    @DisplayName("TC-04 [Security - Polyglot Rejection]: Deve rejeitar upload com magic bytes inválidos com 400 Bad Request")
    void shouldRejectSubmissionWithInvalidMagicBytes() throws Exception {
        MockMultipartFile dataPart = new MockMultipartFile(
                "data", "", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(validSubmissionDto)
        );
        byte[] maliciousBytes = new byte[]{'M', 'Z', (byte) 0x90, 0, 3, 0, 0, 0};
        MockMultipartFile fakeFacePhoto = new MockMultipartFile("facePhoto", "exploit.jpg", "image/jpeg", maliciousBytes);
        MockMultipartFile profilePhoto = new MockMultipartFile("profilePhoto", "profile.jpg", "image/jpeg", validJpegBytes);
        MockMultipartFile fullBodyPhoto = new MockMultipartFile("fullBodyPhoto", "body.jpg", "image/jpeg", validJpegBytes);

        when(candidateSubmissionService.submit(any(), any(), any(), any()))
                .thenThrow(new InvalidFileException("Formato de imagem não suportado. Os bytes do arquivo não correspondem a uma imagem válida."));

        mockMvc.perform(multipart("/api/v1/submissions")
                        .file(dataPart)
                        .file(fakeFacePhoto)
                        .file(profilePhoto)
                        .file(fullBodyPhoto))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Arquivo Inválido"));
    }

    // =========================================================================
    // FASE 2: AUTENTICAÇÃO ADMINISTRATIVA (PAINEL CMS / SCOUTING DESK)
    // =========================================================================

    @Test
    @DisplayName("TC-05 [Admin Login]: Deve autenticar com sucesso e emitir JWT + Cookie HttpOnly")
    void shouldAuthenticateAdminSuccessfully() throws Exception {
        LoginRequestDto loginRequest = new LoginRequestDto("admin@wbscouting.com", "Admin@WbScouting2026!");
        LoginResponseDto loginResponse = LoginResponseDto.builder()
                .accessToken("sample.jwt.token")
                .tokenType("Bearer")
                .expiresIn(28800L)
                .adminEmail("admin@wbscouting.com")
                .adminName("Administrador WB Scouting")
                .build();

        when(authService.login(any(LoginRequestDto.class))).thenReturn(loginResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("sample.jwt.token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.adminEmail").value("admin@wbscouting.com"))
                .andExpect(header().exists(HttpHeaders.SET_COOKIE))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("jwt_token=sample.jwt.token")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("SameSite=Strict")));
    }

    // =========================================================================
    // FASE 3: CONSULTA, TRIAGEM E DETALHAMENTO DE CANDIDATURAS
    // =========================================================================

    @Test
    @DisplayName("TC-06 [Admin Query]: Deve listar candidaturas com paginação e filtros")
    void shouldListApplicationsWithFilters() throws Exception {
        when(submissionRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(candidateEntity)));

        mockMvc.perform(get("/api/v1/admin/applications")
                        .param("page", "0")
                        .param("size", "15")
                        .param("status", "PENDING")
                        .param("sortBy", "createdAt")
                        .param("sortDirection", "DESC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].fullName").value("Isabella Fontana"))
                .andExpect(jsonPath("$.content[0].status").value("PENDING"))
                .andExpect(jsonPath("$.content[0].city").value("Curitiba"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("TC-07 [Admin Metrics]: Deve retornar contadores agregados por status")
    void shouldReturnApplicationCounts() throws Exception {
        when(submissionRepository.countByStatusAndConvertedToModelIdIsNull(SubmissionStatus.PENDING)).thenReturn(12L);
        when(submissionRepository.countByStatusAndConvertedToModelIdIsNull(SubmissionStatus.APPROVED)).thenReturn(3L);
        when(submissionRepository.countByStatusAndConvertedToModelIdIsNull(SubmissionStatus.REJECTED)).thenReturn(5L);
        when(submissionRepository.countByConvertedToModelIdIsNull()).thenReturn(20L);
        when(submissionRepository.countByStatus(SubmissionStatus.PENDING)).thenReturn(12L);
        when(submissionRepository.countByStatus(SubmissionStatus.APPROVED)).thenReturn(3L);
        when(submissionRepository.countByStatus(SubmissionStatus.REJECTED)).thenReturn(5L);
        when(submissionRepository.count()).thenReturn(20L);

        mockMvc.perform(get("/api/v1/admin/applications/counts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pending").value(12))
                .andExpect(jsonPath("$.approved").value(3))
                .andExpect(jsonPath("$.rejected").value(5))
                .andExpect(jsonPath("$.total").value(20));
    }

    @Test
    @DisplayName("TC-08 [Admin Detail]: Deve retornar detalhes completos da candidatura com biometria e fotos")
    void shouldGetApplicationDetails() throws Exception {
        when(submissionRepository.findById(candidateId)).thenReturn(Optional.of(candidateEntity));

        mockMvc.perform(get("/api/v1/admin/applications/{id}", candidateId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(candidateId.toString()))
                .andExpect(jsonPath("$.fullName").value("Isabella Fontana"))
                .andExpect(jsonPath("$.biometrics.height").value(177))
                .andExpect(jsonPath("$.biometrics.bust").value(84.0))
                .andExpect(jsonPath("$.biometrics.waist").value(60.0))
                .andExpect(jsonPath("$.biometrics.hips").value(89.0))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.photos", hasSize(3)));
    }

    // =========================================================================
    // FASE 4: DECISÃO, PROMOÇÃO A MODELO E PURGA LGPD
    // =========================================================================

    @Test
    @DisplayName("TC-09 [Decision Update]: Deve atualizar status da candidatura para APPROVED com parecer técnico")
    void shouldUpdateCandidateDecision() throws Exception {
        CandidateDecisionRequestDto decisionDto = CandidateDecisionRequestDto.builder()
                .status(SubmissionStatus.APPROVED)
                .internalNotes("Perfil comercial e fashion de alto impacto. Aprovado pelo Scouting Desk.")
                .build();

        when(submissionRepository.findById(candidateId)).thenReturn(Optional.of(candidateEntity));
        when(submissionRepository.save(any(CandidateSubmission.class))).thenAnswer(inv -> {
            CandidateSubmission s = inv.getArgument(0);
            s.setStatus(SubmissionStatus.APPROVED);
            s.setFeedbackNotes(decisionDto.getInternalNotes());
            return s;
        });

        mockMvc.perform(patch("/api/v1/admin/applications/{id}/decision", candidateId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(decisionDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.internalNotes").value("Perfil comercial e fashion de alto impacto. Aprovado pelo Scouting Desk."));
    }

    @Test
    @DisplayName("TC-10 [Promotion to Model]: Deve promover candidatura aprovada a Modelo Oficial no casting")
    void shouldPromoteCandidateToOfficialModel() throws Exception {
        UUID officialModelId = UUID.randomUUID();
        ModelResponseDto modelResponse = ModelResponseDto.builder()
                .id(officialModelId)
                .stageName("Isabella Fontana")
                .artisticName("Isabella Fontana")
                .gender(GenderType.FEMALE)
                .heightCm(177)
                .isActive(true)
                .isStar(true)
                .primaryPhotoUrl("https://example.com/storage/face.jpg")
                .build();

        when(candidateSubmissionAdminService.promoteCandidateToModel(eq(candidateId), any(), any()))
                .thenReturn(modelResponse);

        mockMvc.perform(post("/api/v1/admin/applications/{id}/promote", candidateId)
                        .param("activateImmediately", "true"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Candidato promovido a modelo com sucesso."))
                .andExpect(jsonPath("$.data.id").value(officialModelId.toString()))
                .andExpect(jsonPath("$.data.stageName").value("Isabella Fontana"))
                .andExpect(jsonPath("$.data.heightCm").value(177))
                .andExpect(jsonPath("$.data.isActive").value(true))
                .andExpect(jsonPath("$.data.isStar").value(true));

        verify(candidateSubmissionAdminService, times(1))
                .promoteCandidateToModel(eq(candidateId), any(), eq(true));
    }

    @Test
    @DisplayName("TC-11 [Arquivo Morto / Soft Delete]: Deve arquivar candidatura com status ARCHIVED com 204 No Content")
    void shouldPurgeApplicationUnderLgpd() throws Exception {
        when(submissionRepository.findById(candidateId)).thenReturn(Optional.of(candidateEntity));

        mockMvc.perform(delete("/api/v1/admin/applications/{id}", candidateId))
                .andExpect(status().isNoContent());

        verify(submissionRepository, times(1)).save(candidateEntity);
        org.assertj.core.api.Assertions.assertThat(candidateEntity.getStatus()).isEqualTo(com.wbscouting.api.enums.SubmissionStatus.ARCHIVED);
    }
}

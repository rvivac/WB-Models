package com.wbscouting.api.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wbscouting.api.config.SupabaseProperties;
import com.wbscouting.api.dto.admin.candidate.CandidateDecisionRequestDto;
import com.wbscouting.api.entity.CandidateSubmission;
import com.wbscouting.api.enums.SubmissionGender;
import com.wbscouting.api.enums.SubmissionStatus;
import com.wbscouting.api.repository.CandidateSubmissionRepository;
import com.wbscouting.api.security.JwtAuthenticationEntryPoint;
import com.wbscouting.api.security.JwtAuthenticationFilter;
import com.wbscouting.api.security.JwtService;
import com.wbscouting.api.security.JwtTokenProvider;
import com.wbscouting.api.service.storage.StorageService;
import com.wbscouting.api.dto.model.ModelResponseDto;
import com.wbscouting.api.enums.GenderType;
import com.wbscouting.api.exception.DuplicatePromotionException;
import com.wbscouting.api.exception.ResourceNotFoundException;
import com.wbscouting.api.service.submission.CandidateSubmissionAdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminApplicationController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CandidateSubmissionRepository submissionRepository;

    @MockBean
    private CandidateSubmissionAdminService candidateSubmissionAdminService;

    @MockBean
    private StorageService storageService;

    @MockBean
    private SupabaseProperties supabaseProperties;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    private CandidateSubmission submission;
    private UUID submissionId;

    @BeforeEach
    void setUp() {
        submissionId = UUID.randomUUID();
        submission = CandidateSubmission.builder()
                .id(submissionId)
                .protocol("WB-2026-TEST")
                .fullName("Mariana Souza Fagundes")
                .email("mariana.souza@email.com")
                .phone("+55 11 98888-7777")
                .birthDate(LocalDate.of(2008, 5, 14))
                .age(18)
                .gender(SubmissionGender.FEMALE)
                .city("São Paulo")
                .state("SP")
                .height(new BigDecimal("1.78"))
                .bust(new BigDecimal("83.0"))
                .waist(new BigDecimal("59.0"))
                .hips(new BigDecimal("88.0"))
                .shoeSize(37)
                .eyeColor("Castanho Claro")
                .hairColor("Castanho Natural")
                .instagramHandle("@marianasouza")
                .lgpdConsent(true)
                .lgpdConsentAt(OffsetDateTime.now())
                .status(SubmissionStatus.PENDING)
                .facePhotoUrl("https://example.com/face.jpg")
                .profilePhotoUrl("https://example.com/profile.jpg")
                .fullBodyPhotoUrl("https://example.com/body.jpg")
                .createdAt(OffsetDateTime.now())
                .build();

        SupabaseProperties.Buckets buckets = new SupabaseProperties.Buckets();
        when(supabaseProperties.getBuckets()).thenReturn(buckets);
    }

    @Test
    @DisplayName("GET /api/v1/admin/applications deve retornar 200 OK com página de candidaturas")
    void listApplications_ReturnsOk() throws Exception {
        when(submissionRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(submission)));

        mockMvc.perform(get("/api/v1/admin/applications")
                        .param("page", "0")
                        .param("size", "15")
                        .param("sortBy", "createdAt")
                        .param("sortDirection", "DESC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].fullName").value("Mariana Souza Fagundes"))
                .andExpect(jsonPath("$.content[0].height").value(178))
                .andExpect(jsonPath("$.content[0].status").value("PENDING"))
                .andExpect(jsonPath("$.content[0].polaroidsCount").value(3))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/admin/applications/counts deve retornar contadores por status")
    void getCounts_ReturnsOk() throws Exception {
        when(submissionRepository.countByStatus(SubmissionStatus.PENDING)).thenReturn(10L);
        when(submissionRepository.countByStatus(SubmissionStatus.APPROVED)).thenReturn(5L);
        when(submissionRepository.countByStatus(SubmissionStatus.REJECTED)).thenReturn(2L);
        when(submissionRepository.count()).thenReturn(17L);

        mockMvc.perform(get("/api/v1/admin/applications/counts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pending").value(10))
                .andExpect(jsonPath("$.approved").value(5))
                .andExpect(jsonPath("$.total").value(17));
    }

    @Test
    @DisplayName("GET /api/v1/admin/applications/{id} deve retornar 200 OK com detalhes completos")
    void getApplicationById_ReturnsOk() throws Exception {
        when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));

        mockMvc.perform(get("/api/v1/admin/applications/{id}", submissionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(submissionId.toString()))
                .andExpect(jsonPath("$.fullName").value("Mariana Souza Fagundes"))
                .andExpect(jsonPath("$.biometrics.height").value(178))
                .andExpect(jsonPath("$.photos.length()").value(3))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("PATCH /api/v1/admin/applications/{id}/decision deve atualizar status e parecer do scouter")
    void updateDecision_ReturnsOk() throws Exception {
        when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));
        when(submissionRepository.save(any(CandidateSubmission.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CandidateDecisionRequestDto decisionDto = CandidateDecisionRequestDto.builder()
                .status(SubmissionStatus.APPROVED)
                .internalNotes("Excelente desenvoltura editorial e biometria adequada.")
                .build();

        mockMvc.perform(patch("/api/v1/admin/applications/{id}/decision", submissionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(decisionDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.internalNotes").value("Excelente desenvoltura editorial e biometria adequada."));
    }

    @Test
    @DisplayName("DELETE /api/v1/admin/applications/{id} deve purgar arquivos no storage e retornar 204 No Content")
    void deleteApplication_ReturnsNoContent() throws Exception {
        when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));

        mockMvc.perform(delete("/api/v1/admin/applications/{id}", submissionId))
                .andExpect(status().isNoContent());

        verify(storageService, atLeastOnce()).deleteFile(anyString(), anyString());
        verify(submissionRepository).delete(submission);
    }

    @Test
    @DisplayName("POST /api/v1/admin/applications/{id}/promote deve retornar 201 Created com payload do modelo criado")
    void promoteCandidateToModel_ReturnsCreated() throws Exception {
        UUID modelId = UUID.randomUUID();
        ModelResponseDto modelResponse = ModelResponseDto.builder()
                .id(modelId)
                .stageName("Mariana Souza Fagundes")
                .artisticName("Mariana Souza Fagundes")
                .gender(GenderType.FEMALE)
                .heightCm(178)
                .isActive(true)
                .isStar(false)
                .primaryPhotoUrl("https://example.com/face.jpg")
                .build();

        when(candidateSubmissionAdminService.promoteCandidateToModel(eq(submissionId), any(), any()))
                .thenReturn(modelResponse);

        mockMvc.perform(post("/api/v1/admin/applications/{id}/promote", submissionId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Candidato promovido a modelo com sucesso."))
                .andExpect(jsonPath("$.data.id").value(modelId.toString()))
                .andExpect(jsonPath("$.data.stageName").value("Mariana Souza Fagundes"))
                .andExpect(jsonPath("$.data.heightCm").value(178))
                .andExpect(jsonPath("$.data.isActive").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/admin/applications/{id}/promote com ID inexistente deve retornar 404 Not Found")
    void promoteCandidateToModel_WhenNotFound_ReturnsNotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        when(candidateSubmissionAdminService.promoteCandidateToModel(eq(nonExistentId), any(), any()))
                .thenThrow(new ResourceNotFoundException("Candidatura não encontrada com ID: " + nonExistentId));

        mockMvc.perform(post("/api/v1/admin/applications/{id}/promote", nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso Não Encontrado"));
    }

    @Test
    @DisplayName("POST /api/v1/admin/applications/{id}/promote de candidatura já promovida deve retornar 409 Conflict")
    void promoteCandidateToModel_WhenAlreadyPromoted_ReturnsConflict() throws Exception {
        when(candidateSubmissionAdminService.promoteCandidateToModel(eq(submissionId), any(), any()))
                .thenThrow(new DuplicatePromotionException("Esta candidatura já foi promovida ao elenco oficial de modelos."));

        mockMvc.perform(post("/api/v1/admin/applications/{id}/promote", submissionId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Candidatura Já Promovida"));
    }
}

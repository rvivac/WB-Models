package com.wbscouting.api.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wbscouting.api.dto.admin.candidate.*;
import com.wbscouting.api.enums.CandidateStatus;
import com.wbscouting.api.exception.GlobalExceptionHandler;
import com.wbscouting.api.exception.ResourceNotFoundException;
import com.wbscouting.api.security.JwtAuthenticationEntryPoint;
import com.wbscouting.api.security.JwtAuthenticationFilter;
import com.wbscouting.api.security.JwtService;
import com.wbscouting.api.security.JwtTokenProvider;
import com.wbscouting.api.service.candidate.AdminCandidateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminCandidateController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AdminCandidateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdminCandidateService adminCandidateService;

    @MockBean
    private com.wbscouting.api.service.candidate.AdminCandidateQueryService adminCandidateQueryService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    private UUID candidateId;
    private CandidateDetailAdminDto detailDto;
    private CandidateListItemAdminDto itemDto;

    @BeforeEach
    void setUp() {
        candidateId = UUID.randomUUID();

        CandidatePhotoSignedDto photoDto = CandidatePhotoSignedDto.builder()
                .id(UUID.randomUUID())
                .displayOrder(1)
                .signedUrl("https://supabase.co/signed/cand-1/photo1.jpg?token=xyz")
                .expiresInSeconds(900)
                .build();

        itemDto = CandidateListItemAdminDto.builder()
                .id(candidateId)
                .fullName("Camila Queiroz")
                .email("camila@exemplo.com")
                .phone("11988881111")
                .gender("FEMALE")
                .birthDate(LocalDate.of(2005, 5, 20))
                .age(19)
                .isMinor(false)
                .city("Ribeirão Preto")
                .state("SP")
                .heightCm(new BigDecimal("177.00"))
                .status(CandidateStatus.PENDING)
                .photoCount(3)
                .createdAt(OffsetDateTime.now())
                .build();

        detailDto = CandidateDetailAdminDto.builder()
                .id(candidateId)
                .fullName("Camila Queiroz")
                .email("camila@exemplo.com")
                .phone("11988881111")
                .birthDate(LocalDate.of(2005, 5, 20))
                .age(19)
                .isMinor(false)
                .gender("FEMALE")
                .heightCm(new BigDecimal("177.00"))
                .city("Ribeirão Preto")
                .state("SP")
                .status(CandidateStatus.PENDING)
                .internalNotes("Aguardando avaliação da diretoria")
                .photos(List.of(photoDto))
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/admin/candidates - Deve retornar 200 OK com página de candidaturas")
    void listCandidates_ReturnsOkWithPage() throws Exception {
        com.wbscouting.api.dto.common.PageResponseDto<CandidateListItemAdminDto> pageResponse =
                com.wbscouting.api.dto.common.PageResponseDto.from(new PageImpl<>(List.of(itemDto)));

        when(adminCandidateQueryService.listCandidates(eq(CandidateStatus.PENDING), eq("FEMALE"), eq("Camila"), eq(false), eq(0), eq(20)))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/admin/candidates")
                        .param("status", "PENDING")
                        .param("search", "Camila")
                        .param("gender", "FEMALE")
                        .param("isMinor", "false")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(candidateId.toString()))
                .andExpect(jsonPath("$.content[0].fullName").value("Camila Queiroz"))
                .andExpect(jsonPath("$.content[0].status").value("PENDING"))
                .andExpect(jsonPath("$.content[0].photoCount").value(3))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/admin/candidates/{id} - Deve retornar 200 OK com detalhes e fotos assinadas")
    void getCandidateDetail_ReturnsOkWithSignedUrls() throws Exception {
        when(adminCandidateQueryService.getCandidateDetail(candidateId)).thenReturn(detailDto);

        mockMvc.perform(get("/api/v1/admin/candidates/{id}", candidateId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(candidateId.toString()))
                .andExpect(jsonPath("$.fullName").value("Camila Queiroz"))
                .andExpect(jsonPath("$.photos[0].signedUrl").value("https://supabase.co/signed/cand-1/photo1.jpg?token=xyz"))
                .andExpect(jsonPath("$.photos[0].expiresInSeconds").value(900));
    }

    @Test
    @DisplayName("GET /api/v1/admin/candidates/{id} - Deve retornar 404 Problem Details quando não encontrado")
    void getCandidateDetail_NotFound_ReturnsProblemDetails404() throws Exception {
        when(adminCandidateQueryService.getCandidateDetail(candidateId))
                .thenThrow(new ResourceNotFoundException("Candidatura não encontrada com ID: " + candidateId));

        mockMvc.perform(get("/api/v1/admin/candidates/{id}", candidateId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso Não Encontrado"))
                .andExpect(jsonPath("$.detail").value("Candidatura não encontrada com ID: " + candidateId))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("PATCH /api/v1/admin/candidates/{id}/status - Deve atualizar status e retornar 200 OK")
    void updateStatus_ReturnsOk() throws Exception {
        CandidateStatusUpdateDto request = CandidateStatusUpdateDto.builder()
                .status(CandidateStatus.APPROVED)
                .build();

        detailDto.setStatus(CandidateStatus.APPROVED);
        when(adminCandidateService.updateStatus(eq(candidateId), any(CandidateStatusUpdateDto.class)))
                .thenReturn(detailDto);

        mockMvc.perform(patch("/api/v1/admin/candidates/{id}/status", candidateId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(candidateId.toString()))
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    @DisplayName("PATCH /api/v1/admin/candidates/{id}/status - Deve retornar 400 Bad Request quando status for nulo")
    void updateStatus_InvalidStatus_ReturnsBadRequest() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/candidates/{id}/status", candidateId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": null}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /api/v1/admin/candidates/{id}/notes - Deve atualizar notas internas e retornar 200 OK")
    void updateNotes_ReturnsOk() throws Exception {
        CandidateNotesUpdateDto request = CandidateNotesUpdateDto.builder()
                .notes("Convidada para avaliação presencial")
                .build();

        detailDto.setInternalNotes("Convidada para avaliação presencial");
        when(adminCandidateService.updateNotes(eq(candidateId), any(CandidateNotesUpdateDto.class)))
                .thenReturn(detailDto);

        mockMvc.perform(patch("/api/v1/admin/candidates/{id}/notes", candidateId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(candidateId.toString()))
                .andExpect(jsonPath("$.internalNotes").value("Convidada para avaliação presencial"));
    }

    @Test
    @DisplayName("DELETE /api/v1/admin/candidates/{id} - Deve excluir candidatura e retornar 204 No Content")
    void deleteCandidate_ReturnsNoContent() throws Exception {
        doNothing().when(adminCandidateQueryService).deleteCandidate(candidateId);

        mockMvc.perform(delete("/api/v1/admin/candidates/{id}", candidateId))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /admin/candidates - Deve responder com 200 OK na rota sem prefixo /api/v1")
    void listCandidates_WithPathAlias_ReturnsOk() throws Exception {
        com.wbscouting.api.dto.common.PageResponseDto<CandidateListItemAdminDto> pageResponse =
                com.wbscouting.api.dto.common.PageResponseDto.from(new PageImpl<>(List.of(itemDto)));

        when(adminCandidateQueryService.listCandidates(any(), any(), any(), any(), eq(0), eq(20)))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/admin/candidates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(candidateId.toString()));
    }
}

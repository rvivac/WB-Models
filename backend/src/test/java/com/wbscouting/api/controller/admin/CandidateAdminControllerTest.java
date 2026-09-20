package com.wbscouting.api.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wbscouting.api.dto.CandidateSubmissionResponseDto;
import com.wbscouting.api.dto.submission.CandidateStatusUpdateDto;
import com.wbscouting.api.enums.SubmissionGender;
import com.wbscouting.api.enums.SubmissionStatus;
import com.wbscouting.api.security.JwtAuthenticationEntryPoint;
import com.wbscouting.api.security.JwtAuthenticationFilter;
import com.wbscouting.api.security.JwtService;
import com.wbscouting.api.security.JwtTokenProvider;
import com.wbscouting.api.service.submission.CandidateSubmissionAdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CandidateAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
class CandidateAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CandidateSubmissionAdminService adminService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    private UUID sampleId;
    private CandidateSubmissionResponseDto sampleDto;

    @BeforeEach
    void setUp() {
        sampleId = UUID.randomUUID();
        sampleDto = CandidateSubmissionResponseDto.builder()
                .id(sampleId)
                .protocol("WB-20260920-XYZ999")
                .fullName("Isabeli Fontana")
                .email("isabeli@models.com")
                .phone("+5541999998888")
                .birthDate(LocalDate.of(2001, 7, 4))
                .age(25)
                .gender(SubmissionGender.FEMALE)
                .city("Curitiba")
                .state("PR")
                .height(new BigDecimal("1.77"))
                .bust(new BigDecimal("86"))
                .waist(new BigDecimal("60"))
                .hips(new BigDecimal("90"))
                .status(SubmissionStatus.PENDING)
                .facePhotoUrl("https://storage.supabase.co/face.jpg")
                .profilePhotoUrl("https://storage.supabase.co/profile.jpg")
                .fullBodyPhotoUrl("https://storage.supabase.co/body.jpg")
                .createdAt(OffsetDateTime.now())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/admin/submissions - Deve retornar página de candidaturas com 200 OK")
    void shouldListSubmissions() throws Exception {
        when(adminService.listSubmissions(any(), any()))
                .thenReturn(new PageImpl<>(List.of(sampleDto), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/admin/submissions")
                        .param("page", "0")
                        .param("size", "20")
                        .param("search", "Isabeli")
                        .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(sampleId.toString()))
                .andExpect(jsonPath("$.content[0].fullName").value("Isabeli Fontana"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/admin/submissions/{id} - Deve retornar dossiê da candidatura")
    void shouldGetSubmissionById() throws Exception {
        when(adminService.getSubmissionById(sampleId)).thenReturn(sampleDto);

        mockMvc.perform(get("/api/v1/admin/submissions/{id}", sampleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sampleId.toString()))
                .andExpect(jsonPath("$.protocol").value("WB-20260920-XYZ999"))
                .andExpect(jsonPath("$.facePhotoUrl").isNotEmpty());
    }

    @Test
    @DisplayName("PATCH /api/v1/admin/submissions/{id}/status - Deve aprovar candidatura e retornar 200 OK")
    void shouldUpdateSubmissionStatus() throws Exception {
        CandidateStatusUpdateDto updateDto = CandidateStatusUpdateDto.builder()
                .status(SubmissionStatus.APPROVED)
                .feedbackNotes("Perfil excelente para editorial e passarela.")
                .build();

        CandidateSubmissionResponseDto approvedDto = sampleDto;
        approvedDto.setStatus(SubmissionStatus.APPROVED);
        approvedDto.setReviewedBy("Admin Test");
        approvedDto.setFeedbackNotes(updateDto.getFeedbackNotes());

        when(adminService.updateSubmissionStatus(eq(sampleId), any(), any()))
                .thenReturn(approvedDto);

        mockMvc.perform(patch("/api/v1/admin/submissions/{id}/status", sampleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.feedbackNotes").value("Perfil excelente para editorial e passarela."));
    }

    @Test
    @DisplayName("PATCH /api/v1/admin/submissions/{id}/status - Deve rejeitar payload sem status com 400 Bad Request")
    void shouldRejectEmptyStatus() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/submissions/{id}/status", sampleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"feedbackNotes\": \"Sem status\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Erro de Validação"));
    }
}

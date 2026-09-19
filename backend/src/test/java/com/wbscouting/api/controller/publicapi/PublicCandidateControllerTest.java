package com.wbscouting.api.controller.publicapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wbscouting.api.dto.candidate.CandidateApplyRequestDto;
import com.wbscouting.api.dto.candidate.CandidateApplyResponseDto;
import com.wbscouting.api.enums.GenderType;
import com.wbscouting.api.exception.InvalidApplicationException;
import com.wbscouting.api.security.JwtAuthenticationEntryPoint;
import com.wbscouting.api.security.JwtAuthenticationFilter;
import com.wbscouting.api.security.JwtService;
import com.wbscouting.api.security.JwtTokenProvider;
import com.wbscouting.api.service.candidate.CandidateApplicationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PublicCandidateController.class)
@AutoConfigureMockMvc(addFilters = false)
class PublicCandidateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CandidateApplicationService candidateApplicationService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    private CandidateApplyRequestDto validDto;

    @BeforeEach
    void setUp() {
        validDto = CandidateApplyRequestDto.builder()
                .fullName("Camila Queiroz")
                .email("camila@queiroz.com")
                .phone("(11) 98888-7777")
                .birthDate(LocalDate.of(2000, 5, 20))
                .gender(GenderType.FEMALE)
                .heightCm(177)
                .city("Ribeirão Preto")
                .state("SP")
                .bustChestCm(new BigDecimal("85.00"))
                .waistCm(new BigDecimal("61.00"))
                .hipsCm(new BigDecimal("90.00"))
                .shoeSize("37")
                .dressSize("36")
                .instagramHandle("@camilaqueiroz")
                .build();
    }

    @Test
    @DisplayName("POST /public/candidates/apply - Deve retornar 201 Created com protocolo quando requisição for válida")
    void shouldApplySuccessfully() throws Exception {
        UUID candidateId = UUID.randomUUID();
        CandidateApplyResponseDto responseDto = CandidateApplyResponseDto.builder()
                .candidateId(candidateId)
                .message("Candidatura recebida com sucesso.")
                .submittedAt(Instant.now())
                .build();

        when(candidateApplicationService.apply(any(), any())).thenReturn(responseDto);

        byte[] candidateJson = objectMapper.writeValueAsBytes(validDto);
        MockMultipartFile candidatePart = new MockMultipartFile(
                "candidate",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                candidateJson
        );

        MockMultipartFile photo1 = new MockMultipartFile("photos", "p1.jpg", "image/jpeg", "image-content-1".getBytes());
        MockMultipartFile photo2 = new MockMultipartFile("photos", "p2.jpg", "image/jpeg", "image-content-2".getBytes());
        MockMultipartFile photo3 = new MockMultipartFile("photos", "p3.jpg", "image/jpeg", "image-content-3".getBytes());

        mockMvc.perform(multipart("/public/candidates/apply")
                        .file(candidatePart)
                        .file(photo1)
                        .file(photo2)
                        .file(photo3))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.candidateId").value(candidateId.toString()))
                .andExpect(jsonPath("$.message").value("Candidatura recebida com sucesso."))
                .andExpect(jsonPath("$.submittedAt").exists());
    }

    @Test
    @DisplayName("POST /public/candidates/apply - Deve retornar 400 Bad Request quando Bean Validation falhar")
    void shouldReturnBadRequestWhenBeanValidationFails() throws Exception {
        CandidateApplyRequestDto invalidDto = CandidateApplyRequestDto.builder()
                .fullName("Ab") // menor que 3 chars
                .email("email-invalido") // email inválido
                .phone("") // telefone vazio
                .birthDate(LocalDate.now().plusDays(10)) // data futura
                .gender(null) // nulo
                .heightCm(250) // maior que 220
                .city("")
                .state("")
                .build();

        byte[] candidateJson = objectMapper.writeValueAsBytes(invalidDto);
        MockMultipartFile candidatePart = new MockMultipartFile(
                "candidate",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                candidateJson
        );

        MockMultipartFile photo1 = new MockMultipartFile("photos", "p1.jpg", "image/jpeg", "content".getBytes());

        mockMvc.perform(multipart("/public/candidates/apply")
                        .file(candidatePart)
                        .file(photo1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Erro de Validação"))
                .andExpect(jsonPath("$.errors").isMap());
    }

    @Test
    @DisplayName("POST /public/candidates/apply - Deve retornar 400 Bad Request quando serviço lançar InvalidApplicationException")
    void shouldReturnBadRequestWhenInvalidApplicationExceptionThrown() throws Exception {
        when(candidateApplicationService.apply(any(), any()))
                .thenThrow(new InvalidApplicationException("A submissão de candidatura requer o envio de no mínimo 3 e no máximo 6 fotos. Quantidade enviada: 1"));

        byte[] candidateJson = objectMapper.writeValueAsBytes(validDto);
        MockMultipartFile candidatePart = new MockMultipartFile(
                "candidate",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                candidateJson
        );

        MockMultipartFile photo1 = new MockMultipartFile("photos", "p1.jpg", "image/jpeg", "content".getBytes());

        mockMvc.perform(multipart("/public/candidates/apply")
                        .file(candidatePart)
                        .file(photo1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Candidatura Inválida"))
                .andExpect(jsonPath("$.detail").value("A submissão de candidatura requer o envio de no mínimo 3 e no máximo 6 fotos. Quantidade enviada: 1"));
    }
}

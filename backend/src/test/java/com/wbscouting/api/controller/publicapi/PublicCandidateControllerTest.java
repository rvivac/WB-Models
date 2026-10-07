package com.wbscouting.api.controller.publicapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wbscouting.api.dto.CandidateSubmissionRequestDto;
import com.wbscouting.api.dto.CandidateSubmissionResponseDto;
import com.wbscouting.api.enums.SubmissionGender;
import com.wbscouting.api.enums.SubmissionStatus;
import com.wbscouting.api.security.JwtAuthenticationEntryPoint;
import com.wbscouting.api.security.JwtAuthenticationFilter;
import com.wbscouting.api.security.JwtService;
import com.wbscouting.api.security.JwtTokenProvider;
import com.wbscouting.api.service.submission.CandidateSubmissionService;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PublicCandidateController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(PublicCandidateControllerTest.TestConfig.class)
class PublicCandidateControllerTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        public Validator validator() {
            return Validation.buildDefaultValidatorFactory().getValidator();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CandidateSubmissionService candidateSubmissionService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    private CandidateSubmissionRequestDto validDto;

    @BeforeEach
    void setUp() {
        validDto = CandidateSubmissionRequestDto.builder()
                .fullName("Camila Queiroz")
                .email("camila@queiroz.com")
                .phone("(11) 98888-7777")
                .birthDate(LocalDate.of(2000, 5, 20))
                .gender(SubmissionGender.FEMALE)
                .height(new BigDecimal("1.77"))
                .city("Ribeirão Preto")
                .state("SP")
                .bust(new BigDecimal("85.00"))
                .waist(new BigDecimal("61.00"))
                .hips(new BigDecimal("90.00"))
                .shoeSize(37)
                .instagramHandle("camilaqueiroz")
                .lgpdConsent(true)
                .build();
    }

    @Test
    @DisplayName("POST /public/candidates/apply - Deve retornar 201 Created quando requisição for válida")
    void shouldApplySuccessfully() throws Exception {
        UUID candidateId = UUID.randomUUID();
        CandidateSubmissionResponseDto responseDto = CandidateSubmissionResponseDto.builder()
                .id(candidateId)
                .protocol("WB-20261007-ABCDEF")
                .message("Candidatura enviada com sucesso!")
                .status(SubmissionStatus.PENDING)
                .createdAt(OffsetDateTime.now())
                .build();

        when(candidateSubmissionService.submit(any(), any(), any(), any())).thenReturn(responseDto);

        String candidateJson = objectMapper.writeValueAsString(validDto);
        MockMultipartFile dataPart = new MockMultipartFile(
                "data",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                candidateJson.getBytes()
        );

        MockMultipartFile photo1 = new MockMultipartFile("facePhoto", "p1.jpg", "image/jpeg", "image-content-1".getBytes());
        MockMultipartFile photo2 = new MockMultipartFile("profilePhoto", "p2.jpg", "image/jpeg", "image-content-2".getBytes());
        MockMultipartFile photo3 = new MockMultipartFile("fullBodyPhoto", "p3.jpg", "image/jpeg", "image-content-3".getBytes());

        mockMvc.perform(multipart("/public/candidates/apply")
                        .file(dataPart)
                        .file(photo1)
                        .file(photo2)
                        .file(photo3))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(candidateId.toString()))
                .andExpect(jsonPath("$.data.protocol").value("WB-20261007-ABCDEF"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    @DisplayName("POST /public/candidates/apply - Deve retornar 400 Bad Request quando Bean Validation falhar")
    void shouldReturnBadRequestWhenBeanValidationFails() throws Exception {
        CandidateSubmissionRequestDto invalidDto = CandidateSubmissionRequestDto.builder()
                .fullName("Ab") // menor que 3 chars
                .email("email-invalido") // email inválido
                .phone("") // telefone vazio
                .birthDate(LocalDate.now().plusDays(10)) // data futura
                .gender(null) // nulo
                .height(new BigDecimal("2.50")) // maior que 2.30
                .city("")
                .state("")
                .build();

        String candidateJson = objectMapper.writeValueAsString(invalidDto);
        MockMultipartFile dataPart = new MockMultipartFile(
                "data",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                candidateJson.getBytes()
        );

        mockMvc.perform(multipart("/public/candidates/apply")
                        .file(dataPart))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /public/candidates/apply - Deve retornar 400 Bad Request quando JSON for inválido")
    void shouldReturnBadRequestWhenJsonIsInvalid() throws Exception {
        MockMultipartFile invalidDataPart = new MockMultipartFile(
                "data",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                "{invalid-json}".getBytes()
        );

        mockMvc.perform(multipart("/public/candidates/apply")
                        .file(invalidDataPart))
                .andExpect(status().isBadRequest());
    }
}

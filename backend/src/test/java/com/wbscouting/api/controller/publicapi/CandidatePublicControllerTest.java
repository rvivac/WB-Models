package com.wbscouting.api.controller.publicapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wbscouting.api.dto.CandidateSubmissionRequestDto;
import com.wbscouting.api.dto.CandidateSubmissionResponseDto;
import com.wbscouting.api.enums.SubmissionGender;
import com.wbscouting.api.enums.SubmissionStatus;
import com.wbscouting.api.exception.BusinessException;
import com.wbscouting.api.security.JwtAuthenticationEntryPoint;
import com.wbscouting.api.security.JwtAuthenticationFilter;
import com.wbscouting.api.security.JwtService;
import com.wbscouting.api.security.JwtTokenProvider;
import com.wbscouting.api.service.submission.CandidateSubmissionService;
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
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CandidatePublicController.class)
@AutoConfigureMockMvc(addFilters = false)
class CandidatePublicControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CandidateSubmissionService submissionService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    private CandidateSubmissionRequestDto validDto;
    private MockMultipartFile dataPart;
    private MockMultipartFile facePhoto;
    private MockMultipartFile profilePhoto;
    private MockMultipartFile fullBodyPhoto;

    @BeforeEach
    void setUp() throws Exception {
        validDto = CandidateSubmissionRequestDto.builder()
                .fullName("Gisele Bundchen")
                .email("gisele@models.com")
                .phone("+5511999998888")
                .birthDate(LocalDate.of(2000, 7, 20))
                .gender(SubmissionGender.FEMALE)
                .city("Horizontina")
                .state("RS")
                .height(new BigDecimal("1.80"))
                .bust(new BigDecimal("86"))
                .waist(new BigDecimal("60"))
                .hips(new BigDecimal("89"))
                .shoeSize(38)
                .eyeColor("Azul")
                .hairColor("Castanho Claro")
                .instagramHandle("gisele")
                .lgpdConsent(true)
                .build();

        byte[] jpegBytes = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 16, 'J', 'F', 'I', 'F'};

        dataPart = new MockMultipartFile(
                "data",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(validDto)
        );

        facePhoto = new MockMultipartFile("facePhoto", "face.jpg", "image/jpeg", jpegBytes);
        profilePhoto = new MockMultipartFile("profilePhoto", "profile.jpg", "image/jpeg", jpegBytes);
        fullBodyPhoto = new MockMultipartFile("fullBodyPhoto", "body.jpg", "image/jpeg", jpegBytes);
    }

    @Test
    @DisplayName("POST /api/v1/submissions - Deve retornar 201 Created quando o payload e fotos forem válidos")
    void shouldReturn201WhenValid() throws Exception {
        UUID generatedId = UUID.randomUUID();
        CandidateSubmissionResponseDto responseDto = CandidateSubmissionResponseDto.builder()
                .id(generatedId)
                .protocol("WB-20260920-A1B2C3")
                .message("Candidatura enviada com sucesso!")
                .status(SubmissionStatus.PENDING)
                .createdAt(OffsetDateTime.now())
                .build();

        when(submissionService.submit(any(), any(), any(), any())).thenReturn(responseDto);

        mockMvc.perform(multipart("/api/v1/submissions")
                        .file(dataPart)
                        .file(facePhoto)
                        .file(profilePhoto)
                        .file(fullBodyPhoto))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(generatedId.toString()))
                .andExpect(jsonPath("$.protocol").value("WB-20260920-A1B2C3"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("POST /api/v1/submissions - Deve retornar 400 Bad Request quando Bean Validation falhar")
    void shouldReturn400WhenBeanValidationFails() throws Exception {
        validDto.setEmail("email-invalido");
        MockMultipartFile invalidDataPart = new MockMultipartFile(
                "data",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(validDto)
        );

        mockMvc.perform(multipart("/api/v1/submissions")
                        .file(invalidDataPart)
                        .file(facePhoto)
                        .file(profilePhoto)
                        .file(fullBodyPhoto))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Erro de Validação"));
    }

    @Test
    @DisplayName("POST /api/v1/submissions - Deve retornar 400 Bad Request quando serviço lançar BusinessException")
    void shouldReturn400WhenBusinessExceptionThrown() throws Exception {
        when(submissionService.submit(any(), any(), any(), any()))
                .thenThrow(new BusinessException("Para candidatos menores de 18 anos, os dados do responsável legal são obrigatórios."));
        mockMvc.perform(multipart("/api/v1/submissions")
                        .file(dataPart)
                        .file(facePhoto)
                        .file(profilePhoto)
                        .file(fullBodyPhoto))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Regra de Negócio Violada"))
                .andExpect(jsonPath("$.detail").value("Para candidatos menores de 18 anos, os dados do responsável legal são obrigatórios."));
    }

    @Test
    @DisplayName("POST /api/v1/submissions - Deve retornar status específico de StorageException sem mascarar como 502 genérico")
    void shouldReturnSpecificStatusOnStorageException() throws Exception {
        when(submissionService.submit(any(), any(), any(), any()))
                .thenThrow(new com.wbscouting.api.exception.StorageException(
                        "Falha de autenticação com Supabase Storage",
                        org.springframework.http.HttpStatus.UNAUTHORIZED,
                        "STORAGE_AUTH_FAILED",
                        "Falha de Autenticação com Armazenamento"
                ));

        mockMvc.perform(multipart("/api/v1/submissions")
                        .file(dataPart)
                        .file(facePhoto)
                        .file(profilePhoto)
                        .file(fullBodyPhoto))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Falha de Autenticação com Armazenamento"))
                .andExpect(jsonPath("$.storageErrorCode").value("STORAGE_AUTH_FAILED"));
    }
}

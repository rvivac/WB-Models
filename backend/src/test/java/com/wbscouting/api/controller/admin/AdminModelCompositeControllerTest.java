package com.wbscouting.api.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wbscouting.api.dto.media.ModelCompositeResponseDto;
import com.wbscouting.api.security.JwtAuthenticationEntryPoint;
import com.wbscouting.api.security.JwtAuthenticationFilter;
import com.wbscouting.api.security.JwtService;
import com.wbscouting.api.security.JwtTokenProvider;
import com.wbscouting.api.service.media.ModelMediaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminModelCompositeController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminModelCompositeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ModelMediaService modelMediaService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    private UUID modelId;
    private UUID compositeId;
    private ModelCompositeResponseDto compositeDto;

    @BeforeEach
    void setUp() {
        modelId = UUID.randomUUID();
        compositeId = UUID.randomUUID();

        compositeDto = ModelCompositeResponseDto.builder()
                .id(compositeId)
                .fileUrl("https://supabase.co/models-media/models/" + modelId + "/composite/composite.pdf")
                .fileName("composite-isabella-viana.pdf")
                .fileType("PDF")
                .fileSizeBytes(4512300L)
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    @Test
    @DisplayName("GET /admin/models/{modelId}/composite deve retornar composite ativo e 200 OK")
    void getComposite_ReturnsOk() throws Exception {
        when(modelMediaService.getComposite(modelId)).thenReturn(compositeDto);

        mockMvc.perform(get("/admin/models/{modelId}/composite", modelId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(compositeId.toString()))
                .andExpect(jsonPath("$.fileName").value("composite-isabella-viana.pdf"))
                .andExpect(jsonPath("$.fileType").value("PDF"))
                .andExpect(jsonPath("$.fileSizeBytes").value(4512300L));
    }

    @Test
    @DisplayName("PUT /admin/models/{modelId}/composite deve fazer upload/substituição e retornar 200 OK")
    void uploadOrReplaceComposite_ReturnsOk() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "composite.pdf", "application/pdf", "pdf-data".getBytes());

        when(modelMediaService.uploadOrReplaceComposite(eq(modelId), any())).thenReturn(compositeDto);

        mockMvc.perform(multipart("/admin/models/{modelId}/composite", modelId)
                        .file(file)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(compositeId.toString()))
                .andExpect(jsonPath("$.fileType").value("PDF"));
    }

    @Test
    @DisplayName("DELETE /admin/models/{modelId}/composite deve remover composite e retornar 204 No Content")
    void deleteComposite_ReturnsNoContent() throws Exception {
        doNothing().when(modelMediaService).deleteComposite(modelId);

        mockMvc.perform(delete("/admin/models/{modelId}/composite", modelId))
                .andExpect(status().isNoContent());
    }
}

package com.wbscouting.api.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wbscouting.api.dto.media.MediaOrderItemDto;
import com.wbscouting.api.dto.media.MediaReorderRequestDto;
import com.wbscouting.api.dto.media.MediaUploadResponseDto;
import com.wbscouting.api.enums.MediaType;
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
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminModelMediaController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminModelMediaControllerTest {

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
    private UUID mediaId;
    private MediaUploadResponseDto responseDto;

    @BeforeEach
    void setUp() {
        modelId = UUID.randomUUID();
        mediaId = UUID.randomUUID();

        responseDto = MediaUploadResponseDto.builder()
                .id(mediaId)
                .modelId(modelId)
                .mediaType(MediaType.BOOK)
                .fileUrl("https://supabase.co/models-media/test.jpg")
                .storagePath("models-media/test.jpg")
                .displayOrder(1)
                .isCover(true)
                .createdAt(OffsetDateTime.now())
                .build();
    }

    @Test
    @DisplayName("POST /admin/models/{modelId}/media deve realizar upload e retornar 201 Created")
    void uploadMedia_ReturnsCreated() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", "content".getBytes());

        when(modelMediaService.uploadMedia(eq(modelId), eq(MediaType.BOOK), eq(true), any()))
                .thenReturn(responseDto);

        mockMvc.perform(multipart("/admin/models/{modelId}/media", modelId)
                        .file(file)
                        .param("mediaType", "BOOK")
                        .param("isCover", "true"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(mediaId.toString()))
                .andExpect(jsonPath("$.mediaType").value("BOOK"))
                .andExpect(jsonPath("$.isCover").value(true))
                .andExpect(jsonPath("$.fileUrl").value("https://supabase.co/models-media/test.jpg"));
    }

    @Test
    @DisplayName("PUT /admin/models/{modelId}/media/reorder deve retornar 204 No Content")
    void reorderMedia_ReturnsNoContent() throws Exception {
        MediaReorderRequestDto request = MediaReorderRequestDto.builder()
                .items(List.of(MediaOrderItemDto.builder().mediaId(mediaId).displayOrder(1).build()))
                .build();

        doNothing().when(modelMediaService).reorderMedia(eq(modelId), any(MediaReorderRequestDto.class));

        mockMvc.perform(put("/admin/models/{modelId}/media/reorder", modelId)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("PATCH /admin/models/{modelId}/media/{mediaId}/cover deve retornar 204 No Content")
    void setCoverMedia_ReturnsNoContent() throws Exception {
        doNothing().when(modelMediaService).setCoverMedia(modelId, mediaId);

        mockMvc.perform(patch("/admin/models/{modelId}/media/{mediaId}/cover", modelId, mediaId))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /admin/models/{modelId}/media/{mediaId} deve retornar 204 No Content")
    void deleteMedia_ReturnsNoContent() throws Exception {
        doNothing().when(modelMediaService).deleteMedia(modelId, mediaId);

        mockMvc.perform(delete("/admin/models/{modelId}/media/{mediaId}", modelId, mediaId))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /admin/models/{modelId}/media deve listar mídias e retornar 200 OK")
    void listMedia_ReturnsOk() throws Exception {
        when(modelMediaService.listModelMedia(modelId)).thenReturn(List.of(responseDto));

        mockMvc.perform(get("/admin/models/{modelId}/media", modelId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(mediaId.toString()))
                .andExpect(jsonPath("$[0].mediaType").value("BOOK"))
                .andExpect(jsonPath("$[0].isCover").value(true));
    }
}

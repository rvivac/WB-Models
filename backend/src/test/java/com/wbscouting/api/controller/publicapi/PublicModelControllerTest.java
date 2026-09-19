package com.wbscouting.api.controller.publicapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wbscouting.api.dto.publicapi.ModelCardPublicDto;
import com.wbscouting.api.enums.GenderType;
import com.wbscouting.api.security.JwtAuthenticationEntryPoint;
import com.wbscouting.api.security.JwtAuthenticationFilter;
import com.wbscouting.api.security.JwtService;
import com.wbscouting.api.security.JwtTokenProvider;
import com.wbscouting.api.service.publicapi.PublicModelService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PublicModelController.class)
@AutoConfigureMockMvc(addFilters = false)
class PublicModelControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PublicModelService publicModelService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    private UUID modelId;
    private ModelCardPublicDto cardDto;

    @BeforeEach
    void setUp() {
        modelId = UUID.randomUUID();

        cardDto = ModelCardPublicDto.builder()
                .id(modelId)
                .stageName("Gisele Bündchen")
                .gender(GenderType.FEMALE)
                .coverImageUrl("https://supabase.co/models-media/cover.jpg")
                .heightCm(180)
                .city("Horizontina")
                .isStar(true)
                .build();
    }

    @Test
    @DisplayName("GET /public/models/featured deve retornar 200 OK com cabeçalho de cache e lista de destaques")
    void getFeaturedModels_ReturnsOkWithCacheHeader() throws Exception {
        when(publicModelService.getFeaturedModels()).thenReturn(List.of(cardDto));

        mockMvc.perform(get("/public/models/featured"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("max-age=60")))
                .andExpect(jsonPath("$[0].id").value(modelId.toString()))
                .andExpect(jsonPath("$[0].stageName").value("Gisele Bündchen"))
                .andExpect(jsonPath("$[0].isStar").value(true));
    }
}

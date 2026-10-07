package com.wbscouting.api.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wbscouting.api.dto.model.FeaturedModelOrderItemDto;
import com.wbscouting.api.dto.model.FeaturedModelResponseDto;
import com.wbscouting.api.dto.model.FeaturedModelsReorderRequestDto;
import com.wbscouting.api.security.JwtAuthenticationEntryPoint;
import com.wbscouting.api.security.JwtAuthenticationFilter;
import com.wbscouting.api.security.JwtService;
import com.wbscouting.api.security.JwtTokenProvider;
import com.wbscouting.api.service.model.FeaturedModelService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminFeaturedModelsController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminFeaturedModelsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FeaturedModelService featuredModelService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    private UUID modelId1;
    private UUID modelId2;
    private UUID modelId3;
    private UUID modelId4;
    private List<FeaturedModelResponseDto> mockList;

    @BeforeEach
    void setUp() {
        modelId1 = UUID.randomUUID();
        modelId2 = UUID.randomUUID();
        modelId3 = UUID.randomUUID();
        modelId4 = UUID.randomUUID();

        mockList = List.of(
                FeaturedModelResponseDto.builder()
                        .id(modelId1)
                        .artisticName("Isabella Viana")
                        .category("FASHION")
                        .height(179)
                        .isStar(true)
                        .coverPhotoUrl("https://example.com/isabella.jpg")
                        .displayOrder(1)
                        .build(),
                FeaturedModelResponseDto.builder()
                        .id(modelId2)
                        .artisticName("Lucas Albuquerque")
                        .category("COMMERCIAL")
                        .height(188)
                        .isStar(false)
                        .coverPhotoUrl("https://example.com/lucas.jpg")
                        .displayOrder(2)
                        .build(),
                FeaturedModelResponseDto.builder()
                        .id(modelId3)
                        .artisticName("Beatriz Zanin")
                        .category("FASHION")
                        .height(177)
                        .isStar(true)
                        .coverPhotoUrl("https://example.com/beatriz.jpg")
                        .displayOrder(3)
                        .build(),
                FeaturedModelResponseDto.builder()
                        .id(modelId4)
                        .artisticName("Gabriel Siqueira")
                        .category("COMMERCIAL")
                        .height(186)
                        .isStar(false)
                        .coverPhotoUrl("https://example.com/gabriel.jpg")
                        .displayOrder(4)
                        .build()
        );
    }

    @Test
    @DisplayName("GET /admin/featured-models deve retornar lista ordenada de destaques e status 200 OK")
    void getFeaturedHomeModels_ReturnsOk() throws Exception {
        when(featuredModelService.getFeaturedHomeModels()).thenReturn(mockList);

        mockMvc.perform(get("/admin/featured-models"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].artisticName").value("Isabella Viana"))
                .andExpect(jsonPath("$[0].displayOrder").value(1))
                .andExpect(jsonPath("$[0].isStar").value(true));
    }

    @Test
    @DisplayName("PUT /admin/featured-models deve atualizar a ordem e retornar lista atualizada e 200 OK")
    void updateFeaturedHomeModels_ReturnsOk() throws Exception {
        FeaturedModelsReorderRequestDto request = FeaturedModelsReorderRequestDto.builder()
                .items(List.of(
                        FeaturedModelOrderItemDto.builder().modelId(modelId1).displayOrder(1).build(),
                        FeaturedModelOrderItemDto.builder().modelId(modelId2).displayOrder(2).build(),
                        FeaturedModelOrderItemDto.builder().modelId(modelId3).displayOrder(3).build(),
                        FeaturedModelOrderItemDto.builder().modelId(modelId4).displayOrder(4).build()
                ))
                .build();

        when(featuredModelService.updateFeaturedHomeModels(any(FeaturedModelsReorderRequestDto.class)))
                .thenReturn(mockList);

        mockMvc.perform(put("/admin/featured-models")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].id").value(modelId1.toString()));
    }

    @Test
    @DisplayName("PUT /admin/featured-models deve aceitar lista com qualquer quantidade de modelos (inclusive vazia ou 1)")
    void updateFeaturedHomeModels_AcceptsAnySize() throws Exception {
        FeaturedModelsReorderRequestDto request = FeaturedModelsReorderRequestDto.builder()
                .items(List.of(
                        FeaturedModelOrderItemDto.builder().modelId(modelId1).displayOrder(1).build()
                ))
                .build();

        when(featuredModelService.updateFeaturedHomeModels(any(FeaturedModelsReorderRequestDto.class)))
                .thenReturn(List.of(mockList.get(0)));

        mockMvc.perform(put("/admin/featured-models")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(modelId1.toString()));
    }
}

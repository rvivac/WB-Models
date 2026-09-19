package com.wbscouting.api.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wbscouting.api.dto.model.*;
import com.wbscouting.api.enums.GenderType;
import com.wbscouting.api.security.JwtAuthenticationEntryPoint;
import com.wbscouting.api.security.JwtAuthenticationFilter;
import com.wbscouting.api.security.JwtService;
import com.wbscouting.api.security.JwtTokenProvider;
import com.wbscouting.api.service.model.ModelService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminModelController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminModelControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ModelService modelService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    private UUID modelId;
    private ModelAdminResponseDto responseDto;

    @BeforeEach
    void setUp() {
        modelId = UUID.randomUUID();
        responseDto = ModelAdminResponseDto.builder()
                .id(modelId)
                .stageName("Gisele B.")
                .gender(GenderType.FEMALE)
                .isStar(true)
                .isFeaturedHome(true)
                .featuredOrder(1)
                .isActive(true)
                .heightCm(180)
                .city("São Paulo")
                .bustChestCm(new BigDecimal("86.00"))
                .waistCm(new BigDecimal("60.00"))
                .hipsCm(new BigDecimal("89.00"))
                .build();
    }

    @Test
    @DisplayName("Deve cadastrar novo modelo e retornar HTTP 201 com header Location")
    void shouldCreateModelSuccessfully() throws Exception {
        ModelCreateRequestDto request = ModelCreateRequestDto.builder()
                .stageName("Gisele B.")
                .gender(GenderType.FEMALE)
                .isStar(true)
                .heightCm(180)
                .build();

        when(modelService.createModel(any(ModelCreateRequestDto.class))).thenReturn(responseDto);

        mockMvc.perform(post("/admin/models")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").value(modelId.toString()))
                .andExpect(jsonPath("$.stageName").value("Gisele B."))
                .andExpect(jsonPath("$.gender").value("FEMALE"));
    }

    @Test
    @DisplayName("Deve rejeitar criação de modelo com campos obrigatórios ausentes (HTTP 400)")
    void shouldRejectCreationWithMissingFields() throws Exception {
        ModelCreateRequestDto request = ModelCreateRequestDto.builder()
                .stageName("") // Inválido: NotBlank
                .gender(null)  // Inválido: NotNull
                .build();

        mockMvc.perform(post("/admin/models")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Deve atualizar modelo existente com sucesso (HTTP 200)")
    void shouldUpdateModelSuccessfully() throws Exception {
        ModelUpdateRequestDto request = ModelUpdateRequestDto.builder()
                .stageName("Gisele Bundchen")
                .gender(GenderType.FEMALE)
                .isStar(true)
                .heightCm(181)
                .build();

        when(modelService.updateModel(eq(modelId), any(ModelUpdateRequestDto.class))).thenReturn(responseDto);

        mockMvc.perform(put("/admin/models/" + modelId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(modelId.toString()));
    }

    @Test
    @DisplayName("Deve alterar status de ativação pontualmente (HTTP 200)")
    void shouldUpdateStatusSuccessfully() throws Exception {
        ModelStatusPatchDto request = new ModelStatusPatchDto(false);
        responseDto.setIsActive(false);

        when(modelService.updateStatus(eq(modelId), any(ModelStatusPatchDto.class))).thenReturn(responseDto);

        mockMvc.perform(patch("/admin/models/" + modelId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false));
    }

    @Test
    @DisplayName("Deve alterar status de Star pontualmente (HTTP 200)")
    void shouldUpdateStarSuccessfully() throws Exception {
        ModelStarPatchDto request = new ModelStarPatchDto(false);
        responseDto.setIsStar(false);

        when(modelService.updateStar(eq(modelId), any(ModelStarPatchDto.class))).thenReturn(responseDto);

        mockMvc.perform(patch("/admin/models/" + modelId + "/star")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isStar").value(false));
    }

    @Test
    @DisplayName("Deve alterar destaque e ordem da home pontualmente (HTTP 200)")
    void shouldUpdateFeaturedSuccessfully() throws Exception {
        ModelFeaturedPatchDto request = new ModelFeaturedPatchDto(true, 3);
        responseDto.setIsFeaturedHome(true);
        responseDto.setFeaturedOrder(3);

        when(modelService.updateFeatured(eq(modelId), any(ModelFeaturedPatchDto.class))).thenReturn(responseDto);

        mockMvc.perform(patch("/admin/models/" + modelId + "/featured")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isFeaturedHome").value(true))
                .andExpect(jsonPath("$.featuredOrder").value(3));
    }

    @Test
    @DisplayName("Deve listar modelos administrativos com paginação (HTTP 200)")
    void shouldListAdminModelsSuccessfully() throws Exception {
        when(modelService.listAdminModels(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(responseDto)));

        mockMvc.perform(get("/admin/models")
                        .param("gender", "FEMALE")
                        .param("isStar", "true")
                        .param("search", "Gisele"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].stageName").value("Gisele B."));
    }

    @Test
    @DisplayName("Deve obter modelo por ID (HTTP 200)")
    void shouldGetAdminModelByIdSuccessfully() throws Exception {
        when(modelService.getAdminModelById(modelId)).thenReturn(responseDto);

        mockMvc.perform(get("/admin/models/" + modelId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(modelId.toString()))
                .andExpect(jsonPath("$.stageName").value("Gisele B."));
    }

    @Test
    @DisplayName("Deve excluir modelo com sucesso (HTTP 204)")
    void shouldDeleteModelSuccessfully() throws Exception {
        doNothing().when(modelService).deleteModel(modelId);

        mockMvc.perform(delete("/admin/models/" + modelId))
                .andExpect(status().isNoContent());
    }
}

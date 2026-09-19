package com.wbscouting.api.controller.publicapi;

import com.wbscouting.api.dto.publicapi.ModelCardPublicDto;
import com.wbscouting.api.dto.publicapi.PageResponseDto;
import com.wbscouting.api.enums.GenderType;
import com.wbscouting.api.security.JwtAuthenticationEntryPoint;
import com.wbscouting.api.security.JwtAuthenticationFilter;
import com.wbscouting.api.security.JwtService;
import com.wbscouting.api.security.JwtTokenProvider;
import com.wbscouting.api.service.publicapi.PublicModelCatalogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PublicModelCatalogController.class)
@AutoConfigureMockMvc(addFilters = false)
class PublicModelCatalogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PublicModelCatalogService publicModelCatalogService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    private ModelCardPublicDto cardDto;

    @BeforeEach
    void setUp() {
        cardDto = ModelCardPublicDto.builder()
                .id(UUID.randomUUID())
                .stageName("Adriana Lima")
                .gender(GenderType.FEMALE)
                .coverImageUrl("https://supabase.co/adriana-cover.jpg")
                .heightCm(178)
                .city("Salvador")
                .isStar(true)
                .build();
    }

    @Test
    @DisplayName("GET /public/models deve retornar 200 OK com paginação e cabeçalho Cache-Control max-age=120")
    void listModels_ReturnsOkWithCacheHeaderAndPagination() throws Exception {
        PageResponseDto<ModelCardPublicDto> pageResponse = PageResponseDto.<ModelCardPublicDto>builder()
                .content(List.of(cardDto))
                .pageNumber(0)
                .pageSize(24)
                .totalElements(1)
                .totalPages(1)
                .isLast(true)
                .build();

        when(publicModelCatalogService.listModels(eq(GenderType.FEMALE), eq(true), eq("Adriana"), eq(0), eq(24), eq("stageName,asc")))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/public/models")
                        .param("gender", "FEMALE")
                        .param("isStar", "true")
                        .param("search", "Adriana")
                        .param("page", "0")
                        .param("size", "24")
                        .param("sort", "stageName,asc"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("max-age=120")))
                .andExpect(jsonPath("$.content[0].stageName").value("Adriana Lima"))
                .andExpect(jsonPath("$.content[0].coverImageUrl").value("https://supabase.co/adriana-cover.jpg"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/public/models deve responder corretamente pela rota com prefixo /api/v1")
    void listModels_WithPathPrefixReturnsOk() throws Exception {
        PageResponseDto<ModelCardPublicDto> pageResponse = PageResponseDto.<ModelCardPublicDto>builder()
                .content(List.of(cardDto))
                .pageNumber(0)
                .pageSize(24)
                .totalElements(1)
                .totalPages(1)
                .isLast(true)
                .build();

        when(publicModelCatalogService.listModels(isNull(), isNull(), isNull(), anyInt(), anyInt(), anyString()))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/public/models"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("max-age=120")))
                .andExpect(jsonPath("$.content[0].stageName").value("Adriana Lima"));
    }
}

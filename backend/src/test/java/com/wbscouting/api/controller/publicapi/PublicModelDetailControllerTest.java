package com.wbscouting.api.controller.publicapi;

import com.wbscouting.api.dto.publicapi.ModelDetailPublicDto;
import com.wbscouting.api.dto.publicapi.ModelMediaPublicItemDto;
import com.wbscouting.api.enums.GenderType;
import com.wbscouting.api.exception.ResourceNotFoundException;
import com.wbscouting.api.security.JwtAuthenticationEntryPoint;
import com.wbscouting.api.security.JwtAuthenticationFilter;
import com.wbscouting.api.security.JwtService;
import com.wbscouting.api.security.JwtTokenProvider;
import com.wbscouting.api.service.publicapi.PublicModelDetailService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PublicModelDetailController.class)
@AutoConfigureMockMvc(addFilters = false)
class PublicModelDetailControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PublicModelDetailService publicModelDetailService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    private UUID modelId;
    private ModelDetailPublicDto detailDto;

    @BeforeEach
    void setUp() {
        modelId = UUID.randomUUID();

        ModelMediaPublicItemDto bookItem = ModelMediaPublicItemDto.builder()
                .id(UUID.randomUUID())
                .fileUrl("https://supabase.co/book1.jpg")
                .displayOrder(1)
                .isCover(true)
                .build();

        ModelMediaPublicItemDto polaroidItem = ModelMediaPublicItemDto.builder()
                .id(UUID.randomUUID())
                .fileUrl("https://supabase.co/pol1.jpg")
                .displayOrder(1)
                .isCover(false)
                .build();

        ModelMediaPublicItemDto compositeItem = ModelMediaPublicItemDto.builder()
                .id(UUID.randomUUID())
                .fileUrl("https://supabase.co/composite.jpg")
                .displayOrder(1)
                .isCover(false)
                .build();

        detailDto = ModelDetailPublicDto.builder()
                .id(modelId)
                .stageName("Carol Trentini")
                .gender(GenderType.FEMALE)
                .isStar(true)
                .city("Panambi")
                .nationality("Brasileira")
                .age(37)
                .instagramUrl("https://instagram.com/trentinireal")
                .heightCm(180)
                .bustChestCm(new BigDecimal("82.00"))
                .waistCm(new BigDecimal("60.00"))
                .hipsCm(new BigDecimal("89.00"))
                .dressSize("36")
                .shoeSize("38")
                .eyeColor("Azul")
                .hairColor("Loiro")
                .bookPhotos(List.of(bookItem))
                .polaroids(List.of(polaroidItem))
                .composite(compositeItem)
                .build();
    }

    @Test
    @DisplayName("GET /public/models/{id} deve retornar 200 OK com perfil detalhado e cabeçalho Cache-Control max-age=180")
    void getModelDetail_ReturnsOkWithCacheHeader180() throws Exception {
        when(publicModelDetailService.getModelDetail(modelId)).thenReturn(detailDto);

        mockMvc.perform(get("/public/models/{id}", modelId))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("max-age=180")))
                .andExpect(jsonPath("$.id").value(modelId.toString()))
                .andExpect(jsonPath("$.stageName").value("Carol Trentini"))
                .andExpect(jsonPath("$.age").value(37))
                .andExpect(jsonPath("$.isStar").value(true))
                .andExpect(jsonPath("$.bookPhotos[0].fileUrl").value("https://supabase.co/book1.jpg"))
                .andExpect(jsonPath("$.polaroids[0].fileUrl").value("https://supabase.co/pol1.jpg"))
                .andExpect(jsonPath("$.composite.fileUrl").value("https://supabase.co/composite.jpg"));
    }

    @Test
    @DisplayName("GET /api/v1/public/models/{id} deve responder corretamente pela rota com prefixo /api/v1")
    void getModelDetail_WithPathPrefixReturnsOk() throws Exception {
        when(publicModelDetailService.getModelDetail(modelId)).thenReturn(detailDto);

        mockMvc.perform(get("/api/v1/public/models/{id}", modelId))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("max-age=180")))
                .andExpect(jsonPath("$.stageName").value("Carol Trentini"));
    }

    @Test
    @DisplayName("GET /public/models/{id} deve retornar 404 Not Found quando modelo não existe ou está inativo")
    void getModelDetail_NotFound_Returns404() throws Exception {
        when(publicModelDetailService.getModelDetail(modelId))
                .thenThrow(new ResourceNotFoundException("Modelo não encontrado ou inativo"));

        mockMvc.perform(get("/public/models/{id}", modelId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso Não Encontrado"))
                .andExpect(jsonPath("$.detail").value("Modelo não encontrado ou inativo"));
    }
}

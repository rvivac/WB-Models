package com.wbscouting.api.controller.publicapi;

import com.wbscouting.api.dto.content.SiteContentPublicDto;
import com.wbscouting.api.exception.GlobalExceptionHandler;
import com.wbscouting.api.exception.ResourceNotFoundException;
import com.wbscouting.api.security.JwtAuthenticationEntryPoint;
import com.wbscouting.api.security.JwtAuthenticationFilter;
import com.wbscouting.api.security.JwtService;
import com.wbscouting.api.security.JwtTokenProvider;
import com.wbscouting.api.service.content.SiteContentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PublicContentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class PublicContentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SiteContentService siteContentService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Test
    @DisplayName("GET /api/v1/public/content/{sectionKey} - Deve retornar 200 OK com payload e cache de 300s")
    void getPublicContent_ReturnsOkWithCacheHeader() throws Exception {
        SiteContentPublicDto dto = SiteContentPublicDto.builder()
                .sectionKey("HOME_HERO")
                .lang("pt")
                .payload(Map.of("title", "Bem-vindo à WB Scouting"))
                .mediaUrls(Map.of("videoUrl", "https://supabase.co/video.mp4"))
                .build();

        when(siteContentService.getPublicContent("HOME_HERO", "pt")).thenReturn(dto);

        mockMvc.perform(get("/api/v1/public/content/HOME_HERO").param("lang", "pt"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "max-age=300, public"))
                .andExpect(jsonPath("$.sectionKey").value("HOME_HERO"))
                .andExpect(jsonPath("$.lang").value("pt"))
                .andExpect(jsonPath("$.payload.title").value("Bem-vindo à WB Scouting"))
                .andExpect(jsonPath("$.mediaUrls.videoUrl").value("https://supabase.co/video.mp4"));
    }

    @Test
    @DisplayName("GET /api/v1/public/content - Deve retornar mapa consolidado com 200 OK e cache de 300s")
    void getAllPublicContent_ReturnsOkWithCacheHeader() throws Exception {
        Map<String, Map<String, Object>> consolidated = Map.of(
                "HOME_HERO", Map.of("title", "Descubra Novos Talentos"),
                "ABOUT_US", Map.of("manifesto", "Nossa trajetória")
        );

        when(siteContentService.getAllPublicContent("pt")).thenReturn(consolidated);

        mockMvc.perform(get("/api/v1/public/content").param("lang", "pt"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "max-age=300, public"))
                .andExpect(jsonPath("$.HOME_HERO.title").value("Descubra Novos Talentos"))
                .andExpect(jsonPath("$.ABOUT_US.manifesto").value("Nossa trajetória"));
    }

    @Test
    @DisplayName("GET /api/v1/public/content/{sectionKey} - Deve retornar 404 RFC 7807 quando seção não encontrada")
    void getPublicContent_NotFound_ReturnsProblemDetails404() throws Exception {
        when(siteContentService.getPublicContent("UNKNOWN_KEY", "pt"))
                .thenThrow(new ResourceNotFoundException("Conteúdo da seção não encontrado: UNKNOWN_KEY"));

        mockMvc.perform(get("/api/v1/public/content/UNKNOWN_KEY"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso Não Encontrado"))
                .andExpect(jsonPath("$.detail").value("Conteúdo da seção não encontrado: UNKNOWN_KEY"))
                .andExpect(jsonPath("$.status").value(404));
    }
}

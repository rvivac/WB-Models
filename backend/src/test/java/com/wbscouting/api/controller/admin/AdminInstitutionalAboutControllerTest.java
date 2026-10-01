package com.wbscouting.api.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wbscouting.api.dto.AboutPageDto;
import com.wbscouting.api.dto.AboutPillarDto;
import com.wbscouting.api.dto.AboutSeoDto;
import com.wbscouting.api.exception.GlobalExceptionHandler;
import com.wbscouting.api.security.JwtAuthenticationEntryPoint;
import com.wbscouting.api.security.JwtAuthenticationFilter;
import com.wbscouting.api.security.JwtTokenProvider;
import com.wbscouting.api.service.content.AboutPageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminInstitutionalAboutController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AdminInstitutionalAboutControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AboutPageService aboutPageService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Test
    @DisplayName("GET /api/v1/admin/institutional/about - Deve retornar 200 OK com dados da página Sobre Nós")
    void getAboutPageSettings_ReturnsOk() throws Exception {
        AboutPageDto dto = AboutPageDto.builder()
                .title("A Nova Estética")
                .subtitle("MANIFESTO")
                .heroQuote("Frase de impacto")
                .pillars(List.of(AboutPillarDto.builder().order(1).titulo("Curadoria").descricao("Desc").build()))
                .seo(AboutSeoDto.builder().metaTitle("Sobre Nós").build())
                .build();

        when(aboutPageService.getAdminAboutPage()).thenReturn(dto);

        mockMvc.perform(get("/api/v1/admin/institutional/about"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("A Nova Estética"))
                .andExpect(jsonPath("$.subtitle").value("MANIFESTO"))
                .andExpect(jsonPath("$.heroQuote").value("Frase de impacto"))
                .andExpect(jsonPath("$.pillars[0].titulo").value("Curadoria"));

        verify(aboutPageService).getAdminAboutPage();
    }

    @Test
    @DisplayName("PUT /api/v1/admin/institutional/about - Deve atualizar textos com sucesso")
    void updateAboutPageSettings_ReturnsUpdated() throws Exception {
        AboutPageDto payload = AboutPageDto.builder()
                .title("Título Atualizado")
                .subtitle("Subtítulo")
                .description("Descrição")
                .heroQuote("Nova citação")
                .manifestoTitle("Manifesto")
                .manifestoText("Texto")
                .pillarsTitle("Pilares")
                .pillars(List.of(AboutPillarDto.builder().order(1).titulo("Pilar 1").descricao("Desc 1").build()))
                .seo(AboutSeoDto.builder().metaTitle("SEO Meta").build())
                .build();

        when(aboutPageService.updateAboutPage(any(AboutPageDto.class))).thenReturn(payload);

        mockMvc.perform(put("/api/v1/admin/institutional/about")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Título Atualizado"))
                .andExpect(jsonPath("$.heroQuote").value("Nova citação"));

        verify(aboutPageService).updateAboutPage(any(AboutPageDto.class));
    }
}

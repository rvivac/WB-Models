package com.wbscouting.api.controller.publicapi;

import com.wbscouting.api.dto.AboutPageDto;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PublicInstitutionalAboutController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class PublicInstitutionalAboutControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AboutPageService aboutPageService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Test
    @DisplayName("GET /api/v1/public/institutional/about - Deve retornar 200 OK com dados públicos da página Sobre")
    void getPublicAboutPage_ReturnsOk() throws Exception {
        AboutPageDto dto = AboutPageDto.builder()
                .title("A Nova Estética")
                .heroQuote("Frase de impacto")
                .build();

        when(aboutPageService.getPublicAboutPage()).thenReturn(dto);

        mockMvc.perform(get("/api/v1/public/institutional/about"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("A Nova Estética"))
                .andExpect(jsonPath("$.heroQuote").value("Frase de impacto"));

        verify(aboutPageService).getPublicAboutPage();
    }
}

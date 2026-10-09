package com.wbscouting.api.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wbscouting.api.dto.content.TranslationContentDto;
import com.wbscouting.api.dto.content.TranslationUpdateRequestDto;
import com.wbscouting.api.entity.SiteContent;
import com.wbscouting.api.repository.SiteContentRepository;
import com.wbscouting.api.security.JwtAuthenticationEntryPoint;
import com.wbscouting.api.security.JwtAuthenticationFilter;
import com.wbscouting.api.security.JwtService;
import com.wbscouting.api.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminInstitutionalTranslationController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminInstitutionalTranslationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SiteContentRepository siteContentRepository;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    private SiteContent manifestoContent;

    @BeforeEach
    void setUp() {
        Map<String, Object> pt = new HashMap<>();
        pt.put("headline", "A Nova Estética do Scouting Global");
        pt.put("quote", "Acreditamos na autenticidade, na força da personalidade e na beleza singular de cada indivíduo.");
        pt.put("sectionTitle", "Nossa Filosofia");
        pt.put("body", "Fundada com a premissa de unir rigor editorial...");

        Map<String, Object> en = new HashMap<>();
        en.put("headline", "The New Aesthetic of Global Scouting");
        en.put("quote", "We believe in authenticity, personal strength, and the unique beauty of every individual.");
        en.put("sectionTitle", "Our Philosophy");
        en.put("body", "Founded with the premise of uniting editorial rigor...");

        manifestoContent = SiteContent.builder()
                .id(UUID.randomUUID())
                .sectionKey("ABOUT_MANIFESTO")
                .payloadPt(pt)
                .payloadEn(en)
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/admin/institutional/translations - Deve listar seções traduzíveis")
    void shouldListTranslatableSections() throws Exception {
        mockMvc.perform(get("/api/v1/admin/institutional/translations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0].sectionKey").value("ABOUT_MANIFESTO"))
                .andExpect(jsonPath("$[1].sectionKey").value("SCOUTING_GUIDELINES"));
    }

    @Test
    @DisplayName("GET /api/v1/admin/institutional/translations/{sectionKey} - Deve retornar traduções da seção existente")
    void shouldGetExistingSectionTranslations() throws Exception {
        when(siteContentRepository.findBySectionKey("ABOUT_MANIFESTO")).thenReturn(Optional.of(manifestoContent));

        mockMvc.perform(get("/api/v1/admin/institutional/translations/ABOUT_MANIFESTO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sectionKey").value("ABOUT_MANIFESTO"))
                .andExpect(jsonPath("$.title").value("Manifesto da Agência (Sobre Nós)"))
                .andExpect(jsonPath("$.translations.pt.headline").value("A Nova Estética do Scouting Global"))
                .andExpect(jsonPath("$.translations.pt.sectionTitle").value("Nossa Filosofia"))
                .andExpect(jsonPath("$.translations.en.headline").value("The New Aesthetic of Global Scouting"))
                .andExpect(jsonPath("$.translations.en.sectionTitle").value("Our Philosophy"));
    }

    @Test
    @DisplayName("PUT /api/v1/admin/institutional/translations/{sectionKey} - Deve atualizar traduções paralelas")
    void shouldUpdateSectionTranslations() throws Exception {
        when(siteContentRepository.findBySectionKey("ABOUT_MANIFESTO")).thenReturn(Optional.of(manifestoContent));
        when(siteContentRepository.save(any(SiteContent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TranslationUpdateRequestDto request = TranslationUpdateRequestDto.builder()
                .pt(TranslationContentDto.builder()
                        .headline("Novo Título PT")
                        .quote("Nova Citação PT")
                        .sectionTitle("Nossa Filosofia PT")
                        .body("Novo Corpo PT")
                        .build())
                .en(TranslationContentDto.builder()
                        .headline("New Title EN")
                        .quote("New Quote EN")
                        .sectionTitle("Our Philosophy EN")
                        .body("New Body EN")
                        .build())
                .build();

        mockMvc.perform(put("/api/v1/admin/institutional/translations/ABOUT_MANIFESTO")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sectionKey").value("ABOUT_MANIFESTO"))
                .andExpect(jsonPath("$.translations.pt.headline").value("Novo Título PT"))
                .andExpect(jsonPath("$.translations.pt.sectionTitle").value("Nossa Filosofia PT"))
                .andExpect(jsonPath("$.translations.en.headline").value("New Title EN"))
                .andExpect(jsonPath("$.translations.en.sectionTitle").value("Our Philosophy EN"));
    }
}

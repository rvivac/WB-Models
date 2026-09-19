package com.wbscouting.api.controller.publicapi;

import com.wbscouting.api.dto.publicapi.ContactChannelsPublicDto;
import com.wbscouting.api.exception.GlobalExceptionHandler;
import com.wbscouting.api.security.JwtAuthenticationEntryPoint;
import com.wbscouting.api.security.JwtAuthenticationFilter;
import com.wbscouting.api.security.JwtTokenProvider;
import com.wbscouting.api.service.content.ContactChannelsService;
import com.wbscouting.api.service.content.I18nDictionaryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PublicContactController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class PublicContactControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ContactChannelsService contactChannelsService;

    @MockBean
    private I18nDictionaryService i18nDictionaryService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Test
    @DisplayName("GET /api/v1/public/contact-channels - Deve retornar 200 OK com cabeçalho de cache de 600s")
    void getContactChannels_ReturnsOkWithCacheHeader() throws Exception {
        ContactChannelsPublicDto dto = ContactChannelsPublicDto.builder()
                .email("contato@wbscouting.com")
                .whatsappNumber("5511999999999")
                .whatsappUrl("https://wa.me/5511999999999?text=Ol%C3%A1")
                .instagramHandle("@wbscouting")
                .address("São Paulo - SP, Brasil")
                .officeHours("Segunda a Sexta, das 09h às 18h")
                .build();

        when(contactChannelsService.getContactChannels(eq("pt"))).thenReturn(dto);

        mockMvc.perform(get("/api/v1/public/contact-channels?lang=pt"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "max-age=600, public"))
                .andExpect(jsonPath("$.email").value("contato@wbscouting.com"))
                .andExpect(jsonPath("$.whatsappNumber").value("5511999999999"))
                .andExpect(jsonPath("$.whatsappUrl").value("https://wa.me/5511999999999?text=Ol%C3%A1"))
                .andExpect(jsonPath("$.instagramHandle").value("@wbscouting"));
    }

    @Test
    @DisplayName("GET /api/v1/public/i18n/{lang} - Deve retornar 200 OK com cabeçalho de cache de 300s")
    void getDictionary_ReturnsOkWithCacheHeader() throws Exception {
        Map<String, Object> dictionary = Map.of(
                "HOME_HERO", Map.of("title", "WB Scouting"),
                "ABOUT_US", Map.of("description", "Agência de Modelos")
        );

        when(i18nDictionaryService.getDictionary(eq("pt"))).thenReturn(dictionary);

        mockMvc.perform(get("/api/v1/public/i18n/pt"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "max-age=300, public"))
                .andExpect(jsonPath("$.HOME_HERO.title").value("WB Scouting"))
                .andExpect(jsonPath("$.ABOUT_US.description").value("Agência de Modelos"));
    }
}

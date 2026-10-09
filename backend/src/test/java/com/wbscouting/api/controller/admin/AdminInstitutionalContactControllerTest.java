package com.wbscouting.api.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wbscouting.api.dto.content.ContactSettingsDto;
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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminInstitutionalContactController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminInstitutionalContactControllerTest {

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

    private SiteContent siteContent;

    @BeforeEach
    void setUp() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("primaryEmail", "contato@wbscouting.com");
        payload.put("scoutingEmail", "scouting@wbscouting.com");
        payload.put("pressEmail", "press@wbscouting.com");
        payload.put("phone", "+55 11 99999-9999");
        payload.put("whatsapp", "+55 11 99999-9999");
        payload.put("whatsappDefaultMessage", "Olá! Gostaria de falar com a equipe de atendimento da WB Agency.");
        payload.put("businessHours", "Segunda a Sexta: 09h às 18h (GMT-3)");

        Map<String, Object> addr = new LinkedHashMap<>();
        addr.put("street", "Avenida Paulista, 1000");
        addr.put("city", "São Paulo");
        addr.put("state", "SP");
        payload.put("address", addr);

        Map<String, Object> sm = new LinkedHashMap<>();
        sm.put("instagram", "https://instagram.com/wbagency");
        sm.put("linkedin", "https://linkedin.com/company/wbagency");
        payload.put("socialMedia", sm);

        java.util.List<Map<String, String>> smList = new java.util.ArrayList<>();
        Map<String, String> item1 = new LinkedHashMap<>();
        item1.put("id", "sm-1");
        item1.put("name", "TikTok");
        item1.put("url", "https://tiktok.com/@wbagency");
        smList.add(item1);
        payload.put("socialMediaList", smList);

        siteContent = SiteContent.builder()
                .id(UUID.randomUUID())
                .sectionKey("contact")
                .payloadPt(payload)
                .payloadEn(payload)
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/admin/institutional/contact deve retornar canais de contato atuais")
    void getContactSettings_ReturnsOk() throws Exception {
        when(siteContentRepository.findBySectionKey("contact")).thenReturn(Optional.of(siteContent));

        mockMvc.perform(get("/api/v1/admin/institutional/contact"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primaryEmail").value("contato@wbscouting.com"))
                .andExpect(jsonPath("$.whatsapp").value("+55 11 99999-9999"))
                .andExpect(jsonPath("$.address.city").value("São Paulo"))
                .andExpect(jsonPath("$.socialMedia.instagram").value("https://instagram.com/wbagency"))
                .andExpect(jsonPath("$.socialMediaList[0].name").value("TikTok"));
    }

    @Test
    @DisplayName("PUT /api/v1/admin/institutional/contact deve atualizar canais de contato com sucesso")
    void updateContactSettings_ReturnsOk() throws Exception {
        when(siteContentRepository.findBySectionKey("contact")).thenReturn(Optional.of(siteContent));
        when(siteContentRepository.save(any(SiteContent.class))).thenAnswer(inv -> inv.getArgument(0));

        ContactSettingsDto dto = ContactSettingsDto.builder()
                .primaryEmail("novo@wbscouting.com")
                .scoutingEmail("talentos@wbscouting.com")
                .pressEmail("imprensa@wbscouting.com")
                .phone("+55 11 3333-4444")
                .whatsapp("+55 11 98888-7777")
                .whatsappDefaultMessage("Olá equipe!")
                .businessHours("08h às 17h")
                .address(ContactSettingsDto.AddressDto.builder()
                        .street("Rua Oscar Freire, 500")
                        .city("São Paulo")
                        .state("SP")
                        .build())
                .socialMedia(ContactSettingsDto.SocialMediaDto.builder()
                        .instagram("https://instagram.com/novowb")
                        .build())
                .socialMediaList(java.util.List.of(
                        ContactSettingsDto.SocialMediaItemDto.builder()
                                .id("sm-custom")
                                .name("Pinterest")
                                .url("https://pinterest.com/wbagency")
                                .build()
                ))
                .build();

        mockMvc.perform(put("/api/v1/admin/institutional/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primaryEmail").value("novo@wbscouting.com"))
                .andExpect(jsonPath("$.whatsapp").value("+55 11 98888-7777"))
                .andExpect(jsonPath("$.address.street").value("Rua Oscar Freire, 500"))
                .andExpect(jsonPath("$.socialMediaList[0].name").value("Pinterest"));
    }

    @Test
    @DisplayName("PUT /api/v1/admin/institutional/contact com e-mail inválido deve retornar 400 Bad Request")
    void updateContactSettings_InvalidEmail_ReturnsBadRequest() throws Exception {
        ContactSettingsDto dto = ContactSettingsDto.builder()
                .primaryEmail("email-invalido")
                .phone("+55 11 99999-9999")
                .whatsapp("+55 11 99999-9999")
                .build();

        mockMvc.perform(put("/api/v1/admin/institutional/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }
}

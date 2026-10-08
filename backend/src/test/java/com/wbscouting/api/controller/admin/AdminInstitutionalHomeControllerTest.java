package com.wbscouting.api.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wbscouting.api.config.SupabaseProperties;
import com.wbscouting.api.dto.content.HomeContentDto;
import com.wbscouting.api.entity.SiteContent;
import com.wbscouting.api.repository.SiteContentRepository;
import com.wbscouting.api.security.JwtAuthenticationEntryPoint;
import com.wbscouting.api.security.JwtAuthenticationFilter;
import com.wbscouting.api.security.JwtService;
import com.wbscouting.api.service.storage.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminInstitutionalHomeController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminInstitutionalHomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SiteContentRepository siteContentRepository;

    @MockitoBean
    private StorageService storageService;

    @MockitoBean
    private SupabaseProperties supabaseProperties;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    private SiteContent siteContent;

    @BeforeEach
    void setUp() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("heroTitle", "WB AGENCY");
        payload.put("heroSubtitle", "EDITORIAL & HIGH FASHION SCOUTING");
        payload.put("heroDescription", "Representação exclusiva e curadoria estética.");
        payload.put("scrollLabel", "SCROLL");
        payload.put("metaTitle", "WB Agency | Scouting");
        payload.put("metaDescription", "Agência de scouting e alta moda.");

        Map<String, Object> media = new HashMap<>();
        media.put("videoUrl", "https://storage.wb.agency/hero.mp4");
        media.put("posterUrl", "https://storage.wb.agency/poster.webp");

        siteContent = SiteContent.builder()
                .id(UUID.randomUUID())
                .sectionKey("home")
                .payloadPt(payload)
                .payloadEn(payload)
                .mediaUrls(media)
                .build();

        SupabaseProperties.Buckets buckets = new SupabaseProperties.Buckets();
        when(supabaseProperties.getBuckets()).thenReturn(buckets);
    }

    @Test
    @DisplayName("GET /api/v1/admin/institutional/home deve retornar configuração vigente")
    void getHomeContent_ReturnsOk() throws Exception {
        when(siteContentRepository.findBySectionKey("home")).thenReturn(Optional.of(siteContent));

        mockMvc.perform(get("/api/v1/admin/institutional/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.heroTitle").value("WB AGENCY"))
                .andExpect(jsonPath("$.heroSubtitle").value("EDITORIAL & HIGH FASHION SCOUTING"))
                .andExpect(jsonPath("$.videoUrl").value("https://storage.wb.agency/hero.mp4"))
                .andExpect(jsonPath("$.posterUrl").value("https://storage.wb.agency/poster.webp"));
    }

    @Test
    @DisplayName("PUT /api/v1/admin/institutional/home deve atualizar textos e metadados")
    void updateHomeContent_ReturnsOk() throws Exception {
        when(siteContentRepository.findBySectionKey("home")).thenReturn(Optional.of(siteContent));
        when(siteContentRepository.save(any(SiteContent.class))).thenAnswer(inv -> inv.getArgument(0));

        HomeContentDto updateDto = HomeContentDto.builder()
                .heroTitle("NOVA TEMPORADA")
                .heroSubtitle("FALL WINTER 2026")
                .heroDescription("Nova curadoria editorial internacional.")
                .scrollLabel("EXPLORAR")
                .metaTitle("WB Agency | Fall Winter 2026")
                .metaDescription("Lançamento oficial da nova temporada.")
                .build();

        mockMvc.perform(put("/api/v1/admin/institutional/home")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.heroTitle").value("NOVA TEMPORADA"))
                .andExpect(jsonPath("$.scrollLabel").value("EXPLORAR"));
    }

    @Test
    @DisplayName("PUT /api/v1/admin/institutional/home/video deve realizar upload do vídeo hero")
    void uploadHeroVideo_ReturnsOk() throws Exception {
        when(siteContentRepository.findBySectionKey("home")).thenReturn(Optional.of(siteContent));
        when(storageService.uploadFile(anyString(), anyString(), any())).thenReturn("institutional/home-hero.mp4");
        when(storageService.getPublicUrl(anyString(), eq("institutional/home-hero.mp4")))
                .thenReturn("https://storage.wb.agency/institutional/home-hero.mp4");

        MockMultipartFile videoFile = new MockMultipartFile(
                "file",
                "campaign.mp4",
                "video/mp4",
                new byte[]{0x00, 0x01, 0x02}
        );

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/v1/admin/institutional/home/video")
                        .file(videoFile))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.videoUrl").value("https://storage.wb.agency/institutional/home-hero.mp4"));
    }

    @Test
    @DisplayName("PUT /api/v1/admin/institutional/home/poster deve realizar upload do poster de capa")
    void uploadHeroPoster_ReturnsOk() throws Exception {
        when(siteContentRepository.findBySectionKey("home")).thenReturn(Optional.of(siteContent));
        when(storageService.uploadFile(anyString(), anyString(), any())).thenReturn("institutional/home-poster.webp");
        when(storageService.getPublicUrl(anyString(), eq("institutional/home-poster.webp")))
                .thenReturn("https://storage.wb.agency/institutional/home-poster.webp");

        MockMultipartFile posterFile = new MockMultipartFile(
                "file",
                "cover.webp",
                "image/webp",
                new byte[]{0x00, 0x01, 0x02}
        );

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/v1/admin/institutional/home/poster")
                        .file(posterFile))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posterUrl").value("https://storage.wb.agency/institutional/home-poster.webp"));
    }
}

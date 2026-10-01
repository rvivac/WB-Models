package com.wbscouting.api.controller.publicapi;

import com.wbscouting.api.dto.ApplyFaqDto;
import com.wbscouting.api.dto.ApplyHeaderDto;
import com.wbscouting.api.exception.GlobalExceptionHandler;
import com.wbscouting.api.security.JwtAuthenticationEntryPoint;
import com.wbscouting.api.security.JwtAuthenticationFilter;
import com.wbscouting.api.security.JwtTokenProvider;
import com.wbscouting.api.service.content.ApplyFaqService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PublicApplyFaqController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class PublicApplyFaqControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ApplyFaqService applyFaqService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Test
    @DisplayName("GET /api/v1/public/apply-faq - Deve retornar 200 OK com lista de FAQs ativas")
    void getPublicFaqs_ReturnsOk() throws Exception {
        ApplyFaqDto faq = ApplyFaqDto.builder()
                .id(UUID.randomUUID())
                .question("Existe algum custo para inscrição?")
                .answer("Não. A WB Agency nunca cobra nenhuma taxa.")
                .displayOrder(0)
                .isActive(true)
                .build();

        when(applyFaqService.getPublicActiveFaqs()).thenReturn(List.of(faq));

        mockMvc.perform(get("/api/v1/public/apply-faq"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Cache-Control"))
                .andExpect(jsonPath("$[0].question").value("Existe algum custo para inscrição?"))
                .andExpect(jsonPath("$[0].answer").value("Não. A WB Agency nunca cobra nenhuma taxa."));
    }

    @Test
    @DisplayName("GET /api/v1/public/institutional/apply-header - Deve retornar 200 OK com dados de cabeçalho")
    void getPublicApplyHeader_ReturnsOk() throws Exception {
        ApplyHeaderDto header = ApplyHeaderDto.builder()
                .title("QUERO SER MODELO")
                .subtitle("WB SCOUTING DESK")
                .description("Preencha o formulário e envie suas fotos.")
                .build();

        when(applyFaqService.getPublicApplyHeader()).thenReturn(header);

        mockMvc.perform(get("/api/v1/public/institutional/apply-header"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Cache-Control"))
                .andExpect(jsonPath("$.title").value("QUERO SER MODELO"))
                .andExpect(jsonPath("$.subtitle").value("WB SCOUTING DESK"));
    }
}

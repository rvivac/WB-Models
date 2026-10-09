package com.wbscouting.api.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wbscouting.api.dto.ApplyFaqCreateUpdateDto;
import com.wbscouting.api.dto.ApplyFaqDto;
import com.wbscouting.api.dto.ApplyHeaderDto;
import com.wbscouting.api.exception.GlobalExceptionHandler;
import com.wbscouting.api.security.JwtAuthenticationEntryPoint;
import com.wbscouting.api.security.JwtAuthenticationFilter;
import com.wbscouting.api.security.JwtService;
import com.wbscouting.api.service.content.ApplyFaqService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminApplyFaqController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AdminApplyFaqControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ApplyFaqService applyFaqService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Test
    @DisplayName("GET /api/v1/admin/apply-faq - Deve retornar todas as perguntas")
    void getAllFaqs_ReturnsOk() throws Exception {
        ApplyFaqDto faq = ApplyFaqDto.builder()
                .id(UUID.randomUUID())
                .question("Pergunta Teste")
                .answer("Resposta Teste")
                .displayOrder(0)
                .isActive(true)
                .build();

        when(applyFaqService.getAllFaqs()).thenReturn(List.of(faq));

        mockMvc.perform(get("/api/v1/admin/apply-faq"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].question").value("Pergunta Teste"));
    }

    @Test
    @DisplayName("POST /api/v1/admin/apply-faq - Deve criar pergunta com 201 Created")
    void createFaq_ReturnsCreated() throws Exception {
        ApplyFaqCreateUpdateDto createDto = ApplyFaqCreateUpdateDto.builder()
                .question("Nova Pergunta")
                .answer("Nova Resposta")
                .displayOrder(1)
                .isActive(true)
                .build();

        ApplyFaqDto responseDto = ApplyFaqDto.builder()
                .id(UUID.randomUUID())
                .question("Nova Pergunta")
                .answer("Nova Resposta")
                .displayOrder(1)
                .isActive(true)
                .build();

        when(applyFaqService.createFaq(any(ApplyFaqCreateUpdateDto.class))).thenReturn(responseDto);

        mockMvc.perform(post("/api/v1/admin/apply-faq")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.question").value("Nova Pergunta"));
    }

    @Test
    @DisplayName("PUT /api/v1/admin/apply-faq/{id} - Deve atualizar pergunta existente")
    void updateFaq_ReturnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        ApplyFaqCreateUpdateDto updateDto = ApplyFaqCreateUpdateDto.builder()
                .question("Pergunta Editada")
                .answer("Resposta Editada")
                .build();

        ApplyFaqDto responseDto = ApplyFaqDto.builder()
                .id(id)
                .question("Pergunta Editada")
                .answer("Resposta Editada")
                .build();

        when(applyFaqService.updateFaq(eq(id), any(ApplyFaqCreateUpdateDto.class))).thenReturn(responseDto);

        mockMvc.perform(put("/api/v1/admin/apply-faq/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.question").value("Pergunta Editada"));
    }

    @Test
    @DisplayName("PATCH /api/v1/admin/apply-faq/{id}/status - Deve alternar status ativo/inativo")
    void toggleStatus_ReturnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        ApplyFaqDto responseDto = ApplyFaqDto.builder()
                .id(id)
                .question("Pergunta")
                .answer("Resposta")
                .isActive(false)
                .build();

        when(applyFaqService.toggleStatus(eq(id))).thenReturn(responseDto);

        mockMvc.perform(patch("/api/v1/admin/apply-faq/" + id + "/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false));
    }

    @Test
    @DisplayName("DELETE /api/v1/admin/apply-faq/{id} - Deve retornar 204 No Content")
    void deleteFaq_ReturnsNoContent() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(applyFaqService).deleteFaq(eq(id));

        mockMvc.perform(delete("/api/v1/admin/apply-faq/" + id))
                .andExpect(status().isNoContent());

        verify(applyFaqService, times(1)).deleteFaq(id);
    }

    @Test
    @DisplayName("PUT /api/v1/admin/institutional/apply-header - Deve atualizar cabeçalho institucional")
    void updateApplyHeader_ReturnsOk() throws Exception {
        ApplyHeaderDto dto = ApplyHeaderDto.builder()
                .title("QUERO SER MODELO ATUALIZADO")
                .subtitle("WB SCOUTING NOVO")
                .description("Nova descrição")
                .build();

        when(applyFaqService.updateApplyHeader(any(ApplyHeaderDto.class))).thenReturn(dto);

        mockMvc.perform(put("/api/v1/admin/institutional/apply-header")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("QUERO SER MODELO ATUALIZADO"));
    }

    @Test
    @DisplayName("PUT /api/v1/admin/apply-faq - Deve atualizar lote completo de perguntas e respostas")
    void updateAllFaqs_ReturnsOk() throws Exception {
        ApplyFaqDto item = ApplyFaqDto.builder()
                .question("Pergunta Lote")
                .answer("Resposta Lote")
                .questionEn("Question Batch")
                .answerEn("Answer Batch")
                .order(1)
                .build();

        when(applyFaqService.saveBatch(any())).thenReturn(List.of(item));

        java.util.Map<String, Object> payload = java.util.Map.of("items", List.of(item));

        mockMvc.perform(put("/api/v1/admin/apply-faq")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].question").value("Pergunta Lote"))
                .andExpect(jsonPath("$[0].questionEn").value("Question Batch"));
    }
}

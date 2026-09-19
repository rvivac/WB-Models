package com.wbscouting.api.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wbscouting.api.dto.admin.contact.ContactChannelsUpdateRequestDto;
import com.wbscouting.api.dto.publicapi.ContactChannelsPublicDto;
import com.wbscouting.api.entity.Admin;
import com.wbscouting.api.exception.GlobalExceptionHandler;
import com.wbscouting.api.security.JwtAuthenticationEntryPoint;
import com.wbscouting.api.security.JwtAuthenticationFilter;
import com.wbscouting.api.security.JwtTokenProvider;
import com.wbscouting.api.service.content.ContactChannelsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminContactChannelsController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AdminContactChannelsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ContactChannelsService contactChannelsService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Test
    @DisplayName("PUT /api/v1/admin/contact-channels - Deve atualizar e retornar 200 OK")
    void updateContactChannels_ReturnsOk() throws Exception {
        UUID adminId = UUID.randomUUID();
        Admin admin = Admin.builder().id(adminId).email("admin@wbscouting.com").build();
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(admin, null, List.of());

        ContactChannelsUpdateRequestDto request = ContactChannelsUpdateRequestDto.builder()
                .email("contato@wbscouting.com")
                .whatsappNumber("5511999998888")
                .whatsappDefaultMessagePt("Olá!")
                .whatsappDefaultMessageEn("Hello!")
                .instagramHandle("@wbscouting")
                .addressPt("Av. Paulista, 1000")
                .addressEn("Paulista Ave, 1000")
                .officeHoursPt("09h às 18h")
                .officeHoursEn("9 AM to 6 PM")
                .build();

        ContactChannelsPublicDto responseDto = ContactChannelsPublicDto.builder()
                .email("contato@wbscouting.com")
                .whatsappNumber("5511999998888")
                .whatsappUrl("https://wa.me/5511999998888?text=Ol%C3%A1%21")
                .instagramHandle("@wbscouting")
                .address("Av. Paulista, 1000")
                .officeHours("09h às 18h")
                .build();

        when(contactChannelsService.updateContactChannels(any(ContactChannelsUpdateRequestDto.class), any()))
                .thenReturn(responseDto);

        mockMvc.perform(put("/api/v1/admin/contact-channels")
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("contato@wbscouting.com"))
                .andExpect(jsonPath("$.whatsappNumber").value("5511999998888"))
                .andExpect(jsonPath("$.whatsappUrl").value("https://wa.me/5511999998888?text=Ol%C3%A1%21"));
    }

    @Test
    @DisplayName("PUT /api/v1/admin/contact-channels - Deve retornar 400 Bad Request quando e-mail for inválido")
    void updateContactChannels_InvalidEmail_ReturnsBadRequest() throws Exception {
        ContactChannelsUpdateRequestDto request = ContactChannelsUpdateRequestDto.builder()
                .email("email-invalido")
                .whatsappNumber("5511999998888")
                .whatsappDefaultMessagePt("Olá!")
                .addressPt("Av. Paulista, 1000")
                .officeHoursPt("09h às 18h")
                .build();

        mockMvc.perform(put("/api/v1/admin/contact-channels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/v1/admin/contact-channels - Deve retornar 400 Bad Request quando WhatsApp contiver caracteres não numéricos")
    void updateContactChannels_InvalidWhatsapp_ReturnsBadRequest() throws Exception {
        ContactChannelsUpdateRequestDto request = ContactChannelsUpdateRequestDto.builder()
                .email("contato@wbscouting.com")
                .whatsappNumber("telefone-invalido")
                .whatsappDefaultMessagePt("Olá!")
                .addressPt("Av. Paulista, 1000")
                .officeHoursPt("09h às 18h")
                .build();

        mockMvc.perform(put("/api/v1/admin/contact-channels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}

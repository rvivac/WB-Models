package com.wbscouting.api.service.content;

import com.wbscouting.api.constant.ContentSectionKey;
import com.wbscouting.api.dto.admin.contact.ContactChannelsUpdateRequestDto;
import com.wbscouting.api.dto.publicapi.ContactChannelsPublicDto;
import com.wbscouting.api.entity.SiteContent;
import com.wbscouting.api.repository.SiteContentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContactChannelsServiceImplTest {

    @Mock
    private SiteContentRepository siteContentRepository;

    @InjectMocks
    private ContactChannelsServiceImpl contactChannelsService;

    private SiteContent contactContent;

    @BeforeEach
    void setUp() {
        Map<String, Object> ptPayload = Map.of(
                "email", "contato@wbscouting.com",
                "whatsappNumber", "5511988887777",
                "whatsappDefaultMessage", "Olá WB Scouting!",
                "instagramHandle", "@wbscouting",
                "address", "Av. Paulista, 1000 - São Paulo, SP",
                "officeHours", "Segunda a Sexta, das 09h às 18h"
        );

        Map<String, Object> enPayload = Map.of(
                "email", "contact@wbscouting.com",
                "whatsappNumber", "5511988887777",
                "whatsappDefaultMessage", "Hello WB Scouting!",
                "instagramHandle", "@wbscouting",
                "address", "Paulista Ave, 1000 - Sao Paulo, Brazil",
                "officeHours", "Monday to Friday, 9:00 AM - 6:00 PM"
        );

        contactContent = SiteContent.builder()
                .id(UUID.randomUUID())
                .sectionKey(ContentSectionKey.CONTACT_INFO)
                .payloadPt(ptPayload)
                .payloadEn(enPayload)
                .build();
    }

    @Test
    @DisplayName("Deve retornar canais padrão quando não houver registro no banco")
    void getContactChannels_Default_WhenNoDataInDb() {
        when(siteContentRepository.findBySectionKey(ContentSectionKey.CONTACT_INFO))
                .thenReturn(Optional.empty());

        ContactChannelsPublicDto result = contactChannelsService.getContactChannels("pt");

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("contato@wbscouting.com");
        assertThat(result.getWhatsappNumber()).isEqualTo("5511999999999");
        assertThat(result.getWhatsappUrl()).contains("https://wa.me/5511999999999");
        assertThat(result.getInstagramHandle()).isEqualTo("@wbscouting");
        assertThat(result.getAddress()).isEqualTo("São Paulo - SP, Brasil");
        assertThat(result.getOfficeHours()).isEqualTo("Segunda a Sexta, das 09h às 18h");
    }

    @Test
    @DisplayName("Deve retornar canais em inglês quando solicitado e disponível no banco")
    void getContactChannels_En_WhenDataInDb() {
        when(siteContentRepository.findBySectionKey(ContentSectionKey.CONTACT_INFO))
                .thenReturn(Optional.of(contactContent));

        ContactChannelsPublicDto result = contactChannelsService.getContactChannels("en");

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("contact@wbscouting.com");
        assertThat(result.getWhatsappNumber()).isEqualTo("5511988887777");
        assertThat(result.getWhatsappUrl()).contains("https://wa.me/5511988887777");
        assertThat(result.getWhatsappUrl()).contains("Hello%20WB%20Scouting%21");
        assertThat(result.getAddress()).isEqualTo("Paulista Ave, 1000 - Sao Paulo, Brazil");
        assertThat(result.getOfficeHours()).isEqualTo("Monday to Friday, 9:00 AM - 6:00 PM");
    }

    @Test
    @DisplayName("Deve fazer fallback para PT quando payload EN estiver ausente ou vazio")
    void getContactChannels_En_FallbackToPt() {
        contactContent.setPayloadEn(Map.of());
        when(siteContentRepository.findBySectionKey(ContentSectionKey.CONTACT_INFO))
                .thenReturn(Optional.of(contactContent));

        ContactChannelsPublicDto result = contactChannelsService.getContactChannels("en");

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("contato@wbscouting.com");
        assertThat(result.getWhatsappUrl()).contains("Ol%C3%A1%20WB%20Scouting%21");
        assertThat(result.getAddress()).isEqualTo("Av. Paulista, 1000 - São Paulo, SP");
    }

    @Test
    @DisplayName("Deve retornar canais em português quando lang for pt")
    void getContactChannels_Pt_ReturnsPt() {
        when(siteContentRepository.findBySectionKey(ContentSectionKey.CONTACT_INFO))
                .thenReturn(Optional.of(contactContent));

        ContactChannelsPublicDto result = contactChannelsService.getContactChannels("pt");

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("contato@wbscouting.com");
        assertThat(result.getWhatsappNumber()).isEqualTo("5511988887777");
        assertThat(result.getAddress()).isEqualTo("Av. Paulista, 1000 - São Paulo, SP");
    }

    @Test
    @DisplayName("Deve atualizar canais existentes sanitizando o número de WhatsApp")
    void updateContactChannels_ExistingRecord_UpdatesAndSaves() {
        when(siteContentRepository.findBySectionKey(ContentSectionKey.CONTACT_INFO))
                .thenReturn(Optional.of(contactContent));
        when(siteContentRepository.save(any(SiteContent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UUID adminId = UUID.randomUUID();
        ContactChannelsUpdateRequestDto dto = ContactChannelsUpdateRequestDto.builder()
                .email("novo@wbscouting.com")
                .whatsappNumber("+55 (11) 97777-6666")
                .whatsappDefaultMessagePt("Olá, quero conversar!")
                .whatsappDefaultMessageEn("Hello, let's talk!")
                .instagramHandle("@wbscouting_oficial")
                .addressPt("Rua Oscar Freire, 500 - SP")
                .addressEn("Oscar Freire St, 500 - SP")
                .officeHoursPt("08h às 17h")
                .officeHoursEn("8 AM to 5 PM")
                .build();

        ContactChannelsPublicDto updated = contactChannelsService.updateContactChannels(dto, adminId);

        assertThat(updated).isNotNull();
        assertThat(updated.getEmail()).isEqualTo("novo@wbscouting.com");
        assertThat(updated.getWhatsappNumber()).isEqualTo("5511977776666");
        assertThat(updated.getWhatsappUrl()).contains("https://wa.me/5511977776666");

        ArgumentCaptor<SiteContent> captor = ArgumentCaptor.forClass(SiteContent.class);
        verify(siteContentRepository).save(captor.capture());
        SiteContent saved = captor.getValue();
        assertThat(saved.getUpdatedBy()).isEqualTo(adminId);
        assertThat(saved.getPayloadPt().get("whatsappNumber")).isEqualTo("5511977776666");
    }

    @Test
    @DisplayName("Deve criar novo registro caso CONTACT_INFO não exista no banco")
    void updateContactChannels_NewRecord_CreatesAndSaves() {
        when(siteContentRepository.findBySectionKey(ContentSectionKey.CONTACT_INFO))
                .thenReturn(Optional.empty());
        when(siteContentRepository.save(any(SiteContent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UUID adminId = UUID.randomUUID();
        ContactChannelsUpdateRequestDto dto = ContactChannelsUpdateRequestDto.builder()
                .email("contato@wbscouting.com")
                .whatsappNumber("5511999998888")
                .whatsappDefaultMessagePt("Mensagem PT")
                .addressPt("Endereço PT")
                .officeHoursPt("09h às 18h")
                .build();

        ContactChannelsPublicDto updated = contactChannelsService.updateContactChannels(dto, adminId);

        assertThat(updated).isNotNull();
        verify(siteContentRepository).save(any(SiteContent.class));
    }
}

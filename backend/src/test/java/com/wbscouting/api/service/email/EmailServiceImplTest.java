package com.wbscouting.api.service.email;

import com.wbscouting.api.config.MailProperties;
import com.wbscouting.api.dto.CandidateApplicationDto;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private SpringTemplateEngine templateEngine;

    private MailProperties mailProperties;

    private EmailServiceImpl emailService;

    @BeforeEach
    void setUp() {
        mailProperties = new MailProperties();
        mailProperties.setFromAddress("no-reply@wbagency.com");
        mailProperties.setFromName("WB Agency");
        mailProperties.setAgencyNotificationEmail("contato@wbagency.com");
        mailProperties.setResetPasswordBaseUrl("http://localhost:4200/admin/reset-password");

        emailService = new EmailServiceImpl(mailSender, templateEngine, mailProperties);
    }

    @Test
    @DisplayName("Deve enviar e-mail de recuperação de senha com sucesso")
    void shouldSendPasswordResetEmailSuccessfully() {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("email/password-reset"), any(Context.class)))
                .thenReturn("<html>Corpo de Redefinição</html>");

        emailService.sendPasswordResetEmail("admin@wbscouting.com", "Admin Geral", "token-xyz-123");

        verify(mailSender, times(1)).send(mimeMessage);
        verify(templateEngine, times(1)).process(eq("email/password-reset"), any(Context.class));
    }

    @Test
    @DisplayName("Deve capturar exceção e garantir resiliência caso ocorra erro no envio de recuperação de senha")
    void shouldHandleExceptionResilientlyOnPasswordReset() {
        when(mailSender.createMimeMessage()).thenThrow(new MailSendException("SMTP Timeout"));

        assertThatCode(() ->
                emailService.sendPasswordResetEmail("admin@wbscouting.com", "Admin Geral", "token-xyz-123")
        ).doesNotThrowAnyException();

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Deve enviar e-mail de notificação de candidatura com sucesso")
    void shouldSendCandidateApplicationNotificationSuccessfully() {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("email/candidate-application"), any(Context.class)))
                .thenReturn("<html>Ficha do Candidato</html>");

        CandidateApplicationDto candidateDto = CandidateApplicationDto.builder()
                .id(UUID.randomUUID())
                .fullName("Larissa Lima")
                .email("larissa@exemplo.com")
                .phone("(11) 98888-7777")
                .age(22)
                .gender("Feminino")
                .heightCm(new BigDecimal("178.00"))
                .weightKg(new BigDecimal("57.00"))
                .instagramHandle("@larissalima")
                .createdAt(OffsetDateTime.now())
                .photos(List.of(
                        CandidateApplicationDto.CandidatePhotoDto.builder()
                                .photoPosition((short) 1)
                                .fileUrl("https://storage.wbscouting.com/p1.jpg")
                                .build()
                ))
                .build();

        emailService.sendCandidateApplicationNotification("contato@wbscouting.com", candidateDto);

        verify(mailSender, times(1)).send(mimeMessage);
        verify(templateEngine, times(1)).process(eq("email/candidate-application"), any(Context.class));
    }

    @Test
    @DisplayName("Deve capturar exceção e garantir resiliência caso ocorra erro no envio de candidatura")
    void shouldHandleExceptionResilientlyOnCandidateNotification() {
        when(mailSender.createMimeMessage()).thenThrow(new MailSendException("Falha de conexão"));

        CandidateApplicationDto candidateDto = CandidateApplicationDto.builder()
                .fullName("Larissa Lima")
                .build();

        assertThatCode(() ->
                emailService.sendCandidateApplicationNotification("contato@wbscouting.com", candidateDto)
        ).doesNotThrowAnyException();
    }
}

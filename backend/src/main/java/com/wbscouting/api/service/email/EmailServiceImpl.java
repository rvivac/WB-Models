package com.wbscouting.api.service.email;

import com.wbscouting.api.config.MailProperties;
import com.wbscouting.api.dto.CandidateApplicationDto;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.nio.charset.StandardCharsets;
import java.time.Year;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    private final MailProperties mailProperties;

    @Async("emailExecutor")
    @Override
    public void sendPasswordResetEmail(String recipientEmail, String recipientName, String resetToken) {
        String threadName = Thread.currentThread().getName();
        log.info("[{}] Iniciando envio de e-mail de recuperação de senha para: {}", threadName, recipientEmail);

        try {
            String resetUrl = mailProperties.getResetPasswordBaseUrl();
            if (resetUrl.contains("?")) {
                resetUrl += "&token=" + resetToken;
            } else {
                resetUrl += "?token=" + resetToken;
            }

            Context context = new Context();
            context.setVariable("recipientName", recipientName != null ? recipientName : "Administrador");
            context.setVariable("resetUrl", resetUrl);
            context.setVariable("currentYear", Year.now().getValue());

            String htmlBody = templateEngine.process("email/password-reset", context);
            String subject = "WB Scouting - Redefinição de Senha de Acesso";

            sendHtmlEmail(recipientEmail, subject, htmlBody);
            log.info("[{}] E-mail de recuperação de senha enviado com sucesso para: {}", threadName, recipientEmail);
        } catch (Exception ex) {
            log.error("[{}] Falha ao processar e enviar e-mail de recuperação de senha para '{}': {}",
                    threadName, recipientEmail, ex.getMessage(), ex);
        }
    }

    @Async("emailExecutor")
    @Override
    public void sendCandidateApplicationNotification(String recipientEmail, CandidateApplicationDto candidateData) {
        String threadName = Thread.currentThread().getName();
        log.info("[{}] Iniciando envio de notificação de candidatura ({}) para: {}",
                threadName, candidateData.getFullName(), recipientEmail);

        try {
            Context context = new Context();
            context.setVariable("candidate", candidateData);
            context.setVariable("currentYear", Year.now().getValue());

            String htmlBody = templateEngine.process("email/candidate-application", context);
            String subject = "WB Scouting - Nova Candidatura Recebida: " + candidateData.getFullName();

            sendHtmlEmail(recipientEmail, subject, htmlBody);
            log.info("[{}] E-mail de candidatura enviado com sucesso para: {}", threadName, recipientEmail);
        } catch (Exception ex) {
            log.error("[{}] Falha ao processar e enviar notificação de candidatura de '{}' para '{}': {}",
                    threadName, candidateData.getFullName(), recipientEmail, ex.getMessage(), ex);
        }
    }

    private void sendHtmlEmail(String recipientEmail, String subject, String htmlBody) throws Exception {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(
                mimeMessage,
                MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                StandardCharsets.UTF_8.name()
        );

        InternetAddress fromAddress = new InternetAddress(
                mailProperties.getFromAddress(),
                mailProperties.getFromName(),
                StandardCharsets.UTF_8.name()
        );

        helper.setFrom(fromAddress);
        helper.setTo(recipientEmail);
        helper.setSubject(subject);
        helper.setText(htmlBody, true);

        mailSender.send(mimeMessage);
    }
}

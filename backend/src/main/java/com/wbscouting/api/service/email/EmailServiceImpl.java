package com.wbscouting.api.service.email;

import com.wbscouting.api.config.EmailConfig;
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
import org.springframework.util.StringUtils;
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

    private boolean isNoOpMode() {
        return mailSender instanceof EmailConfig.NoOpJavaMailSender;
    }

    @Async("emailExecutor")
    @Override
    public void sendPasswordResetEmail(String recipientEmail, String recipientName, String resetToken) {
        String threadName = Thread.currentThread().getName();

        if (!StringUtils.hasText(recipientEmail) || !StringUtils.hasText(resetToken)) {
            log.warn("[{}] [PASSWORD-RESET] Parâmetros inválidos para envio de e-mail: recipientEmail='{}', resetTokenPresente={}",
                    threadName, recipientEmail, StringUtils.hasText(resetToken));
            return;
        }

        if (isNoOpMode()) {
            log.warn("[{}] [PASSWORD-RESET] MODO MOCK: e-mail NÃO ENVIADO para '{}'. Configurar SPRING_MAIL_HOST/USERNAME/PASSWORD para envios reais.",
                    threadName, recipientEmail);
            return;
        }

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
            String subject = "WB Agency - Redefinição de Senha de Acesso";

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

        if (!StringUtils.hasText(recipientEmail) || candidateData == null) {
            log.warn("[{}] [CANDIDATE-NOTIFY] Parâmetros inválidos para envio de notificação: recipientEmail='{}', candidateDataNull={}",
                    threadName, recipientEmail, candidateData == null);
            return;
        }

        if (isNoOpMode()) {
            log.warn("[{}] [CANDIDATE-NOTIFY] MODO MOCK: e-mail NÃO ENVIADO para '{}' referente a candidatura de '{}'. " +
                            "Configurar SPRING_MAIL_HOST/USERNAME/PASSWORD para envios reais.",
                    threadName, recipientEmail, candidateData.getFullName());
            return;
        }

        String nomeCandidato = candidateData.getFullName() != null ? candidateData.getFullName() : "<Candidato sem nome>";
        log.info("[{}] Iniciando envio de notificação de candidatura ({}) para: {}",
                threadName, nomeCandidato, recipientEmail);

        try {
            Context context = new Context();
            context.setVariable("candidate", candidateData);
            context.setVariable("currentYear", Year.now().getValue());

            String htmlBody = templateEngine.process("email/candidate-application", context);
            String sanitizedFullName = sanitizeHeader(nomeCandidato);
            String subject = sanitizeHeader("WB Agency - Nova Candidatura Recebida: " + sanitizedFullName);

            sendHtmlEmail(recipientEmail, subject, htmlBody);
            log.info("[{}] E-mail de candidatura enviado com sucesso para: {}", threadName, recipientEmail);
        } catch (Exception ex) {
            log.error("[{}] Falha ao processar e enviar notificação de candidatura de '{}' para '{}': {}",
                    threadName, nomeCandidato, recipientEmail, ex.getMessage(), ex);
        }
    }

    private void sendHtmlEmail(String recipientEmail, String subject, String htmlBody) throws Exception {
        if (isNoOpMode()) {
            return;
        }

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(
                mimeMessage,
                MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                StandardCharsets.UTF_8.name()
        );

        InternetAddress fromAddress = new InternetAddress(
                sanitizeHeader(mailProperties.getFromAddress()),
                sanitizeHeader(mailProperties.getFromName()),
                StandardCharsets.UTF_8.name()
        );

        helper.setFrom(fromAddress);
        helper.setTo(sanitizeHeader(recipientEmail));
        helper.setSubject(sanitizeHeader(subject));
        helper.setText(htmlBody != null ? htmlBody : "", true);

        try {
            mailSender.send(mimeMessage);
        } catch (Exception sendEx) {
            log.error("Falha de transporte SMTP ao enviar e-mail PARA='{}' ASSUNTO='{}'. " +
                            "Verificar: SPRING_MAIL_HOST/PORT, credenciais (SPRING_MAIL_USERNAME/PASSWORD), firewall/porta 587 liberada, " +
                            "provedor (ex: Gmail requer app-senha + 2FA, ou usar SendGrid/Resend/Mailgun). Erro: {}",
                    recipientEmail, subject, sendEx.getMessage(), sendEx);
            throw sendEx;
        }
    }

    /**
     * Expurgador rigoroso de caracteres de quebra de linha e injeção de cabeçalhos SMTP/CRLF (Item 9 - EAP-SEG-002).
     */
    private String sanitizeHeader(String input) {
        if (input == null) {
            return "";
        }
        return input.replaceAll("(?i)[\\r\\n]|%0d|%0a", " ").trim().replaceAll("\\s{2,}", " ");
    }
}

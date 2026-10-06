package com.wbscouting.api.config;

import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.util.Properties;

@Slf4j
@Configuration
public class EmailConfig {

    private static final String NO_OP_WARN =
            "[NO-OP MAIL] Serviço de e-mail está em modo MOCK (sem configuração SMTP). " +
            "Defina SPRING_MAIL_HOST, SPRING_MAIL_USERNAME, SPRING_MAIL_PASSWORD nas variáveis de ambiente (Render) para habilitar envios reais.";

    @Bean
    @Primary
    public JavaMailSender javaMailSender(
            @Value("${spring.mail.host:}") String host,
            @Value("${spring.mail.port:587}") int port,
            @Value("${spring.mail.username:}") String username,
            @Value("${spring.mail.password:}") String password,
            @Value("${spring.mail.protocol:smtp}") String protocol,
            @Value("${spring.mail.default-encoding:UTF-8}") String defaultEncoding,
            @Value("${spring.mail.properties.mail.smtp.auth:true}") boolean smtpAuth,
            @Value("${spring.mail.properties.mail.smtp.starttls.enable:true}") boolean starttlsEnable,
            @Value("${spring.mail.properties.mail.smtp.starttls.required:true}") boolean starttlsRequired,
            @Value("${spring.mail.properties.mail.smtp.connectiontimeout:10000}") int connTimeout,
            @Value("${spring.mail.properties.mail.smtp.timeout:10000}") int readTimeout,
            @Value("${spring.mail.properties.mail.smtp.writetimeout:10000}") int writeTimeout
    ) {
        boolean temCredenciais = StringUtils.hasText(host)
                && StringUtils.hasText(username)
                && StringUtils.hasText(password);

        if (!temCredenciais) {
            log.warn("================================================================");
            log.warn(NO_OP_WARN);
            log.warn("  Host configurado......: [{}]", StringUtils.hasText(host) ? host : "<VAZIO>");
            log.warn("  Username configurado..: [{}]", StringUtils.hasText(username) ? username : "<VAZIO>");
            log.warn("  Envios reais..........: DESABILITADOS (e-mails serão apenas LOGADOS)");
            log.warn("================================================================");
            return new NoOpJavaMailSender();
        }

        log.info("[EMAIL CONFIG] SMTP ativo: host={}:{}, usuario={}, starttls={}, auth={}",
                host, port, username, starttlsEnable, smtpAuth);

        JavaMailSenderImpl impl = new JavaMailSenderImpl();
        impl.setHost(host);
        impl.setPort(port);
        impl.setUsername(username);
        impl.setPassword(password);
        impl.setProtocol(protocol);
        impl.setDefaultEncoding(defaultEncoding);

        Properties props = new Properties();
        props.put("mail.transport.protocol", protocol);
        props.put("mail.smtp.auth", String.valueOf(smtpAuth));
        props.put("mail.smtp.starttls.enable", String.valueOf(starttlsEnable));
        props.put("mail.smtp.starttls.required", String.valueOf(starttlsRequired));
        props.put("mail.smtp.connectiontimeout", String.valueOf(connTimeout));
        props.put("mail.smtp.timeout", String.valueOf(readTimeout));
        props.put("mail.smtp.writetimeout", String.valueOf(writeTimeout));
        props.put("mail.mime.charset", defaultEncoding);
        impl.setJavaMailProperties(props);

        return impl;
    }

    @Slf4j
    public static class NoOpJavaMailSender implements JavaMailSender {

        @Override
        public MimeMessage createMimeMessage() {
            log.debug(NO_OP_WARN);
            return new MimeMessage((Session) null);
        }

        @Override
        public MimeMessage createMimeMessage(InputStream contentStream) throws MailException {
            return createMimeMessage();
        }

        @Override
        public void send(MimeMessage... mimeMessages) throws MailException {
            for (MimeMessage m : mimeMessages) {
                try {
                    Address[] to = m.getRecipients(Message.RecipientType.TO);
                    String subject = m.getSubject();
                    log.warn("[NO-OP MAIL] (send MimeMessage) PARA={} | ASSUNTO='{}' | CORPO NÃO ENVIADO (SMTP desabilitado)",
                            to != null ? formatarDestinatarios(to) : "<destinatario desconhecido>",
                            subject != null ? subject : "<sem assunto>");
                } catch (MessagingException ex) {
                    log.warn("[NO-OP MAIL] (send MimeMessage) Não foi possível extrair detalhes da mensagem: {}", ex.getMessage());
                }
            }
        }

        @Override
        public void send(SimpleMailMessage... simpleMessages) throws MailException {
            for (SimpleMailMessage m : simpleMessages) {
                log.warn("[NO-OP MAIL] (send SimpleMailMessage) PARA={} | ASSUNTO='{}' | CORPO NÃO ENVIADO (SMTP desabilitado)",
                        m.getTo() != null ? String.join(", ", m.getTo()) : "<vazio>",
                        m.getSubject() != null ? m.getSubject() : "<sem assunto>");
            }
        }

        private static String formatarDestinatarios(Address[] addresses) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < addresses.length; i++) {
                if (i > 0) sb.append(",");
                sb.append(addresses[i].toString());
            }
            return sb.toString();
        }
    }
}

package com.wbscouting.api.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.mail")
public class MailProperties {

    private String fromAddress = "no-reply@wbscouting.com";
    private String fromName = "WB Scouting";
    private String agencyNotificationEmail = "contato@wbscouting.com";
    private String resetPasswordBaseUrl = "http://localhost:4200/admin/reset-password";
}

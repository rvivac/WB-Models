package com.wbscouting.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.mail")
public class MailProperties {

    private String fromAddress = "no-reply@wbagency.com.br";
    private String fromName = "WB Agency";
    private String agencyNotificationEmail = "info@wbagency.com.br";
    private String resetPasswordBaseUrl = "http://localhost:4200/admin/reset-password";

    public String getFromAddress() { return fromAddress; }
    public void setFromAddress(String fromAddress) { this.fromAddress = fromAddress; }
    public String getFromName() { return fromName; }
    public void setFromName(String fromName) { this.fromName = fromName; }
    public String getAgencyNotificationEmail() { return agencyNotificationEmail; }
    public void setAgencyNotificationEmail(String agencyNotificationEmail) { this.agencyNotificationEmail = agencyNotificationEmail; }
    public String getResetPasswordBaseUrl() { return resetPasswordBaseUrl; }
    public void setResetPasswordBaseUrl(String resetPasswordBaseUrl) { this.resetPasswordBaseUrl = resetPasswordBaseUrl; }
}
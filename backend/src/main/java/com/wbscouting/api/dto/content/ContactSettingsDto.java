package com.wbscouting.api.dto.content;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContactSettingsDto {

    @NotBlank(message = "O e-mail principal é obrigatório.")
    @Email(message = "Formato de e-mail principal inválido.")
    private String primaryEmail;

    @Email(message = "Formato de e-mail de scouting inválido.")
    private String scoutingEmail;

    @Email(message = "Formato de e-mail de imprensa inválido.")
    private String pressEmail;

    @NotBlank(message = "O telefone é obrigatório.")
    private String phone;

    @NotBlank(message = "O número de WhatsApp é obrigatório.")
    private String whatsapp;

    private String whatsappDefaultMessage;

    @Valid
    private AddressDto address;

    private String businessHours;

    @Valid
    private SocialMediaDto socialMedia;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AddressDto {
        private String street;
        private String complement;
        private String neighborhood;
        private String city;
        private String state;
        private String zipCode;
        private String country;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SocialMediaDto {
        private String instagram;
        private String linkedin;
        private String facebook;
        private String tiktok;
    }
}

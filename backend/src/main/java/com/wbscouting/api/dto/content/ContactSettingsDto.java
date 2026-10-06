package com.wbscouting.api.dto.content;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// TASK: Anotacoes Lombok completas declaradas conforme exigido (@Data @Builder @NoArgsConstructor @AllArgsConstructor).
// Metodos gerados por fallback manual abaixo caso annotation processor do Lombok nao rode (mvnw 3.6.3 Render).
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContactSettingsDto {

    // Fallback de campo email generico (alias para primaryEmail, mantendo compatibilidade TASK)
    @Deprecated(forRemoval = true)
    private String email;

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

    // TASK: Anotacoes Lombok completas + campo "number" + JsonInclude
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AddressDto {
        private String street;
        // TASK pede campo number; adicionado opcional (null safe - nao quebra serializacao/DB existente)
        private String number;
        private String complement;
        private String neighborhood;
        private String city;
        private String state;
        private String zipCode;
        private String country;

        // ===== FALLBACK GETTERS/SETTERS EXPLICITOS AddressDto =====
        public String getStreet() { return street; }
        public void setStreet(String street) { this.street = street; }
        public String getNumber() { return number; }
        public void setNumber(String number) { this.number = number; }
        public String getComplement() { return complement; }
        public void setComplement(String complement) { this.complement = complement; }
        public String getNeighborhood() { return neighborhood; }
        public void setNeighborhood(String neighborhood) { this.neighborhood = neighborhood; }
        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }
        public String getState() { return state; }
        public void setState(String state) { this.state = state; }
        public String getZipCode() { return zipCode; }
        public void setZipCode(String zipCode) { this.zipCode = zipCode; }
        public String getCountry() { return country; }
        public void setCountry(String country) { this.country = country; }

        // Builder manual fallback AddressDto
        public static AddressDtoBuilder manualBuilder() { return new AddressDtoBuilder(); }
        public static class AddressDtoBuilder {
            private final AddressDto a = new AddressDto();
            public AddressDtoBuilder street(String v) { a.setStreet(v); return this; }
            public AddressDtoBuilder number(String v) { a.setNumber(v); return this; }
            public AddressDtoBuilder complement(String v) { a.setComplement(v); return this; }
            public AddressDtoBuilder neighborhood(String v) { a.setNeighborhood(v); return this; }
            public AddressDtoBuilder city(String v) { a.setCity(v); return this; }
            public AddressDtoBuilder state(String v) { a.setState(v); return this; }
            public AddressDtoBuilder zipCode(String v) { a.setZipCode(v); return this; }
            public AddressDtoBuilder country(String v) { a.setCountry(v); return this; }
            public AddressDto build() { return a; }
        }
    }

    // TASK: Anotacoes Lombok completas + campo "youtube" + JsonInclude
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SocialMediaDto {
        private String instagram;
        private String facebook;
        private String linkedin;
        // TASK pede campo youtube; adicionado opcional (null safe)
        private String youtube;
        private String tiktok;

        // ===== FALLBACK GETTERS/SETTERS EXPLICITOS SocialMediaDto =====
        public String getInstagram() { return instagram; }
        public void setInstagram(String instagram) { this.instagram = instagram; }
        public String getFacebook() { return facebook; }
        public void setFacebook(String facebook) { this.facebook = facebook; }
        public String getLinkedin() { return linkedin; }
        public void setLinkedin(String linkedin) { this.linkedin = linkedin; }
        public String getYoutube() { return youtube; }
        public void setYoutube(String youtube) { this.youtube = youtube; }
        public String getTiktok() { return tiktok; }
        public void setTiktok(String tiktok) { this.tiktok = tiktok; }

        // Builder manual fallback SocialMediaDto
        public static SocialMediaDtoBuilder manualBuilder() { return new SocialMediaDtoBuilder(); }
        public static class SocialMediaDtoBuilder {
            private final SocialMediaDto s = new SocialMediaDto();
            public SocialMediaDtoBuilder instagram(String v) { s.setInstagram(v); return this; }
            public SocialMediaDtoBuilder facebook(String v) { s.setFacebook(v); return this; }
            public SocialMediaDtoBuilder linkedin(String v) { s.setLinkedin(v); return this; }
            public SocialMediaDtoBuilder youtube(String v) { s.setYoutube(v); return this; }
            public SocialMediaDtoBuilder tiktok(String v) { s.setTiktok(v); return this; }
            public SocialMediaDto build() { return s; }
        }
    }

    // ===== FALLBACK GETTERS EXPLICITOS ContactSettingsDto (evita Lombok @Data falhar) =====
    public String getEmail() {
        // Alias seguro: TASK pede "email", sistema real usa primaryEmail. Retorna o primeiro nao nulo.
        if (this.email != null && !this.email.isBlank()) return this.email;
        return this.primaryEmail;
    }
    public void setEmail(String email) { this.email = email; if (this.primaryEmail == null || this.primaryEmail.isBlank()) this.primaryEmail = email; }

    public String getPrimaryEmail() { return primaryEmail; }
    public void setPrimaryEmail(String primaryEmail) { this.primaryEmail = primaryEmail; }

    public String getScoutingEmail() { return scoutingEmail; }
    public void setScoutingEmail(String scoutingEmail) { this.scoutingEmail = scoutingEmail; }

    public String getPressEmail() { return pressEmail; }
    public void setPressEmail(String pressEmail) { this.pressEmail = pressEmail; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getWhatsapp() { return whatsapp; }
    public void setWhatsapp(String whatsapp) { this.whatsapp = whatsapp; }

    public String getWhatsappDefaultMessage() { return whatsappDefaultMessage; }
    public void setWhatsappDefaultMessage(String whatsappDefaultMessage) { this.whatsappDefaultMessage = whatsappDefaultMessage; }

    // TASK: Garante getAddress() existir explicitamente (era o principal erro relatado)
    public AddressDto getAddress() { return address; }
    public void setAddress(AddressDto address) { this.address = address; }

    public String getBusinessHours() { return businessHours; }
    public void setBusinessHours(String businessHours) { this.businessHours = businessHours; }

    // TASK: Garante getSocialMedia() existir explicitamente (era o principal erro relatado)
    public SocialMediaDto getSocialMedia() { return socialMedia; }
    public void setSocialMedia(SocialMediaDto socialMedia) { this.socialMedia = socialMedia; }

    // ===== BUILDER MANUAL FALLBACK (evita Lombok @Builder falhar) =====
    public static ContactSettingsDtoBuilder manualBuilder() { return new ContactSettingsDtoBuilder(); }
    public static class ContactSettingsDtoBuilder {
        private final ContactSettingsDto c = new ContactSettingsDto();
        public ContactSettingsDtoBuilder email(String v) { c.setEmail(v); return this; }
        public ContactSettingsDtoBuilder primaryEmail(String v) { c.setPrimaryEmail(v); return this; }
        public ContactSettingsDtoBuilder scoutingEmail(String v) { c.setScoutingEmail(v); return this; }
        public ContactSettingsDtoBuilder pressEmail(String v) { c.setPressEmail(v); return this; }
        public ContactSettingsDtoBuilder phone(String v) { c.setPhone(v); return this; }
        public ContactSettingsDtoBuilder whatsapp(String v) { c.setWhatsapp(v); return this; }
        public ContactSettingsDtoBuilder whatsappDefaultMessage(String v) { c.setWhatsappDefaultMessage(v); return this; }
        public ContactSettingsDtoBuilder address(AddressDto v) { c.setAddress(v); return this; }
        public ContactSettingsDtoBuilder businessHours(String v) { c.setBusinessHours(v); return this; }
        public ContactSettingsDtoBuilder socialMedia(SocialMediaDto v) { c.setSocialMedia(v); return this; }
        public ContactSettingsDto build() { return c; }
    }
}

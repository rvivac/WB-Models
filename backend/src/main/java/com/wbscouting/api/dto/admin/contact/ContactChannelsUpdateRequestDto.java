package com.wbscouting.api.dto.admin.contact;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactChannelsUpdateRequestDto {

    @NotBlank(message = "O e-mail de contato é obrigatório")
    @Email(message = "Formato de e-mail inválido")
    private String email;

    @NotBlank(message = "O número de WhatsApp é obrigatório")
    @Pattern(regexp = "^[0-9]{10,15}$", message = "WhatsApp deve conter apenas dígitos (código do país e DDD, entre 10 e 15 dígitos)")
    private String whatsappNumber;

    @NotBlank(message = "A mensagem padrão do WhatsApp em português é obrigatória")
    private String whatsappDefaultMessagePt;

    private String whatsappDefaultMessageEn;

    private String instagramHandle;

    @NotBlank(message = "O endereço em português é obrigatório")
    private String addressPt;

    private String addressEn;

    @NotBlank(message = "O horário de atendimento em português é obrigatório")
    private String officeHoursPt;

    private String officeHoursEn;
}

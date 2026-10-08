package com.wbscouting.api.dto.content;

import com.wbscouting.api.entity.ContactChannel;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContactChannelDto {

    private UUID id;

    @NotBlank(message = "Tipo do canal é obrigatório (e.g. WHATSAPP, EMAIL, PHONE, INSTAGRAM)")
    private String type;

    @NotBlank(message = "Valor ou URL do canal é obrigatório")
    private String value;

    @NotBlank(message = "Rótulo/Label do canal é obrigatório")
    private String label;

    @Builder.Default
    private Boolean active = true;

    @Builder.Default
    private Integer displayOrder = 0;

    public static ContactChannelDto fromEntity(ContactChannel entity) {
        if (entity == null) return null;
        return ContactChannelDto.builder()
                .id(entity.getId())
                .type(entity.getType())
                .value(entity.getValue())
                .label(entity.getLabel())
                .active(entity.getActive())
                .displayOrder(entity.getDisplayOrder())
                .build();
    }
}

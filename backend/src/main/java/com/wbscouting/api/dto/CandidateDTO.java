package com.wbscouting.api.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class CandidateDTO {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ApplicationRequest {
        @NotBlank(message = "Nome completo é obrigatório")
        @Size(max = 200)
        private String fullName;

        @NotBlank(message = "Email é obrigatório")
        @Email(message = "Email inválido")
        private String email;

        @NotBlank(message = "Telefone é obrigatório")
        private String phone;

        @NotNull(message = "Idade é obrigatória")
        @Min(value = 1, message = "Idade inválida")
        private Integer age;

        private String guardianName;

        @NotBlank(message = "Gênero é obrigatório")
        private String gender;

        @NotNull(message = "Altura é obrigatória")
        private BigDecimal heightCm;

        private BigDecimal weightKg;
        private BigDecimal bustChestCm;
        private BigDecimal waistCm;
        private BigDecimal hipsCm;
        private String instagramHandle;
        private String tiktokHandle;

        @NotNull(message = "O consentimento da LGPD é obrigatório")
        @AssertTrue(message = "Você deve aceitar os termos da LGPD")
        private Boolean lgpdAccepted;

        @NotEmpty(message = "Envie pelo menos 1 foto para avaliação")
        @Size(max = 8, message = "Limite máximo de 8 fotos")
        private List<PhotoUploadRequest> photos;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PhotoUploadRequest {
        @NotNull(message = "Posição da foto é obrigatória (1 a 8)")
        @Min(1)
        @Max(8)
        private Short photoPosition;

        @NotBlank(message = "URL do arquivo é obrigatória")
        private String fileUrl;

        @NotBlank(message = "Path de armazenamento é obrigatório")
        private String filePath;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private UUID id;
        private String fullName;
        private String email;
        private String phone;
        private Integer age;
        private String gender;
        private BigDecimal heightCm;
        private OffsetDateTime createdAt;
        private Integer photoCount;
    }
}

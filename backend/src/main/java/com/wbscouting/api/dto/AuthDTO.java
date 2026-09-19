package com.wbscouting.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class AuthDTO {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LoginRequest {
        @NotBlank(message = "O email é obrigatório")
        @Email(message = "Formato de email inválido")
        private String email;

        @NotBlank(message = "A senha é obrigatória")
        private String password;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AuthResponse {
        private String token;
        @Builder.Default
        private String tokenType = "Bearer";
        private String name;
        private String email;
        private String role;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ForgotPasswordRequest {
        @NotBlank(message = "O email é obrigatório")
        @Email(message = "Formato de email inválido")
        private String email;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ResetPasswordRequest {
        @NotBlank(message = "O token de recuperação é obrigatório")
        private String token;

        @NotBlank(message = "A nova senha é obrigatória")
        @jakarta.validation.constraints.Size(min = 8, message = "A senha deve conter no mínimo 8 caracteres")
        private String newPassword;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MessageResponse {
        private String message;
    }
}

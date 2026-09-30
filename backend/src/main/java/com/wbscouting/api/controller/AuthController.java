package com.wbscouting.api.controller;

import com.wbscouting.api.dto.auth.ForgotPasswordRequestDto;
import com.wbscouting.api.dto.auth.LoginRequestDto;
import com.wbscouting.api.dto.auth.LoginResponseDto;
import com.wbscouting.api.dto.auth.ResetPasswordRequestDto;
import com.wbscouting.api.service.auth.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDto request) {
        authService.processForgotPassword(request);
        return ResponseEntity.ok(Map.of(
                "message", "Se o e-mail informado estiver cadastrado em nosso sistema, as instruções para redefinição de senha serão enviadas em instantes."
        ));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequestDto request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(Map.of(
                "message", "Senha redefinida com sucesso."
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response) {
        authService.logout(request, response);
        return ResponseEntity.noContent().build();
    }
}



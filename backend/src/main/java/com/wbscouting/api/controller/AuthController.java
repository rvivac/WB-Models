package com.wbscouting.api.controller;

import com.wbscouting.api.dto.auth.ForgotPasswordRequestDto;
import com.wbscouting.api.dto.auth.LoginRequestDto;
import com.wbscouting.api.dto.auth.LoginResponseDto;
import com.wbscouting.api.dto.auth.ResetPasswordRequestDto;
import com.wbscouting.api.entity.AdminLoginHistory;
import com.wbscouting.api.repository.AdminLoginHistoryRepository;
import com.wbscouting.api.repository.AdminRepository;
import com.wbscouting.api.service.auth.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping({"/auth", "/api/v1/auth"})
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final com.wbscouting.api.service.audit.AuditLogService auditLogService;
    /** Repositorio de auditoria historica de acessos admin. */
    private final AdminLoginHistoryRepository adminLoginHistoryRepository;
    /** Lookup rapido de admin por email para capturar o UUID do usuario autenticado. */
    private final AdminRepository adminRepository;

    /**
     * Persiste o historico de login na tabela admin_login_history.<br>
     * <strong>FAIL-SAFE</strong>: Qualquer falha aqui NAO cancela o login do usuario.
     * Isso evita indisponibilidade do portal em caso de problema de IO/DB na tabela de auditoria.
     */
    private void recordLoginHistory(LoginResponseDto response, String clientIp, String userAgent) {
        if (response == null || response.getAdminEmail() == null || response.getAccessToken() == null) {
            return; // Autenticacao ainda nao completou (ex: desafio 2FA pendente). Aguardar etapa final.
        }
        try {
            adminRepository.findByEmailAndIsActiveTrue(response.getAdminEmail().trim().toLowerCase())
                    .ifPresentOrElse(
                            admin -> adminLoginHistoryRepository.save(
                                    AdminLoginHistory.builder()
                                            .adminId(admin.getId())
                                            .ipAddress(clientIp)
                                            .userAgent(truncateUserAgent(userAgent))
                                            .loggedAt(java.time.OffsetDateTime.now())
                                            .build()
                            ),
                            () -> org.slf4j.LoggerFactory.getLogger(AuthController.class)
                                    .warn("[AUTH-HISTORY] Nao foi possivel encontrar admin ativo {} para gravar historico de login (desincronia transacional rara).",
                                            response.getAdminEmail())
                    );
        } catch (Exception ex) {
            // FAIL-SAFE: Nunca deixa auditoria quebrar login do usuario.
            org.slf4j.LoggerFactory.getLogger(AuthController.class)
                    .warn("[AUTH-HISTORY] Falha ao gravar historico de login para '{}' (FAIL-SAFE: login nao foi interrompido). Causa: {}",
                            response.getAdminEmail(), ex.getMessage());
        }
    }

    /**
     * Trunca User-Agent caso ultrapasse 2000 caracteres (limite razoavel de TEXT;
     * evita DoS acidental ou malicioso de payload gigante).
     */
    private String truncateUserAgent(String userAgent) {
        if (userAgent == null) return null;
        return userAgent.length() > 2000 ? userAgent.substring(0, 2000) : userAgent;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(
            @Valid @RequestBody LoginRequestDto request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        String clientIp = extractClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        try {
            LoginResponseDto response = authService.login(request);

            // 🔥 Persiste HISTORICO DE LOGIN na tabela admin_login_history (IP + UA + data).
            //    FAIL-SAFE: nunca cancela o login se a tabela falhar.
            recordLoginHistory(response, clientIp, userAgent);

            if (response.getAccessToken() != null) {
                boolean isSecure = httpRequest.isSecure() || "https".equalsIgnoreCase(httpRequest.getHeader("X-Forwarded-Proto"));
                ResponseCookie jwtCookie = ResponseCookie.from("jwt_token", response.getAccessToken())
                        .httpOnly(true)
                        .secure(isSecure)
                        .path("/")
                        .maxAge(response.getExpiresIn() != null ? response.getExpiresIn() : 28800)
                        .sameSite("Strict")
                        .build();

                httpResponse.addHeader(HttpHeaders.SET_COOKIE, jwtCookie.toString());
            }

            auditLogService.recordLogAsync(
                    null,
                    request != null ? request.getEmail() : null,
                    "LOGIN",
                    "AUTH",
                    null,
                    "Autenticação de administrador realizada com sucesso",
                    Map.of("email", request != null ? request.getEmail() : ""),
                    clientIp,
                    userAgent
            );

            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            auditLogService.recordLogAsync(
                    null,
                    request != null ? request.getEmail() : "desconhecido",
                    "LOGIN_FAILED",
                    "AUTH",
                    null,
                    "Tentativa de login falhou: " + ex.getMessage(),
                    Map.of("error", ex.getClass().getSimpleName()),
                    clientIp,
                    userAgent
            );
            throw ex;
        }
    }

    @PostMapping("/2fa/challenge")
    public ResponseEntity<LoginResponseDto> challenge2fa(
            @Valid @RequestBody com.wbscouting.api.dto.auth.TwoFactorChallengeRequestDto request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        LoginResponseDto response = authService.challenge2fa(request);

        // 🔥 Persiste HISTORICO DE LOGIN tambem no sucesso do desafio 2FA.
        String clientIp = extractClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        recordLoginHistory(response, clientIp, userAgent);

        if (response.getAccessToken() != null) {
            boolean isSecure = httpRequest.isSecure() || "https".equalsIgnoreCase(httpRequest.getHeader("X-Forwarded-Proto"));
            ResponseCookie jwtCookie = ResponseCookie.from("jwt_token", response.getAccessToken())
                    .httpOnly(true)
                    .secure(isSecure)
                    .path("/")
                    .maxAge(response.getExpiresIn() != null ? response.getExpiresIn() : 28800)
                    .sameSite("Strict")
                    .build();

            httpResponse.addHeader(HttpHeaders.SET_COOKIE, jwtCookie.toString());
        }

        return ResponseEntity.ok(response);
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
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        String clientIp = extractClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        String operator = "anonymous";
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null) {
            operator = auth.getName();
        }

        authService.logout(request, response);

        auditLogService.recordLogAsync(
                null,
                operator,
                "LOGOUT",
                "AUTH",
                null,
                "Encerramento de sessão (Logout) seguro de administrador",
                Map.of("operator", operator),
                clientIp,
                userAgent
        );

        boolean isSecure = request.isSecure() || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));
        ResponseCookie deleteCookie = ResponseCookie.from("jwt_token", "")
                .httpOnly(true)
                .secure(isSecure)
                .path("/")
                .maxAge(0)
                .sameSite("Strict")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString());
        return ResponseEntity.noContent().build();
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            String[] ips = xForwardedFor.split(",");
            if (ips.length > 0) {
                return ips[0].trim();
            }
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp.trim();
        }
        return request.getRemoteAddr();
    }
}



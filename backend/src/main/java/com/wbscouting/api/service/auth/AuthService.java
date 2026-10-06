package com.wbscouting.api.service.auth;

import com.wbscouting.api.dto.AuthDTO;
import com.wbscouting.api.dto.auth.ForgotPasswordRequestDto;
import com.wbscouting.api.dto.auth.LoginRequestDto;
import com.wbscouting.api.dto.auth.LoginResponseDto;
import com.wbscouting.api.dto.auth.ResetPasswordRequestDto;

public interface AuthService {

    /**
     * Autenticacao padrao com auditoria completa de IP e User-Agent para
     * gravar historico de login na tabela admin_login_history.
     */
    LoginResponseDto login(LoginRequestDto request, String clientIp, String userAgent);

    /** Desafio 2FA finalizado com sucesso, tambem registra historico de login. */
    LoginResponseDto challenge2fa(com.wbscouting.api.dto.auth.TwoFactorChallengeRequestDto request, String clientIp, String userAgent);

    /**
     * Overload legado SEM auditoria de IP/UA. Mantido para compatibilidade com
     * testes unitarios (ex: AdminInitialPasswordRejectionTest.shouldRejectLogin...).
     * Equivale a chamar login(request, null, null).
     */
    default LoginResponseDto login(LoginRequestDto request) {
        return login(request, null, null);
    }

    /** Overload legado SEM auditoria (para testes / codigo que nao tem HttpServletRequest). */
    default LoginResponseDto challenge2fa(com.wbscouting.api.dto.auth.TwoFactorChallengeRequestDto request) {
        return challenge2fa(request, null, null);
    }

    void processForgotPassword(ForgotPasswordRequestDto request);

    void resetPassword(ResetPasswordRequestDto request);

    AuthDTO.MessageResponse forgotPassword(AuthDTO.ForgotPasswordRequest request);

    AuthDTO.MessageResponse resetPassword(AuthDTO.ResetPasswordRequest request);

    void logout(jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response);
}


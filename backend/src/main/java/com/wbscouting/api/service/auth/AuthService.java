package com.wbscouting.api.service.auth;

import com.wbscouting.api.dto.AuthDTO;
import com.wbscouting.api.dto.auth.ForgotPasswordRequestDto;
import com.wbscouting.api.dto.auth.LoginRequestDto;
import com.wbscouting.api.dto.auth.LoginResponseDto;
import com.wbscouting.api.dto.auth.ResetPasswordRequestDto;

public interface AuthService {

    LoginResponseDto login(LoginRequestDto request);

    void processForgotPassword(ForgotPasswordRequestDto request);

    void resetPassword(ResetPasswordRequestDto request);

    AuthDTO.MessageResponse forgotPassword(AuthDTO.ForgotPasswordRequest request);

    AuthDTO.MessageResponse resetPassword(AuthDTO.ResetPasswordRequest request);
}


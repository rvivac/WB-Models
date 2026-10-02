package com.wbscouting.api.service.admin;

import com.wbscouting.api.dto.admin.ChangePasswordRequestDto;
import com.wbscouting.api.dto.auth.TwoFactorConfirmRequestDto;
import com.wbscouting.api.dto.auth.TwoFactorConfirmResponseDto;
import com.wbscouting.api.dto.auth.TwoFactorDisableRequestDto;
import com.wbscouting.api.dto.auth.TwoFactorSetupResponseDto;

import java.util.Map;

public interface AdminProfileService {

    void changePassword(String email, ChangePasswordRequestDto request);

    TwoFactorSetupResponseDto setup2fa(String email);

    TwoFactorConfirmResponseDto confirm2fa(String email, TwoFactorConfirmRequestDto request);

    void disable2fa(String email, TwoFactorDisableRequestDto request);

    Map<String, Object> get2faStatus(String email);
}

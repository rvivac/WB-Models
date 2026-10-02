package com.wbscouting.api.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TwoFactorSetupResponseDto {
    private String secret;
    private String otpauthUrl;
    private String qrCodeDataUrl;
}

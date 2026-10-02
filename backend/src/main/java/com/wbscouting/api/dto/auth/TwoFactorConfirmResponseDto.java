package com.wbscouting.api.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TwoFactorConfirmResponseDto {
    private boolean enabled;
    private List<String> backupCodes;
}

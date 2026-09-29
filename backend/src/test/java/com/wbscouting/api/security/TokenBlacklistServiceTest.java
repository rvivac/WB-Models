package com.wbscouting.api.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class TokenBlacklistServiceTest {

    private TokenBlacklistService blacklistService;

    @BeforeEach
    void setUp() {
        blacklistService = new TokenBlacklistService();
    }

    @Test
    @DisplayName("Deve revogar token e confirmar que está na blacklist")
    void shouldBlacklistTokenSuccessfully() {
        String token = "valid-test-token-12345";
        Date expiration = new Date(System.currentTimeMillis() + 3600000); // 1 hora no futuro

        assertThat(blacklistService.isBlacklisted(token)).isFalse();

        blacklistService.blacklistToken(token, expiration);

        assertThat(blacklistService.isBlacklisted(token)).isTrue();
    }

    @Test
    @DisplayName("Não deve considerar revogado token já expirado naturalmente")
    void shouldNotConsiderExpiredTokenAsBlacklisted() {
        String token = "expired-token-999";
        Date pastExpiration = new Date(System.currentTimeMillis() - 1000); // 1 segundo no passado

        blacklistService.blacklistToken(token, pastExpiration);

        assertThat(blacklistService.isBlacklisted(token)).isFalse();
    }

    @Test
    @DisplayName("Deve ignorar strings de token nulas ou vazias")
    void shouldHandleNullOrEmptyTokensGracefully() {
        blacklistService.blacklistToken(null, new Date());
        blacklistService.blacklistToken("", new Date());
        blacklistService.blacklistToken("   ", new Date());

        assertThat(blacklistService.isBlacklisted(null)).isFalse();
        assertThat(blacklistService.isBlacklisted("")).isFalse();
    }
}

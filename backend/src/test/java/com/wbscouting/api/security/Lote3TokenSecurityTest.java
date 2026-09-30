package com.wbscouting.api.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EAP-SEG-002: Lote 3 - Tokens Criptográficos e Hash SHA-256 (Item 14)")
class Lote3TokenSecurityTest {

    @Test
    @DisplayName("TokenHashUtils: Gera tokens seguros únicos de 32 bytes (64 hex)")
    void shouldGenerateSecureUniqueTokens() {
        Set<String> generatedTokens = new HashSet<>();
        for (int i = 0; i < 50; i++) {
            String token = TokenHashUtils.generateSecureToken();
            assertThat(token).hasSize(64);
            assertThat(token).matches("^[a-f0-9]{64}$");
            generatedTokens.add(token);
        }
        // Todos os 50 tokens devem ser absolutamente distintos
        assertThat(generatedTokens).hasSize(50);
    }

    @Test
    @DisplayName("TokenHashUtils: Calcula hash SHA-256 exato e determinístico")
    void shouldCalculateAccurateSha256() throws NoSuchAlgorithmException {
        String rawToken = "my-secret-test-token-123456";
        String computedHash = TokenHashUtils.hashToken(rawToken);

        assertThat(computedHash).hasSize(64);
        assertThat(computedHash).matches("^[a-f0-9]{64}$");

        // Validação cruzada com MessageDigest padrão
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] expectedBytes = md.digest(rawToken.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String expectedHex = java.util.HexFormat.of().formatHex(expectedBytes);

        assertThat(computedHash).isEqualTo(expectedHex);
    }

    @Test
    @DisplayName("TokenHashUtils: constantTimeVerify valida tokens legítimos e rejeita divergências")
    void shouldVerifyTokensInConstantTime() {
        String rawToken = TokenHashUtils.generateSecureToken();
        String persistedHash = TokenHashUtils.hashToken(rawToken);

        // Token correto
        assertThat(TokenHashUtils.constantTimeVerify(rawToken, persistedHash)).isTrue();

        // Token incorreto
        assertThat(TokenHashUtils.constantTimeVerify(rawToken + "x", persistedHash)).isFalse();
        assertThat(TokenHashUtils.constantTimeVerify("different-token", persistedHash)).isFalse();
        assertThat(TokenHashUtils.constantTimeVerify(null, persistedHash)).isFalse();
        assertThat(TokenHashUtils.constantTimeVerify(rawToken, null)).isFalse();
    }
}

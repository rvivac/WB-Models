package com.wbscouting.api.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * Utilitário criptográfico para geração de tokens de recuperação e persistência em SHA-256
 * em conformidade com o Item 14 da EAP-SEG-002.
 */
public final class TokenHashUtils {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private TokenHashUtils() {
    }

    /**
     * Gera um token seguro aleatório de 32 bytes (256 bits) codificado em hexadecimal.
     */
    public static String generateSecureToken() {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        return HexFormat.of().formatHex(randomBytes);
    }

    /**
     * Calcula o hash SHA-256 do token em formato hexadecimal minúsculo (64 caracteres).
     */
    public static String hashToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.trim().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo SHA-256 indisponível na JVM", e);
        }
    }

    /**
     * Compara o token em texto puro contra o hash persistido em tempo constante (MessageDigest.isEqual),
     * mitigando timing attacks.
     */
    public static boolean constantTimeVerify(String rawToken, String persistedHash) {
        if (rawToken == null || persistedHash == null || persistedHash.isBlank()) {
            return false;
        }
        String computedHash = hashToken(rawToken);
        return MessageDigest.isEqual(
                computedHash.getBytes(StandardCharsets.UTF_8),
                persistedHash.getBytes(StandardCharsets.UTF_8)
        );
    }
}

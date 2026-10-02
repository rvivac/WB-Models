package com.wbscouting.api.service.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TotpServiceTest {

    private TotpService totpService;

    @BeforeEach
    void setUp() {
        totpService = new TotpService();
    }

    @Test
    @DisplayName("Deve gerar segredo Base32 válido e montar URI otpauth")
    void shouldGenerateValidSecretAndOtpauthUrl() {
        String secret = totpService.generateSecret();
        assertNotNull(secret);
        assertEquals(32, secret.length());

        String otpauthUrl = totpService.getOtpauthUrl("admin@wbagency.com.br", secret);
        assertTrue(otpauthUrl.startsWith("otpauth://totp/"));
        assertTrue(otpauthUrl.contains("secret=" + secret));
        assertTrue(otpauthUrl.contains("admin@wbagency.com.br"));
    }

    @Test
    @DisplayName("Deve gerar QR Code em formato Base64 Data URL")
    void shouldGenerateQrCodeDataUrl() {
        String secret = totpService.generateSecret();
        String otpauthUrl = totpService.getOtpauthUrl("admin@wbagency.com.br", secret);
        String qrCodeDataUrl = totpService.generateQrCodeDataUrl(otpauthUrl);

        assertNotNull(qrCodeDataUrl);
        assertTrue(qrCodeDataUrl.startsWith("data:image/png;base64,"));
        assertTrue(qrCodeDataUrl.length() > 100);
    }

    @Test
    @DisplayName("Deve validar código de backup gerado e seu respectivo hash")
    void shouldGenerateAndVerifyBackupCodes() {
        List<String> codes = totpService.generateBackupCodes(8);
        assertEquals(8, codes.size());

        for (String code : codes) {
            assertTrue(code.matches("^[A-Z0-9]{4}-[A-Z0-9]{4}$"));
            String hash = totpService.hashBackupCode(code);
            assertNotNull(hash);
            assertTrue(totpService.verifyBackupCode(code, hash));
            assertFalse(totpService.verifyBackupCode("INVALIDO", hash));
        }
    }
}

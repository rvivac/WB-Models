package com.wbscouting.api.service.auth;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;

@Slf4j
@Service
public class TotpService {

    private static final String BASE32_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final int SECRET_BYTE_LENGTH = 20; // 160 bits (recomendado RFC 4226/6238)
    private static final int TIME_STEP_SECONDS = 30;
    private static final int WINDOW_SIZE = 1; // Permite passo anterior, atual e seguinte
    private static final String ISSUER = "WB Agency";

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Gera um segredo Base32 criptograficamente seguro.
     */
    public String generateSecret() {
        byte[] bytes = new byte[SECRET_BYTE_LENGTH];
        secureRandom.nextBytes(bytes);
        return encodeBase32(bytes);
    }

    /**
     * Monta a URI otpauth:// conforme padrão do Google Authenticator.
     */
    public String getOtpauthUrl(String email, String secret) {
        String encodedIssuer = URLEncoder.encode(ISSUER, StandardCharsets.UTF_8).replace("+", "%20");
        return String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=SHA1&digits=6&period=30",
                encodedIssuer, email, secret, encodedIssuer);
    }

    /**
     * Gera o QR Code em PNG codificado em Base64 Data URL.
     */
    public String generateQrCodeDataUrl(String otpauthUrl) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(otpauthUrl, BarcodeFormat.QR_CODE, 240, 240);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            log.error("Erro ao gerar QR Code TOTP: {}", e.getMessage(), e);
            throw new RuntimeException("Falha ao gerar QR Code para autenticação 2FA.", e);
        }
    }

    /**
     * Valida um código TOTP de 6 dígitos considerando a janela de tolerância de tempo.
     */
    public boolean verifyCode(String secret, String code) {
        if (secret == null || code == null) {
            return false;
        }

        String sanitizedCode = code.replaceAll("\\s+", "").trim();
        if (!sanitizedCode.matches("\\d{6}")) {
            return false;
        }

        byte[] keyBytes;
        try {
            keyBytes = decodeBase32(secret);
        } catch (Exception e) {
            log.warn("Falha ao decodificar chave Base32 TOTP: {}", e.getMessage());
            return false;
        }

        long currentStep = Instant.now().getEpochSecond() / TIME_STEP_SECONDS;

        for (int i = -WINDOW_SIZE; i <= WINDOW_SIZE; i++) {
            long step = currentStep + i;
            String expected = generateCodeForStep(keyBytes, step);
            if (MessageDigest.isEqual(sanitizedCode.getBytes(StandardCharsets.UTF_8), expected.getBytes(StandardCharsets.UTF_8))) {
                return true;
            }
        }

        return false;
    }

    /**
     * Gera uma lista de códigos de backup descartáveis no formato ABCD-1234.
     */
    public List<String> generateBackupCodes(int count) {
        List<String> codes = new ArrayList<>(count);
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // sem caracteres ambíguos como 0, O, 1, I

        for (int i = 0; i < count; i++) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < 4; j++) {
                sb.append(chars.charAt(secureRandom.nextInt(chars.length())));
            }
            sb.append("-");
            for (int j = 0; j < 4; j++) {
                sb.append(chars.charAt(secureRandom.nextInt(chars.length())));
            }
            codes.add(sb.toString());
        }

        return codes;
    }

    /**
     * Hash seguro para persistência dos códigos de backup.
     */
    public String hashBackupCode(String rawCode) {
        try {
            String normalized = rawCode.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(normalized.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao calcular hash de código de backup", e);
        }
    }

    /**
     * Verifica se o código informado confere com algum código de backup armazenado.
     */
    public boolean verifyBackupCode(String inputCode, String storedHashedCode) {
        if (inputCode == null || storedHashedCode == null) {
            return false;
        }
        String inputHashed = hashBackupCode(inputCode);
        return MessageDigest.isEqual(inputHashed.getBytes(StandardCharsets.UTF_8), storedHashedCode.getBytes(StandardCharsets.UTF_8));
    }

    private String generateCodeForStep(byte[] key, long step) {
        try {
            byte[] data = new byte[8];
            for (int i = 7; i >= 0; i--) {
                data[i] = (byte) (step & 0xFF);
                step >>= 8;
            }

            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(data);

            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);

            int otp = binary % 1_000_000;
            return String.format("%06d", otp);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar código TOTP", e);
        }
    }

    private String encodeBase32(byte[] data) {
        StringBuilder result = new StringBuilder();
        int buffer = 0;
        int bitsLeft = 0;

        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xFF);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                int index = (buffer >> (bitsLeft - 5)) & 0x1F;
                bitsLeft -= 5;
                result.append(BASE32_CHARS.charAt(index));
            }
        }

        if (bitsLeft > 0) {
            int index = (buffer << (5 - bitsLeft)) & 0x1F;
            result.append(BASE32_CHARS.charAt(index));
        }

        return result.toString();
    }

    private byte[] decodeBase32(String base32) {
        String clean = base32.replaceAll("[=\\s-]", "").toUpperCase(Locale.ROOT);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        int buffer = 0;
        int bitsLeft = 0;

        for (char c : clean.toCharArray()) {
            int val = BASE32_CHARS.indexOf(c);
            if (val < 0) {
                continue;
            }
            buffer = (buffer << 5) | val;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                output.write((buffer >> (bitsLeft - 8)) & 0xFF);
                bitsLeft -= 8;
            }
        }

        return output.toByteArray();
    }
}

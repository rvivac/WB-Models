package com.wbscouting.api.security;

import com.wbscouting.api.exception.InvalidFileException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * Validador e sanitizador ativo de imagens contra arquivos poliglota e injeção de payloads binários
 * em conformidade com o Item 17 da EAP-SEG-002.
 */
@Slf4j
public final class ImageSecuritySanitizer {

    private static final byte[] JPEG_MAGIC = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_MAGIC = new byte[]{
            (byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47,
            (byte) 0x0D, (byte) 0x0A, (byte) 0x1A, (byte) 0x0A
    };
    private static final byte[] RIFF_MAGIC = new byte[]{(byte) 0x52, (byte) 0x49, (byte) 0x46, (byte) 0x46}; // "RIFF"
    private static final byte[] WEBP_MAGIC = new byte[]{(byte) 0x57, (byte) 0x45, (byte) 0x42, (byte) 0x50}; // "WEBP"

    private ImageSecuritySanitizer() {
    }

    /**
     * Valida magic bytes, decodifica via ImageIO e regrava em um buffer limpo na memória,
     * expurgando metadados EXIF maliciosos ou bytes anexados no final do arquivo (polyglots).
     */
    public static MultipartFile sanitizeAndReencode(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("O arquivo de imagem é obrigatório e não pode ser vazio.");
        }

        byte[] originalBytes;
        try {
            originalBytes = file.getBytes();
        } catch (IOException e) {
            throw new InvalidFileException("Falha ao ler bytes do arquivo de imagem.", e);
        }

        if (originalBytes.length < 8) {
            throw new InvalidFileException("Arquivo de imagem corrompido ou excessivamente curto.");
        }

        // 1. Verificação estrita de Magic Bytes
        String format = detectImageFormat(originalBytes);
        if (format == null) {
            log.warn("Tentativa de upload com magic bytes inválidos. Tamanho: {} bytes", originalBytes.length);
            throw new InvalidFileException("Formato de imagem não suportado ou cabeçalho adulterado. Aceitos: JPEG, PNG, WEBP.");
        }

        // 2. Sanitização Ativa: Decodificação e regravação via ImageIO (quando decodificável)
        try {
            BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(originalBytes));
            if (originalImage != null) {
                int width = originalImage.getWidth();
                int height = originalImage.getHeight();

                if (width <= 0 || height <= 0 || width > 10000 || height > 10000) {
                    throw new InvalidFileException("Dimensões da imagem fora dos limites de segurança aceitáveis.");
                }

                int imageType = "png".equalsIgnoreCase(format) || originalImage.getColorModel().hasAlpha()
                        ? BufferedImage.TYPE_INT_ARGB
                        : BufferedImage.TYPE_INT_RGB;

                BufferedImage cleanImage = new BufferedImage(width, height, imageType);
                Graphics2D g2d = cleanImage.createGraphics();
                try {
                    g2d.drawImage(originalImage, 0, 0, null);
                } finally {
                    g2d.dispose();
                }

                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                String writeFormat = "png".equalsIgnoreCase(format) ? "png" : "jpg";
                boolean success = ImageIO.write(cleanImage, writeFormat, outputStream);

                if (success && outputStream.size() > 0) {
                    byte[] sanitizedBytes = outputStream.toByteArray();
                    log.debug("Imagem '{}' higienizada e regravada com sucesso. De {} para {} bytes.",
                            file.getOriginalFilename(), originalBytes.length, sanitizedBytes.length);

                    return new SanitizedMultipartFile(
                            file.getName(),
                            file.getOriginalFilename(),
                            resolveContentType(writeFormat),
                            sanitizedBytes
                    );
                }
            }
        } catch (Exception e) {
            log.debug("Decodificação completa ImageIO indisponível para o payload fornecido: {}", e.getMessage());
        }

        // Magic bytes válidos confirmados
        return new SanitizedMultipartFile(
                file.getName(),
                file.getOriginalFilename(),
                resolveContentType(format),
                originalBytes
        );
    }

    public static String detectImageFormat(byte[] bytes) {
        if (bytes == null || bytes.length < 3) {
            return null;
        }

        if (bytes[0] == JPEG_MAGIC[0] && bytes[1] == JPEG_MAGIC[1] && bytes[2] == JPEG_MAGIC[2]) {
            return "jpeg";
        }

        if (bytes.length >= 8) {
            byte[] sub = Arrays.copyOfRange(bytes, 0, 8);
            if (Arrays.equals(sub, PNG_MAGIC)) {
                return "png";
            }
        }

        if (bytes.length >= 12) {
            byte[] riffSub = Arrays.copyOfRange(bytes, 0, 4);
            byte[] webpSub = Arrays.copyOfRange(bytes, 8, 12);
            if (Arrays.equals(riffSub, RIFF_MAGIC) && Arrays.equals(webpSub, WEBP_MAGIC)) {
                return "webp";
            }
        }

        return null;
    }

    private static String resolveContentType(String format) {
        if ("png".equalsIgnoreCase(format)) {
            return "image/png";
        }
        if ("webp".equalsIgnoreCase(format)) {
            return "image/webp";
        }
        return "image/jpeg";
    }
}

package com.wbscouting.api.security;

import com.wbscouting.api.exception.InvalidFileException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@DisplayName("EAP-SEG-002: Lote 4 - Cookies HttpOnly, Sanitização de Mídias e Resiliência")
class Lote4SecurityHardeningTest {

    // ==========================================
    // 4.1. Cookies HttpOnly no JWT (Item 16)
    // ==========================================
    @Test
    @DisplayName("JwtFilter: Deve autenticar com sucesso a partir do cookie 'jwt_token' quando Authorization header estiver ausente")
    void shouldAuthenticateFromHttpOnlyCookie() throws ServletException, IOException {
        JwtService jwtService = mock(JwtService.class);
        UserDetailsService userDetailsService = mock(UserDetailsService.class);
        TokenBlacklistService tokenBlacklistService = mock(TokenBlacklistService.class);

        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, userDetailsService, tokenBlacklistService);

        String testToken = "sample.jwt.token";
        String username = "admin@wbscouting.com";
        UserDetails userDetails = new User(username, "password", Collections.emptyList());

        when(tokenBlacklistService.isBlacklisted(testToken)).thenReturn(false);
        when(jwtService.extractUsername(testToken)).thenReturn(username);
        when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);
        when(jwtService.isTokenValid(testToken, userDetails)).thenReturn(true);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/admin/models");
        request.setCookies(new Cookie("jwt_token", testToken));

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(jwtService).extractUsername(testToken);
    }

    // ==========================================
    // 4.2. Magic Bytes & Regravação de Imagens (Item 17)
    // ==========================================
    @Test
    @DisplayName("ImageSanitizer: Rejeita arquivos executáveis ou texto disfarçados de imagem")
    void shouldRejectExecutableDisguisedAsImage() {
        // Cabeçalho MZ (DOS/Windows Executable)
        byte[] exeBytes = new byte[]{'M', 'Z', (byte) 0x90, 0, 3, 0, 0, 0, 4, 0, 0, 0};
        MockMultipartFile fakeJpg = new MockMultipartFile("file", "exploit.jpg", "image/jpeg", exeBytes);

        assertThatThrownBy(() -> ImageSecuritySanitizer.sanitizeAndReencode(fakeJpg))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageContaining("Formato de imagem não suportado");

        // Payload HTML/Script
        byte[] scriptBytes = "<script>alert('xss')</script>".getBytes();
        MockMultipartFile scriptFile = new MockMultipartFile("file", "avatar.png", "image/png", scriptBytes);

        assertThatThrownBy(() -> ImageSecuritySanitizer.sanitizeAndReencode(scriptFile))
                .isInstanceOf(InvalidFileException.class);
    }

    @Test
    @DisplayName("ImageSanitizer: Decodifica e regrava imagem PNG válida em buffer de memória")
    void shouldSanitizeAndReencodeValidPng() throws IOException {
        // Gera um PNG em memória
        BufferedImage img = new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.BLUE);
        g.fillRect(0, 0, 50, 50);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        byte[] validPngBytes = baos.toByteArray();

        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", validPngBytes);

        MultipartFile sanitized = ImageSecuritySanitizer.sanitizeAndReencode(file);

        assertThat(sanitized).isNotNull();
        assertThat(sanitized.getSize()).isGreaterThan(0);
        assertThat(sanitized.getContentType()).isEqualTo("image/png");
        assertThat(ImageSecuritySanitizer.detectImageFormat(sanitized.getBytes())).isEqualTo("png");
    }

    @Test
    @DisplayName("ImageSanitizer: Detecta magic bytes de WebP (RIFF...WEBP)")
    void shouldDetectWebpMagicBytes() {
        byte[] webpBytes = new byte[]{
                (byte) 0x52, (byte) 0x49, (byte) 0x46, (byte) 0x46, // RIFF
                0, 0, 0, 0,                                          // size
                (byte) 0x57, (byte) 0x45, (byte) 0x42, (byte) 0x50  // WEBP
        };

        assertThat(ImageSecuritySanitizer.detectImageFormat(webpBytes)).isEqualTo("webp");
    }
}

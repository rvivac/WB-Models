package com.wbscouting.api.security;

import com.wbscouting.api.entity.Admin;
import com.wbscouting.api.enums.AdminRole;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@DisplayName("2.2. Teste de Rejeição de Token Forjado com o Segredo Antigo")
class JwtForgedTokenRejectionTest {

    // Segredo Oficial Ativo no Backend (HMAC-SHA256 256 bits)
    private static final String ACTIVE_SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    // Segredo Antigo / Revogado / Forjado por Atacante
    private static final String OLD_OR_FORGED_SECRET = "99887766554433221100AABBCCDDEEFF99887766554433221100AABBCCDDEEFF";

    private static final long EXPIRATION_MS = 28800000L; // 8 horas

    private JwtService activeJwtService;
    private JwtService forgedJwtService;
    private JwtTokenProvider activeJwtTokenProvider;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private FilterChain filterChain;

    private Admin admin;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        SecurityContextHolder.clearContext();

        // Serviço ativo usando a chave atual
        activeJwtService = new JwtService(ACTIVE_SECRET, EXPIRATION_MS);
        activeJwtTokenProvider = new JwtTokenProvider(ACTIVE_SECRET, EXPIRATION_MS);

        // Serviço legado/atacante gerando tokens com chave antiga ou não autorizada
        forgedJwtService = new JwtService(OLD_OR_FORGED_SECRET, EXPIRATION_MS);

        admin = Admin.builder()
                .id(UUID.randomUUID())
                .name("Super Admin")
                .email("admin@wbscouting.com")
                .passwordHash("hashed-secret")
                .role(AdminRole.SUPER_ADMIN)
                .isActive(true)
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Deve rejeitar token forjado com segredo antigo na validação de JwtService.isTokenValid")
    void shouldRejectForgedTokenInJwtServiceValidation() {
        // Token gerado com a chave antiga
        String forgedToken = forgedJwtService.generateToken(admin);
        assertThat(forgedToken).isNotBlank();

        // O serviço com a chave ativa DEVE rejeitar
        boolean isValid = activeJwtService.isTokenValid(forgedToken, admin);
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Deve lançar SignatureException ao tentar extrair claims de token assinado com chave antiga")
    void shouldThrowSignatureExceptionWhenExtractingClaimsFromForgedToken() {
        String forgedToken = forgedJwtService.generateToken(admin);

        assertThatThrownBy(() -> activeJwtService.extractAllClaims(forgedToken))
                .isInstanceOf(SignatureException.class)
                .hasMessageContaining("JWT signature does not match");
    }

    @Test
    @DisplayName("Deve retornar false em JwtTokenProvider.validateToken para token forjado com chave antiga")
    void shouldReturnFalseInJwtTokenProviderForForgedToken() {
        String forgedToken = forgedJwtService.generateToken(admin);

        boolean isValid = activeJwtTokenProvider.validateToken(forgedToken);
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Filtro JwtAuthenticationFilter deve rejeitar token forjado e NÃO autenticar a requisição")
    void shouldRejectForgedTokenInJwtAuthenticationFilterAndKeepContextUnauthenticated() throws ServletException, IOException {
        String forgedToken = forgedJwtService.generateToken(admin);

        when(tokenBlacklistService.isBlacklisted(forgedToken)).thenReturn(false);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + forgedToken);
        MockHttpServletResponse response = new MockHttpServletResponse();

        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
                activeJwtService,
                userDetailsService,
                tokenBlacklistService
        );

        filter.doFilter(request, response, filterChain);

        // Deve delegar para a cadeia de filtros
        verify(filterChain, times(1)).doFilter(request, response);

        // NENHUMA consulta ao UserDetailsService deve ser feita para autenticar token com assinatura inválida
        verify(userDetailsService, never()).loadUserByUsername(anyString());

        // O SecurityContextHolder DEVE permanecer completamente não autenticado
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Deve rejeitar token legítimo cujo payload foi adulterado (tampered)")
    void shouldRejectLegitimateTokenWhenPayloadIsTampered() {
        String legitimateToken = activeJwtService.generateToken(admin);

        // Separação dos blocos do JWT: header.payload.signature
        String[] parts = legitimateToken.split("\\.");
        assertThat(parts).hasSize(3);

        // Adulteração manual do payload
        String tamperedPayload = "eyJzdWIiOiJoYWNrZXJAd2JzY291dGluZy5jb20iLCJuYW1lIjoiSGFja2VyIn0";
        String tamperedToken = parts[0] + "." + tamperedPayload + "." + parts[2];

        boolean isValid = activeJwtService.isTokenValid(tamperedToken, admin);
        assertThat(isValid).isFalse();

        assertThatThrownBy(() -> activeJwtService.extractAllClaims(tamperedToken))
                .isInstanceOf(SignatureException.class);
    }
}

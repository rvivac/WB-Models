package com.wbscouting.api.security;

import com.wbscouting.api.entity.Admin;
import com.wbscouting.api.enums.AdminRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;
    private Admin admin;

    private static final String TEST_SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final long EXPIRATION_MS = 28800000L; // 8 horas

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(TEST_SECRET, EXPIRATION_MS);

        admin = Admin.builder()
                .id(UUID.randomUUID())
                .name("Super Admin")
                .email("admin@wbscouting.com")
                .passwordHash("hashed-secret")
                .role(AdminRole.SUPER_ADMIN)
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("Deve gerar token JWT com as claims esperadas: sub, id, name, role")
    void shouldGenerateTokenWithExpectedClaims() {
        String token = jwtService.generateToken(admin);

        assertThat(token).isNotBlank();

        Claims claims = jwtService.extractAllClaims(token);
        assertThat(claims.getSubject()).isEqualTo("admin@wbscouting.com");
        assertThat(claims.get("id", String.class)).isEqualTo(admin.getId().toString());
        assertThat(claims.get("name", String.class)).isEqualTo("Super Admin");
        assertThat(claims.get("role", String.class)).isEqualTo("SUPER_ADMIN");
    }

    @Test
    @DisplayName("Deve extrair username corretamente do token")
    void shouldExtractUsernameCorrectly() {
        String token = jwtService.generateToken(admin);
        String username = jwtService.extractUsername(token);

        assertThat(username).isEqualTo("admin@wbscouting.com");
    }

    @Test
    @DisplayName("Deve validar token com sucesso para o usuário correto")
    void shouldValidateTokenSuccessfully() {
        String token = jwtService.generateToken(admin);
        boolean isValid = jwtService.isTokenValid(token, admin);

        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Deve invalidar token se pertencer a outro usuário")
    void shouldInvalidateTokenForDifferentUser() {
        String token = jwtService.generateToken(admin);

        Admin otherAdmin = Admin.builder()
                .id(UUID.randomUUID())
                .name("Outro Admin")
                .email("outro@wbscouting.com")
                .passwordHash("hashed")
                .role(AdminRole.SUPER_ADMIN)
                .isActive(true)
                .build();

        boolean isValid = jwtService.isTokenValid(token, otherAdmin);
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Deve lançar SignatureException quando o token for assinado com chave diferente")
    void shouldThrowSignatureExceptionForTamperedSecret() {
        JwtService anotherJwtService = new JwtService("A1B2C3D4E5F60718293A4B5C6D7E8F90A1B2C3D4E5F60718293A4B5C6D7E8F90", EXPIRATION_MS);
        String token = anotherJwtService.generateToken(admin);

        assertThatThrownBy(() -> jwtService.extractAllClaims(token))
                .isInstanceOf(SignatureException.class);
    }

    @Test
    @DisplayName("Deve lançar MalformedJwtException quando a estrutura do token for inválida")
    void shouldThrowMalformedJwtExceptionForInvalidStructure() {
        assertThatThrownBy(() -> jwtService.extractAllClaims("invalid.token.string"))
                .isInstanceOf(MalformedJwtException.class);
    }

    @Test
    @DisplayName("Deve retornar tempo de expiração em segundos correspondente a 8 horas")
    void shouldReturnCorrectExpirationInSeconds() {
        assertThat(jwtService.getExpirationInSeconds()).isEqualTo(28800L);
    }
}

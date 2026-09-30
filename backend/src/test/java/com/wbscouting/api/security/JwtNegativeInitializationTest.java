package com.wbscouting.api.security;

import io.jsonwebtoken.security.WeakKeyException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("2.1. Teste Negativo de Inicialização sem JWT_SECRET")
class JwtNegativeInitializationTest {

    private static final long EXPIRATION_MS = 28800000L;

    @Test
    @DisplayName("Deve falhar ao instanciar JwtTokenProvider com segredo nulo")
    void shouldFailWhenInstantiatingJwtTokenProviderWithNullSecret() {
        assertThatThrownBy(() -> new JwtTokenProvider(null, EXPIRATION_MS))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Deve falhar com WeakKeyException ao instanciar JwtTokenProvider com segredo vazio")
    void shouldFailWhenInstantiatingJwtTokenProviderWithEmptySecret() {
        assertThatThrownBy(() -> new JwtTokenProvider("", EXPIRATION_MS))
                .isInstanceOf(WeakKeyException.class);
    }

    @Test
    @DisplayName("Deve falhar com WeakKeyException ao instanciar JwtTokenProvider com segredo fraco (< 256 bits)")
    void shouldFailWhenInstantiatingJwtTokenProviderWithWeakSecret() {
        // "c2VuaGE=" decodifica para "senha" (5 bytes / 40 bits < 256 bits exigidos)
        assertThatThrownBy(() -> new JwtTokenProvider("c2VuaGE=", EXPIRATION_MS))
                .isInstanceOf(WeakKeyException.class);
    }

    @Test
    @DisplayName("Deve falhar ao instanciar JwtService com segredo nulo")
    void shouldFailWhenInstantiatingJwtServiceWithNullSecret() {
        assertThatThrownBy(() -> new JwtService(null, EXPIRATION_MS))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Deve falhar com WeakKeyException ao instanciar JwtService com segredo vazio")
    void shouldFailWhenInstantiatingJwtServiceWithEmptySecret() {
        assertThatThrownBy(() -> new JwtService("", EXPIRATION_MS))
                .isInstanceOf(WeakKeyException.class);
    }

    @Test
    @DisplayName("Deve falhar com WeakKeyException ao instanciar JwtService com segredo fraco (< 256 bits)")
    void shouldFailWhenInstantiatingJwtServiceWithWeakSecret() {
        assertThatThrownBy(() -> new JwtService("chave-fraca-insegura", EXPIRATION_MS))
                .isInstanceOf(WeakKeyException.class);
    }

    @Test
    @DisplayName("Contexto Spring deve falhar na inicialização (Fail-Fast) quando app.security.jwt.secret for vazio")
    void shouldFailContextInitializationWhenJwtSecretIsEmpty() {
        new ApplicationContextRunner()
                .withPropertyValues(
                        "app.security.jwt.secret=",
                        "app.security.jwt.expiration-ms=28800000"
                )
                .withUserConfiguration(JwtTokenProvider.class)
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(WeakKeyException.class);
                });
    }
}

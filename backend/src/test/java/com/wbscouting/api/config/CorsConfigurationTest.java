package com.wbscouting.api.config;

import com.wbscouting.api.security.JwtAuthenticationEntryPoint;
import com.wbscouting.api.security.JwtAuthenticationFilter;
import com.wbscouting.api.security.ratelimit.RateLimitingFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DisplayName("Dual-Environment CORS Configuration Tests")
class CorsConfigurationTest {

    @Test
    @DisplayName("CorsConfigurationSource deve aceitar localhost:4200, 127.0.0.1:4200 e domínios de produção")
    void shouldAcceptDualEnvironmentOrigins() {
        JwtAuthenticationFilter jwtFilter = mock(JwtAuthenticationFilter.class);
        JwtAuthenticationEntryPoint entryPoint = mock(JwtAuthenticationEntryPoint.class);
        RateLimitingFilter rateLimitingFilter = mock(RateLimitingFilter.class);

        SecurityConfig securityConfig = new SecurityConfig(jwtFilter, entryPoint, rateLimitingFilter);
        ReflectionTestUtils.setField(securityConfig, "allowedOrigins",
                List.of("http://localhost:4200", "http://127.0.0.1:4200", "https://wbagency.com.br", "https://www.wbagency.com.br"));
        ReflectionTestUtils.setField(securityConfig, "allowedMethods",
                List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        ReflectionTestUtils.setField(securityConfig, "allowedHeaders", List.of("*"));
        ReflectionTestUtils.setField(securityConfig, "allowCredentials", true);
        ReflectionTestUtils.setField(securityConfig, "maxAge", 3600L);

        CorsConfigurationSource source = securityConfig.corsConfigurationSource();

        List<String> testOrigins = List.of(
                "http://localhost:4200",
                "http://127.0.0.1:4200",
                "https://wbagency.com.br",
                "https://www.wbagency.com.br"
        );

        for (String origin : testOrigins) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/api/v1/models");
            request.addHeader("Origin", origin);

            CorsConfiguration config = source.getCorsConfiguration(request);
            assertThat(config).isNotNull();
            assertThat(config.checkOrigin(origin)).isEqualTo(origin);
            assertThat(config.getAllowedMethods()).contains("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS");
            assertThat(config.getAllowCredentials()).isTrue();
            assertThat(config.getAllowedHeaders()).contains("*");
        }
    }
}

package com.wbscouting.api.security;

import com.wbscouting.api.dto.candidate.CandidateApplyRequestDto;
import com.wbscouting.api.exception.InvalidSortPropertyException;
import com.wbscouting.api.security.ratelimit.RateLimitingFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@DisplayName("EAP-SEG-002: Lote 2 - Entrada, Erros, CORS e Rate Limiting")
class Lote2SecurityHardeningTest {

    private Validator validator;
    private RateLimitingFilter rateLimitingFilter;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        rateLimitingFilter = new RateLimitingFilter();
    }

    // ==========================================
    // 2.1. Allowlist Estrita de Sort e Paginação (Item 8)
    // ==========================================
    @Test
    @DisplayName("SortAllowlist: Deve aceitar propriedades legítimas e constantes")
    void shouldAcceptAllowedSortProperties() {
        assertThat(SortAllowlistValidator.isPropertyAllowed("id")).isTrue();
        assertThat(SortAllowlistValidator.isPropertyAllowed("createdAt")).isTrue();
        assertThat(SortAllowlistValidator.isPropertyAllowed("fullName")).isTrue();
        assertThat(SortAllowlistValidator.isPropertyAllowed("stageName")).isTrue();
        assertThat(SortAllowlistValidator.isPropertyAllowed("displayOrder")).isTrue();

        Sort validSort = Sort.by(Sort.Direction.ASC, "stageName").and(Sort.by(Sort.Direction.DESC, "createdAt"));
        SortAllowlistValidator.validateSort(validSort);
    }

    @Test
    @DisplayName("SortAllowlist: Deve rejeitar e lançar InvalidSortPropertyException para campos não permitidos")
    void shouldRejectDisallowedSortProperties() {
        assertThat(SortAllowlistValidator.isPropertyAllowed("password")).isFalse();
        assertThat(SortAllowlistValidator.isPropertyAllowed("secretKey")).isFalse();
        assertThat(SortAllowlistValidator.isPropertyAllowed("1;DROP TABLE users;")).isFalse();

        Sort maliciousSort = Sort.by(Sort.Direction.ASC, "password");
        assertThatThrownBy(() -> SortAllowlistValidator.validateSort(maliciousSort))
                .isInstanceOf(InvalidSortPropertyException.class)
                .hasMessageContaining("password");
    }

    @Test
    @DisplayName("SortAllowlist: sanitizeOrDefault deve recuar para createdAt DESC em caso de violação")
    void shouldFallbackToDefaultSortOnSanitization() {
        Sort maliciousSort = Sort.by(Sort.Direction.ASC, "injected_col");
        Sort safeSort = SortAllowlistValidator.sanitizeOrDefault(maliciousSort);

        assertThat(safeSort.getOrderFor("createdAt")).isNotNull();
        assertThat(safeSort.getOrderFor("createdAt").isDescending()).isTrue();
    }

    // ==========================================
    // 2.2. Sanitização de E-mail e Validação de fullName (Item 9)
    // ==========================================
    @Test
    @DisplayName("FullName Validation: Deve aceitar nomes válidos com acentos e caracteres legítimos")
    void shouldAcceptValidFullNames() {
        CandidateApplyRequestDto dto = CandidateApplyRequestDto.builder()
                .fullName("João da Silva Santos")
                .build();

        Set<ConstraintViolation<CandidateApplyRequestDto>> violations = validator.validateProperty(dto, "fullName");
        assertThat(violations).isEmpty();

        dto.setFullName("Maria d'Ávila-Souza");
        violations = validator.validateProperty(dto, "fullName");
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("FullName Validation: Deve rejeitar nomes com CRLF, tags HTML ou símbolos proibidos")
    void shouldRejectMaliciousFullNames() {
        // Injeção de CRLF / SMTP
        CandidateApplyRequestDto crlfDto = CandidateApplyRequestDto.builder()
                .fullName("João\r\nBcc: victim@domain.com")
                .build();
        Set<ConstraintViolation<CandidateApplyRequestDto>> violations = validator.validateProperty(crlfDto, "fullName");
        assertThat(violations).isNotEmpty();

        // XSS / Tags
        CandidateApplyRequestDto xssDto = CandidateApplyRequestDto.builder()
                .fullName("João <script>alert(1)</script>")
                .build();
        violations = validator.validateProperty(xssDto, "fullName");
        assertThat(violations).isNotEmpty();

        // Tamanho menor que 3 caracteres
        CandidateApplyRequestDto shortDto = CandidateApplyRequestDto.builder()
                .fullName("Jo")
                .build();
        violations = validator.validateProperty(shortDto, "fullName");
        assertThat(violations).isNotEmpty();
    }

    // ==========================================
    // 2.4. Rate Limiting com Bucket4j (Item 11)
    // ==========================================
    @Test
    @DisplayName("RateLimiting: /api/v1/auth/login deve permitir 5 tentativas e bloquear a 6ª com HTTP 429")
    void shouldRateLimitLoginEndpoint() throws ServletException, IOException {
        String testIp = "192.168.1.100";
        FilterChain filterChain = mock(FilterChain.class);

        // Primeiras 5 requisições devem passar
        for (int i = 1; i <= 5; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
            request.setRemoteAddr(testIp);
            MockHttpServletResponse response = new MockHttpServletResponse();

            rateLimitingFilter.doFilter(request, response, filterChain);
            assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        }
        verify(filterChain, times(5)).doFilter(any(), any());

        // 6ª requisição deve ser bloqueada com 429 Too Many Requests
        MockHttpServletRequest blockedRequest = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        blockedRequest.setRemoteAddr(testIp);
        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();

        rateLimitingFilter.doFilter(blockedRequest, blockedResponse, filterChain);

        assertThat(blockedResponse.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        assertThat(blockedResponse.getHeader("Retry-After")).isNotNull();
        assertThat(blockedResponse.getContentAsString()).contains("Limite de tentativas de login excedido");
        // Não deve ter invocado o chain na 6ª chamada
        verify(filterChain, times(5)).doFilter(any(), any());
    }

    @Test
    @DisplayName("RateLimiting: /api/v1/auth/forgot-password deve permitir 3 requisições e bloquear a 4ª com HTTP 429")
    void shouldRateLimitForgotPasswordEndpoint() throws ServletException, IOException {
        String testIp = "10.0.0.50";
        FilterChain filterChain = mock(FilterChain.class);

        // Primeiras 3 requisições devem passar
        for (int i = 1; i <= 3; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/forgot-password");
            request.setRemoteAddr(testIp);
            MockHttpServletResponse response = new MockHttpServletResponse();

            rateLimitingFilter.doFilter(request, response, filterChain);
            assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        }
        verify(filterChain, times(3)).doFilter(any(), any());

        // 4ª requisição deve ser bloqueada com 429
        MockHttpServletRequest blockedRequest = new MockHttpServletRequest("POST", "/api/v1/auth/forgot-password");
        blockedRequest.setRemoteAddr(testIp);
        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();

        rateLimitingFilter.doFilter(blockedRequest, blockedResponse, filterChain);

        assertThat(blockedResponse.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        assertThat(blockedResponse.getHeader("Retry-After")).isNotNull();
        assertThat(blockedResponse.getContentAsString()).contains("Limite de recuperação de senha excedido");
        verify(filterChain, times(3)).doFilter(any(), any());
    }

    @Test
    @DisplayName("RateLimiting: /api/v1/submissions deve permitir 5 submissões por hora e bloquear a 6ª com HTTP 429")
    void shouldRateLimitSubmissionsEndpoint() throws ServletException, IOException {
        String testIp = "200.100.50.25";
        FilterChain filterChain = mock(FilterChain.class);

        // Primeiras 5 requisições devem passar
        for (int i = 1; i <= 5; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/submissions");
            request.setRemoteAddr(testIp);
            MockHttpServletResponse response = new MockHttpServletResponse();

            rateLimitingFilter.doFilter(request, response, filterChain);
            assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        }
        verify(filterChain, times(5)).doFilter(any(), any());

        // 6ª requisição deve ser bloqueada com 429
        MockHttpServletRequest blockedRequest = new MockHttpServletRequest("POST", "/api/v1/submissions");
        blockedRequest.setRemoteAddr(testIp);
        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();

        rateLimitingFilter.doFilter(blockedRequest, blockedResponse, filterChain);

        assertThat(blockedResponse.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        assertThat(blockedResponse.getHeader("Retry-After")).isNotNull();
        assertThat(blockedResponse.getContentAsString()).contains("Limite de candidaturas excedido");
        verify(filterChain, times(5)).doFilter(any(), any());
    }
}

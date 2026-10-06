package com.wbscouting.api.security.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Filtro de Rate Limiting baseado em IP com Bucket4j (Item 11 da EAP-SEG-002).
 * Protege endpoints sensíveis contra ataques de força bruta, credential stuffing e abuso:
 * - POST /auth/login: 5 tentativas por minuto por IP.
 * - POST /auth/forgot-password: 3 requisições a cada 15 minutos por IP.
 * - POST /submissions (e rotas públicas de candidatura): 5 submissões por hora por IP.
 */
@Slf4j
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Mapas em memória indexados por IP
    private final Map<String, Bucket> loginBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> forgotPasswordBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> submissionBuckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        // 🔴 Early-exit OBRIGATORIO RFC9110 CORS: OPTIONS preflight do navegador
        // NUNCA deve ser sujeito a rate limit (senão navegadores modernos retornam 403)
        if ("OPTIONS".equalsIgnoreCase(method)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 🔴 Early-exit: health checks do Render / Cloudflare — nao queremos 429 aqui
        if (path.startsWith("/actuator/") || path.startsWith("/actuator") || "/error".equals(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        if ("POST".equalsIgnoreCase(method)) {
            String clientIp = extractClientIp(request);

            if (isLoginEndpoint(path)) {
                Bucket bucket = loginBuckets.computeIfAbsent(clientIp, k -> createLoginBucket());
                if (!tryConsume(bucket, response, "Limite de tentativas de login excedido. Máximo de 5 requisições por minuto.")) {
                    log.warn("Rate limit atingido para login pelo IP: {}", clientIp);
                    return;
                }
            } else if (isForgotPasswordEndpoint(path)) {
                Bucket bucket = forgotPasswordBuckets.computeIfAbsent(clientIp, k -> createForgotPasswordBucket());
                if (!tryConsume(bucket, response, "Limite de recuperação de senha excedido. Máximo de 3 requisições a cada 15 minutos.")) {
                    log.warn("Rate limit atingido para forgot-password pelo IP: {}", clientIp);
                    return;
                }
            } else if (isSubmissionEndpoint(path)) {
                Bucket bucket = submissionBuckets.computeIfAbsent(clientIp, k -> createSubmissionBucket());
                if (!tryConsume(bucket, response, "Limite de candidaturas excedido. Máximo de 5 submissões por hora.")) {
                    log.warn("Rate limit atingido para submissão pelo IP: {}", clientIp);
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isLoginEndpoint(String path) {
        return path.endsWith("/auth/login") || path.endsWith("/auth/login/");
    }

    private boolean isForgotPasswordEndpoint(String path) {
        return path.endsWith("/auth/forgot-password") || path.endsWith("/auth/forgot-password/");
    }

    private boolean isSubmissionEndpoint(String path) {
        return path.endsWith("/submissions") || path.endsWith("/submissions/")
                || path.endsWith("/candidates") || path.endsWith("/candidates/")
                || path.endsWith("/public/candidates") || path.endsWith("/public/candidates/");
    }

    private Bucket createLoginBucket() {
        // 5 tentativas por minuto
        Bandwidth limit = Bandwidth.builder()
                .capacity(5)
                .refillIntervally(5, Duration.ofMinutes(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private Bucket createForgotPasswordBucket() {
        // 3 tentativas a cada 15 minutos
        Bandwidth limit = Bandwidth.builder()
                .capacity(3)
                .refillIntervally(3, Duration.ofMinutes(15))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private Bucket createSubmissionBucket() {
        // 5 submissões por hora
        Bandwidth limit = Bandwidth.builder()
                .capacity(5)
                .refillIntervally(5, Duration.ofHours(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private boolean tryConsume(Bucket bucket, HttpServletResponse response, String message) throws IOException {
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            return true;
        }

        long retryAfterSeconds = Math.max(1, TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()));

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value()); // HTTP 429
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        response.setContentType("application/problem+json;charset=UTF-8");

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.TOO_MANY_REQUESTS,
                message + String.format(" Tente novamente em %d segundos.", retryAfterSeconds)
        );
        problemDetail.setTitle("Too Many Requests");
        problemDetail.setType(URI.create("https://wbscouting.com/errors/too-many-requests"));
        problemDetail.setProperty("timestamp", Instant.now().toString());
        problemDetail.setProperty("retryAfterSeconds", retryAfterSeconds);

        response.getWriter().write(objectMapper.writeValueAsString(problemDetail));
        return false;
    }

    private String extractClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isBlank() || "unknown".equalsIgnoreCase(xfHeader)) {
            xfHeader = request.getHeader("CF-Connecting-IP");
        }
        if (xfHeader == null || xfHeader.isBlank() || "unknown".equalsIgnoreCase(xfHeader)) {
            xfHeader = request.getHeader("X-Real-IP");
        }
        if (xfHeader != null && !xfHeader.isBlank() && !"unknown".equalsIgnoreCase(xfHeader)) {
            // Em múltiplos proxies, o IP original do cliente é o primeiro elemento
            return xfHeader.split(",")[0].trim();
        }
        String remoteAddr = request.getRemoteAddr();
        if ("0:0:0:0:0:0:0:1".equals(remoteAddr)) {
            return "127.0.0.1";
        }
        return remoteAddr != null ? remoteAddr : "unknown";
    }
}

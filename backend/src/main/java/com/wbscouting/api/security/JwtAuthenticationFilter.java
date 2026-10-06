package com.wbscouting.api.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final TokenBlacklistService tokenBlacklistService;

    /**
     * Whitelist de caminhos PUBLICOS que NUNCA precisam de validacao JWT.
     * Motivo: preflight OPTIONS CORS e endpoints publicos devem passar imediatamente
     * sem que o filtro tente validar token ausente. O SecurityFilterChain garante o
     * autorizaHTTPRequests final; aqui so pulamos processamento desnecessario.
     */
    private static final java.util.List<String> PUBLIC_PATH_PREFIXES = java.util.List.of(
            "/options",
            "/auth/", "/api/v1/auth/",
            "/public/", "/api/v1/public/",
            "/apply", "/api/v1/apply",
            "/submissions/", "/api/v1/submissions/",
            "/candidates/", "/api/v1/candidates/",
            "/models/", "/api/v1/models/",
            "/site-contents/", "/api/v1/site-contents/",
            "/contact-channels/", "/api/v1/public/contact-channels/", "/public/contact-channels/",
            "/i18n/", "/api/v1/public/i18n/", "/public/i18n/",
            "/storage/local/", "/api/v1/storage/local/",
            "/actuator/", "/actuator/health",
            "/error"
    );

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        // 🔴 ANTI-BLOQUEIO CORS: Preflight OPTIONS do navegador NUNCA envia Authorization.
        // Sem este early-exit, o Spring Security/filter chain pode responder 403 ANTES
        // do CorsFilter (DefaultCorsFilter) injetar os headers Allow-*.
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            log.trace("[CORS] Early-exit OPTIONS preflight para URI: {}", request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        // Early-exit 2: Rota ja reconhecida como publica — nao perde tempo com JWT
        final String uri = request.getRequestURI();
        for (String prefix : PUBLIC_PATH_PREFIXES) {
            if (uri.startsWith(prefix)) {
                filterChain.doFilter(request, response);
                return;
            }
        }

        final String jwt = getJwtFromRequest(request);

        if (StringUtils.hasText(jwt)) {
            if (tokenBlacklistService.isBlacklisted(jwt)) {
                log.warn("Requisição bloqueada: Token JWT consta na blacklist de revogação.");
                filterChain.doFilter(request, response);
                return;
            }

            try {
                final String username = jwtService.extractUsername(jwt);

                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                    if (jwtService.isTokenValid(jwt, userDetails)) {
                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        log.debug("Administrador autenticado com sucesso via JWT: {}", username);
                    }
                }
            } catch (JwtException | IllegalArgumentException ex) {
                log.warn("Falha na validação do token JWT da requisição: {}", ex.getMessage());
                // Não propaga exceção aqui para permitir que requisições a rotas públicas continuem.
                // Em rotas protegidas, o JwtAuthenticationEntryPoint será acionado pelo Spring Security.
            }
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7).trim();
        }

        // Suporte a autenticação via Cookie HttpOnly (Item 16 da EAP-SEG-002)
        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if ("jwt_token".equals(cookie.getName()) && StringUtils.hasText(cookie.getValue())) {
                    return cookie.getValue().trim();
                }
            }
        }

        return null;
    }
}


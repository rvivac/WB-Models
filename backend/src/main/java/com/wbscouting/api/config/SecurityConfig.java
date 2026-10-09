package com.wbscouting.api.config;

import com.wbscouting.api.security.JwtAuthenticationEntryPoint;
import com.wbscouting.api.security.JwtAuthenticationFilter;
import com.wbscouting.api.security.ratelimit.RateLimitingFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final RateLimitingFilter rateLimitingFilter;

    /**
     * Origens permitidas (fallback literal se variavel de ambiente for sobrescrita).
     * OBS: Nao usamos mais estas em setAllowedOrigins() — elas sao MERGEADAS com
     * os padroes wildcard abaixo em setAllowedOriginPatterns() para evitar 403
     * em preflights com barra final, porta implicita ou subdominio dinamico.
     */
    @Value("${app.cors.allowed-origins:http://localhost:4200,http://localhost:8080,https://wbagency.com.br,https://www.wbagency.com.br,http://wbagency.com.br,http://www.wbagency.com.br}")
    private List<String> allowedOrigins;

    @Value("${app.cors.allowed-methods:GET,POST,PUT,PATCH,DELETE,OPTIONS}")
    private List<String> allowedMethods;

    @Value("${app.cors.allowed-headers:Authorization,Content-Type,X-Requested-With,Accept,Origin}")
    private List<String> allowedHeaders;

    @Value("${app.cors.allow-credentials:true}")
    private boolean allowCredentials;

    @Value("${app.cors.max-age:3600}")
    private Long maxAge;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers
                        .cacheControl(org.springframework.security.config.Customizer.withDefaults())
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                )
                .authorizeHttpRequests(auth -> auth
                        // Libera todo o preflight CORS do navegador (OBRIGATORIO antes de qualquer auth)
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Health check do Render (evita 502 na porta do load balancer quando JWT nao e enviado)
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/error").permitAll()

                        // Endpoints públicos de autenticação
                        .requestMatchers("/auth/**", "/api/v1/auth/**").permitAll()

                        // Endpoints públicos de candidatura ("Quero ser modelo")
                        // Obs: rotas /apply e /submissions sao ambas LIBERADAS (TASK item 3)
                        .requestMatchers(HttpMethod.POST, "/apply/**", "/api/v1/apply/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/submissions/**", "/api/v1/submissions/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/candidates/**", "/api/v1/candidates/**", "/public/candidates/**", "/api/v1/public/candidates/**", "/public/candidates/apply", "/api/v1/public/candidates/apply").permitAll()

                        // Endpoints públicos de leitura (Catálogo, Home, Destaques, Conteúdos, Contato, I18n, Storage Local)
                        .requestMatchers(HttpMethod.GET, "/models/**", "/api/v1/models/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/featured-models/**", "/api/v1/featured-models/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/home-settings/**", "/api/v1/home-settings/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/public/**", "/api/v1/public/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/site-contents/**", "/api/v1/site-contents/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/contact-channels/**", "/api/v1/contact-channels/**", "/api/v1/public/contact-channels/**", "/public/contact-channels/**", "/public/institutional/contact/**", "/api/v1/public/institutional/contact/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/i18n/**", "/api/v1/public/i18n/**", "/public/i18n/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/storage/local/**", "/api/v1/storage/local/**").permitAll()

                        // Endpoints de gestão de usuários restritos exclusivamente a WEBMASTER e SUPER_ADMIN (ADM-018)
                        .requestMatchers("/admin/users/**", "/api/v1/admin/users/**").hasAnyRole("WEBMASTER", "SUPER_ADMIN")

                        // Endpoints de trilha de auditoria restritos exclusivamente a WEBMASTER (ADM-019)
                        .requestMatchers("/admin/audit-logs/**", "/api/v1/admin/audit-logs/**").hasAnyRole("WEBMASTER", "SUPER_ADMIN")

                        // Endpoints protegidos (Gestão e Backoffice)
                        .requestMatchers("/admin/**", "/api/v1/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN", "CONTENT_ADMIN", "WEBMASTER", "SCOUT")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 🔑 Anti-403 CORS Render + Hostinger: usamos ALLOWED ORIGIN PATTERNS
        // com wildcards. Isso permite origens com barra final (ex: https://wbagency.com.br/),
        // portas dinamicas no localhost, e subdominios wildcard do Render.
        // Mergeamos os patterns padrão abaixo com a lista ${app.cors.allowed-origins}
        // (mantemos compatibilidade retroativa se houver override em ENV/Runtime).
        java.util.ArrayList<String> originPatterns = new java.util.ArrayList<>(List.of(
                // Frontend em produção (Hostinger)
                "https://wbagency.com.br",
                "https://wbagency.com.br/",
                "https://*.wbagency.com.br",
                "https://*.wbagency.com.br/",
                // Ambiente local (portas padrão Angular e Spring)
                "http://localhost:4200",
                "http://localhost:4200/",
                "http://localhost:8080",
                "http://localhost:8080/",
                "http://localhost:[*]",
                "http://127.0.0.1:[*]",
                // Backend em si (Render - evita 502 quando backend chama a si proprio em webhooks)
                "https://wb-models-*.onrender.com",
                "https://*.onrender.com"
        ));
        if (allowedOrigins != null && !allowedOrigins.isEmpty()) {
            originPatterns.addAll(allowedOrigins);
        }
        configuration.setAllowedOriginPatterns(originPatterns);

        configuration.setAllowedMethods(allowedMethods != null && !allowedMethods.isEmpty()
                ? allowedMethods
                : List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // Headers de entrada: * (o Spring resolve corretamente mesmo com allowCredentials=true
        // quando usado em conjunto com AllowedOriginPatterns, não AllowedOrigins literal).
        configuration.setAllowedHeaders(allowedHeaders != null && !allowedHeaders.isEmpty()
                ? allowedHeaders
                : List.of("*"));

        // Headers que o navegador LIBERA para o JS ler (Content-Disposition = download de arquivos)
        configuration.setExposedHeaders(List.of("Authorization", "Content-Disposition", "Retry-After", "X-Total-Count"));

        configuration.setAllowCredentials(allowCredentials);
        configuration.setMaxAge(maxAge != null ? maxAge : 3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}

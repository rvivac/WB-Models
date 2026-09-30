package com.wbscouting.api.config;

import com.wbscouting.api.exception.InvalidSortPropertyException;
import com.wbscouting.api.security.SortAllowlistValidator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuração Web MVC para validação de ordenação dinâmica e limites de paginação (Item 8 - EAP-SEG-002).
 */
@Slf4j
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Bean
    public PageableHandlerMethodArgumentResolverCustomizer pageableCustomizer() {
        return resolver -> {
            resolver.setMaxPageSize(50);
            resolver.setOneIndexedParameters(false);
        };
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SortValidationInterceptor());
    }

    public static class SortValidationInterceptor implements HandlerInterceptor {
        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            String[] sortParams = request.getParameterValues("sort");
            if (sortParams != null) {
                for (String param : sortParams) {
                    if (param == null || param.isBlank()) continue;
                    // O formato do Spring Data é 'campo,direcao' ou apenas 'campo'
                    String[] parts = param.split(",");
                    String property = parts[0].trim();
                    if (!property.isEmpty() && !SortAllowlistValidator.isPropertyAllowed(property)) {
                        log.warn("Tentativa de ordenação por propriedade não permitida: '{}' a partir do IP {}",
                                property, request.getRemoteAddr());
                        throw new InvalidSortPropertyException(property);
                    }
                }
            }

            String sortBy = request.getParameter("sortBy");
            if (sortBy != null && !sortBy.isBlank()) {
                String property = sortBy.trim();
                if (!SortAllowlistValidator.isPropertyAllowed(property)) {
                    log.warn("Tentativa de ordenação por propriedade 'sortBy' não permitida: '{}' a partir do IP {}",
                            property, request.getRemoteAddr());
                    throw new InvalidSortPropertyException(property);
                }
            }

            return true;
        }
    }
}

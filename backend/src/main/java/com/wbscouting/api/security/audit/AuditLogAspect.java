package com.wbscouting.api.security.audit;

import com.wbscouting.api.entity.Admin;
import com.wbscouting.api.service.audit.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private final AuditLogService auditLogService;

    @AfterReturning(pointcut = "@annotation(auditAction)", returning = "result")
    public void interceptAuditAction(JoinPoint joinPoint, AuditAction auditAction, Object result) {
        try {
            // 1. Extração do Administrador autenticado
            UUID adminId = null;
            String adminEmail = null;

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
                Object principal = authentication.getPrincipal();
                if (principal instanceof Admin admin) {
                    adminId = admin.getId();
                    adminEmail = admin.getEmail();
                } else if (principal instanceof UserDetails userDetails) {
                    adminEmail = userDetails.getUsername();
                } else if (authentication.getName() != null) {
                    adminEmail = authentication.getName();
                }
            }

            // 2. Extração de IP e User-Agent via HttpServletRequest
            String clientIp = "127.0.0.1";
            String userAgent = "Unknown";

            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                clientIp = extractClientIp(request);
                String uaHeader = request.getHeader("User-Agent");
                if (uaHeader != null && !uaHeader.isBlank()) {
                    userAgent = uaHeader;
                }
            }

            // 3. Extração de Resource ID e Details JSON a partir dos argumentos ou resultado
            String resourceId = resolveResourceId(joinPoint, result);
            String description = auditAction.description();
            if (description == null || description.isBlank()) {
                description = String.format("Executou %s em %s", auditAction.action(), auditAction.resource());
                if (resourceId != null && !resourceId.isBlank()) {
                    description += " (" + resourceId + ")";
                }
            }

            Map<String, Object> details = buildDetailsMap(joinPoint, result);

            // 4. Gravação assíncrona não-bloqueante
            auditLogService.recordLogAsync(
                    adminId,
                    adminEmail,
                    auditAction.action(),
                    auditAction.resource(),
                    resourceId,
                    description,
                    details,
                    clientIp,
                    userAgent
            );
        } catch (Exception ex) {
            log.warn("Falha ao interceptar auditoria via AOP: {}", ex.getMessage());
        }
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            String[] ips = xForwardedFor.split(",");
            if (ips.length > 0) {
                return ips[0].trim();
            }
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp.trim();
        }
        return request.getRemoteAddr();
    }

    private String resolveResourceId(JoinPoint joinPoint, Object result) {
        // Tentar encontrar argumento UUID ou String nomeado id/candidateId/modelId
        Object[] args = joinPoint.getArgs();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();

        if (paramNames != null && args != null) {
            for (int i = 0; i < paramNames.length && i < args.length; i++) {
                String name = paramNames[i].toLowerCase();
                if ((name.contains("id") || name.equals("key") || name.equals("code")) && args[i] != null) {
                    return String.valueOf(args[i]);
                }
            }
        }

        // Tentar extrair do resultado se for ResponseEntity com getBody()
        if (result instanceof ResponseEntity<?> responseEntity && responseEntity.getBody() != null) {
            Object body = responseEntity.getBody();
            try {
                Method getIdMethod = body.getClass().getMethod("getId");
                Object idVal = getIdMethod.invoke(body);
                if (idVal != null) {
                    return String.valueOf(idVal);
                }
            } catch (Exception ignored) {
            }
        }

        return null;
    }

    private Map<String, Object> buildDetailsMap(JoinPoint joinPoint, Object result) {
        Map<String, Object> details = new HashMap<>();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        details.put("method", signature.getDeclaringType().getSimpleName() + "." + signature.getName());

        Object[] args = joinPoint.getArgs();
        String[] paramNames = signature.getParameterNames();
        if (paramNames != null && args != null) {
            Map<String, Object> params = new HashMap<>();
            for (int i = 0; i < paramNames.length && i < args.length; i++) {
                String pName = paramNames[i];
                Object pVal = args[i];
                if (pVal != null && !isSensitiveOrFrameworkType(pVal)) {
                    params.put(pName, String.valueOf(pVal));
                }
            }
            if (!params.isEmpty()) {
                details.put("parameters", params);
            }
        }

        return details;
    }

    private boolean isSensitiveOrFrameworkType(Object val) {
        String typeName = val.getClass().getName();
        return typeName.contains("HttpServlet") ||
                typeName.contains("Principal") ||
                typeName.contains("Authentication") ||
                typeName.toLowerCase().contains("password");
    }
}

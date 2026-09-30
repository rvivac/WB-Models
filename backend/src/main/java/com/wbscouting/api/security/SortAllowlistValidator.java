package com.wbscouting.api.security;

import com.wbscouting.api.exception.InvalidSortPropertyException;
import org.springframework.data.domain.Sort;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Validador e higienizador defensivo para propriedades de ordenação dinâmica (Sort)
 * em conformidade com o Item 8 da EAP-SEG-002.
 */
public final class SortAllowlistValidator {

    public static final Set<String> ALLOWED_SORT_PROPERTIES;

    static {
        Set<String> allowed = new HashSet<>();
        // Propriedades comuns de auditoria e identificação
        allowed.add("id");
        allowed.add("createdAt");
        allowed.add("updatedAt");

        // Propriedades de modelos
        allowed.add("name");
        allowed.add("stageName");
        allowed.add("gender");
        allowed.add("isStar");
        allowed.add("isActive");
        allowed.add("displayOrder");

        // Propriedades de candidaturas e candidatos
        allowed.add("fullName");
        allowed.add("status");
        allowed.add("city");
        allowed.add("height");
        allowed.add("heightCm");
        allowed.add("age");
        allowed.add("email");
        allowed.add("phone");
        allowed.add("experienceYears");

        ALLOWED_SORT_PROPERTIES = Collections.unmodifiableSet(allowed);
    }

    private SortAllowlistValidator() {
    }

    /**
     * Valida estritamente se todas as ordens especificadas pertencem à lista branca.
     * Caso contrário, lança {@link InvalidSortPropertyException}.
     */
    public static void validateSort(Sort sort) {
        if (sort == null || sort.isUnsorted()) {
            return;
        }

        for (Sort.Order order : sort) {
            String property = order.getProperty();
            if (!isPropertyAllowed(property)) {
                throw new InvalidSortPropertyException(property);
            }
        }
    }

    /**
     * Valida uma única propriedade contra a allowlist.
     */
    public static boolean isPropertyAllowed(String property) {
        if (property == null || property.isBlank()) {
            return false;
        }
        return ALLOWED_SORT_PROPERTIES.contains(property.trim());
    }

    /**
     * Higieniza o Sort fornecido. Se for nulo ou contiver propriedades fora da allowlist,
     * retorna a ordenação padrão segura: createdAt DESC.
     */
    public static Sort sanitizeOrDefault(Sort sort) {
        if (sort == null || sort.isUnsorted()) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }

        try {
            validateSort(sort);
            return sort;
        } catch (InvalidSortPropertyException e) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
    }
}

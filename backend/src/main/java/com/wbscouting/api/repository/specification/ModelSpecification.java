package com.wbscouting.api.repository.specification;

import com.wbscouting.api.entity.Model;
import com.wbscouting.api.enums.GenderType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class ModelSpecification {

    /**
     * Specification PADRAO de filtros para Model (genero, star, ativo, busca stageName).
     * <p>
     * <strong>REGRAS DE SEGURANÇA — NUNCA QUEBRAR (causa HHH90003004 + totalElements=0):</strong>
     * <ul>
     *   <li>NUNCA adicionar {@code root.fetch("media", ...)} ou fetch em collections.</li>
     *   <li>NUNCA adicionar {@code root.join("media", JoinType.LEFT).fetch(...)}</li>
     *   <li>Esta classe eh usada em consultas PAGINADAS (Pageable). Qualquer FETCH em
     *       colecoes causa warning HHH90003004 e o Spring Data Page retorna
     *       totalElements=0 INJUSTIFICADAMENTE (bug Hibernate + Spring Data).</li>
     *   <li>Para carregar a colecao media (1:N) em listas: use DTO projections / query
     *       a parte / buscar por ID individual (onde EntityGraph eh permitido).</li>
     * </ul>
     */
    public static Specification<Model> filter(GenderType gender, Boolean isStar, Boolean isActive, String search) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (gender != null) {
                predicates.add(cb.equal(root.get("gender"), gender));
            }

            if (isStar != null) {
                predicates.add(cb.equal(root.get("isStar"), isStar));
            }

            if (isActive != null) {
                predicates.add(cb.equal(root.get("isActive"), isActive));
            }

            if (StringUtils.hasText(search)) {
                String searchPattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("stageName")), searchPattern));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

package com.wbscouting.api.specification;

import com.wbscouting.api.entity.Model;
import com.wbscouting.api.enums.GenderType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class PublicModelSpecification {

    public static Specification<Model> filter(GenderType gender, Boolean isStar, String search) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Predicado fixo: apenas modelos ativos
            predicates.add(root.get("isActive").in(true));

            // Predicado opcional: gender
            if (gender != null) {
                predicates.add(cb.equal(root.get("gender"), gender));
            }

            // Predicado opcional: isStar
            if (isStar != null) {
                predicates.add(cb.equal(root.get("isStar"), isStar));
            }

            // Predicado opcional: search em stage_name (case-insensitive)
            if (StringUtils.hasText(search)) {
                String searchPattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("stageName")), searchPattern));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

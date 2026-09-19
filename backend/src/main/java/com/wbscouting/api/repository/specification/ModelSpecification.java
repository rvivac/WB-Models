package com.wbscouting.api.repository.specification;

import com.wbscouting.api.entity.Model;
import com.wbscouting.api.enums.GenderType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class ModelSpecification {

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

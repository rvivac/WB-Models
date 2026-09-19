package com.wbscouting.api.specification;

import com.wbscouting.api.entity.Candidate;
import com.wbscouting.api.enums.CandidateStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CandidateSpecification {

    public static Specification<Candidate> filter(
            CandidateStatus status,
            String gender,
            String search,
            Boolean isMinor) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Comparação de status
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            // 2. Comparação de gênero (case-insensitive)
            if (StringUtils.hasText(gender)) {
                predicates.add(cb.equal(cb.upper(root.get("gender")), gender.trim().toUpperCase()));
            }

            // 3. Cláusula OR com LIKE %term% em minúsculas para fullName e email
            if (StringUtils.hasText(search)) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                Predicate nameMatch = cb.like(cb.lower(root.get("fullName")), pattern);
                Predicate emailMatch = cb.like(cb.lower(root.get("email")), pattern);
                predicates.add(cb.or(nameMatch, emailMatch));
            }

            // 4. Cálculo de menoridade: birthDate > LocalDate.now().minusYears(18)
            if (isMinor != null) {
                LocalDate cutoffDate = LocalDate.now().minusYears(18);
                if (Boolean.TRUE.equals(isMinor)) {
                    // Menor de 18 anos
                    Predicate birthDateMinor = cb.greaterThan(root.get("birthDate"), cutoffDate);
                    Predicate ageMinor = cb.lessThan(root.get("age"), 18);
                    predicates.add(cb.or(birthDateMinor, ageMinor));
                } else {
                    // Maior ou igual a 18 anos
                    Predicate birthDateAdult = cb.lessThanOrEqualTo(root.get("birthDate"), cutoffDate);
                    Predicate ageAdult = cb.greaterThanOrEqualTo(root.get("age"), 18);
                    predicates.add(cb.or(birthDateAdult, ageAdult));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

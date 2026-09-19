package com.wbscouting.api.specification;

import com.wbscouting.api.entity.Candidate;
import com.wbscouting.api.enums.CandidateStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AdminCandidateSpecification {

    public static Specification<Candidate> filter(CandidateStatus status, String search, String gender, Boolean isMinor) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Filtro opcional por status da candidatura
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            // 2. Filtro opcional por busca textual (full_name ou email)
            if (StringUtils.hasText(search)) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                Predicate nameMatch = cb.like(cb.lower(root.get("fullName")), pattern);
                Predicate emailMatch = cb.like(cb.lower(root.get("email")), pattern);
                predicates.add(cb.or(nameMatch, emailMatch));
            }

            // 3. Filtro opcional por gênero (case-insensitive)
            if (StringUtils.hasText(gender)) {
                predicates.add(cb.equal(cb.upper(root.get("gender")), gender.trim().toUpperCase()));
            }

            // 4. Filtro opcional por menoridade (isMinor)
            if (isMinor != null) {
                LocalDate cutoffDate = LocalDate.now().minusYears(18);
                if (Boolean.TRUE.equals(isMinor)) {
                    // Menor de idade: age < 18 ou (age is null e birthDate > cutoffDate)
                    Predicate ageMinor = cb.lessThan(root.get("age"), 18);
                    Predicate birthDateMinor = cb.greaterThan(root.get("birthDate"), cutoffDate);
                    predicates.add(cb.or(ageMinor, cb.and(cb.isNull(root.get("age")), birthDateMinor)));
                } else {
                    // Maior de idade: age >= 18 ou (age is null e birthDate <= cutoffDate)
                    Predicate ageAdult = cb.greaterThanOrEqualTo(root.get("age"), 18);
                    Predicate birthDateAdult = cb.lessThanOrEqualTo(root.get("birthDate"), cutoffDate);
                    predicates.add(cb.or(ageAdult, cb.and(cb.isNull(root.get("age")), birthDateAdult)));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

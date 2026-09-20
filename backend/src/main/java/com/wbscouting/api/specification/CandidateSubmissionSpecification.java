package com.wbscouting.api.specification;

import com.wbscouting.api.entity.CandidateSubmission;
import com.wbscouting.api.enums.SubmissionGender;
import com.wbscouting.api.enums.SubmissionStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

public class CandidateSubmissionSpecification {

    public static Specification<CandidateSubmission> filter(
            String search,
            SubmissionStatus status,
            SubmissionGender gender,
            BigDecimal minHeight,
            BigDecimal maxHeight,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Busca textual em fullName, email ou city
            if (StringUtils.hasText(search)) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                Predicate nameMatch = cb.like(cb.lower(root.get("fullName")), pattern);
                Predicate emailMatch = cb.like(cb.lower(root.get("email")), pattern);
                Predicate cityMatch = cb.like(cb.lower(root.get("city")), pattern);
                predicates.add(cb.or(nameMatch, emailMatch, cityMatch));
            }

            // 2. Filtro por status
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            // 3. Filtro por gênero
            if (gender != null) {
                predicates.add(cb.equal(root.get("gender"), gender));
            }

            // 4. Filtro por altura mínima e máxima
            if (minHeight != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("height"), minHeight));
            }
            if (maxHeight != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("height"), maxHeight));
            }

            // 5. Filtro por intervalo de datas de criação (createdAt)
            if (startDate != null) {
                OffsetDateTime startDateTime = startDate.atStartOfDay().atOffset(ZoneOffset.UTC);
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDateTime));
            }
            if (endDate != null) {
                OffsetDateTime endDateTime = endDate.atTime(LocalTime.MAX).atOffset(ZoneOffset.UTC);
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endDateTime));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

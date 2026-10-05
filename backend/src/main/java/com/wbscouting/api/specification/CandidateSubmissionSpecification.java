package com.wbscouting.api.specification;

import com.wbscouting.api.entity.CandidateSubmission;
import com.wbscouting.api.enums.SubmissionGender;
import com.wbscouting.api.enums.SubmissionStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CandidateSubmissionSpecification {

    private CandidateSubmissionSpecification() {}

    public static Specification<CandidateSubmission> filter(
            String search,
            SubmissionStatus status,
            SubmissionGender gender,
            Boolean isMinor,
            BigDecimal minHeight,
            BigDecimal maxHeight,
            LocalDate startDate,
            LocalDate endDate,
            Boolean includePromoted
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (search != null && !search.isBlank()) {
                String like = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("fullName")), like),
                    cb.like(cb.lower(cb.coalesce(root.get("city"), cb.literal(""))), like),
                    cb.like(cb.lower(cb.coalesce(root.get("email"), cb.literal(""))), like),
                    cb.like(cb.lower(cb.coalesce(root.get("phone"), cb.literal(""))), like),
                    cb.like(cb.lower(cb.coalesce(root.get("instagramHandle"), cb.literal(""))), like)
                ));
            }
            if (status != null) predicates.add(cb.equal(root.get("status"), status));
            if (gender != null) predicates.add(cb.equal(root.get("gender"), gender));
            if (isMinor != null) predicates.add(cb.equal(root.get("isMinor"), isMinor));
            if (minHeight != null) predicates.add(cb.greaterThanOrEqualTo(root.get("heightCm"), minHeight));
            if (maxHeight != null) predicates.add(cb.lessThanOrEqualTo(root.get("heightCm"), maxHeight));
            if (startDate != null) predicates.add(cb.greaterThanOrEqualTo(cb.function("DATE", LocalDate.class, root.get("createdAt")), startDate));
            if (endDate != null) predicates.add(cb.lessThanOrEqualTo(cb.function("DATE", LocalDate.class, root.get("createdAt")), endDate));
            if (includePromoted == null || !includePromoted) predicates.add(cb.isNull(root.get("convertedToModelId")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<CandidateSubmission> filter(
            String search,
            SubmissionStatus status,
            SubmissionGender gender,
            BigDecimal minHeight,
            BigDecimal maxHeight,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return filter(search, status, gender, null, minHeight, maxHeight, startDate, endDate, false);
    }
}
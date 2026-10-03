package com.wbscouting.api.specification;

import com.wbscouting.api.entity.AdminAuditLog;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class AdminAuditLogSpecification {

    public static Specification<AdminAuditLog> filter(
            String adminEmail,
            String resourceType,
            String action,
            OffsetDateTime startDate,
            OffsetDateTime endDate,
            String search) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(adminEmail)) {
                predicates.add(cb.like(cb.lower(root.get("adminEmail")), "%" + adminEmail.trim().toLowerCase() + "%"));
            }

            if (StringUtils.hasText(resourceType)) {
                predicates.add(cb.equal(cb.upper(root.get("resourceType")), resourceType.trim().toUpperCase()));
            }

            if (StringUtils.hasText(action)) {
                predicates.add(cb.equal(cb.upper(root.get("action")), action.trim().toUpperCase()));
            }

            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate));
            }

            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endDate));
            }

            if (StringUtils.hasText(search)) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                Predicate descMatch = cb.like(cb.lower(root.get("description")), pattern);
                Predicate emailMatch = cb.like(cb.lower(root.get("adminEmail")), pattern);
                Predicate resIdMatch = cb.like(cb.lower(root.get("resourceId")), pattern);
                Predicate ipMatch = cb.like(cb.lower(root.get("ipAddress")), pattern);
                predicates.add(cb.or(descMatch, emailMatch, resIdMatch, ipMatch));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

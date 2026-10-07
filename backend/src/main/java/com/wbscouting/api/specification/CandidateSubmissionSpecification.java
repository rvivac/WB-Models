// ✅ PASSO 1 TASK CRITICA: PACOTE CORRIGIDO (combinando caminho fisico + declaracao package)
// ANTES: package com.wbscouting.api.service.submission (ERRADO - causava class file does not contain expected class
//        e abortava analysis de tipos do javac, consequentemente TODOS annotation processors do Lombok paravam!)
// AGORA: package com.wbscouting.api.specification (CORRETO - arquivo fisico esta em specification/)
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
        // 🆕 Assinatura ORIGINAL (8 params): usada por CandidateAdminController
        return filter(search, status, gender, null, minHeight, maxHeight, startDate, endDate, false);
    }

    public static Specification<CandidateSubmission> filter(
            String search,
            SubmissionStatus status,
            SubmissionGender gender,
            Boolean isMinor,
            BigDecimal minHeight,
            BigDecimal maxHeight,
            LocalDate startDate,
            LocalDate endDate
    ) {
        // Assinatura com isMinor (9 params): usada por AdminApplicationController legado (chamadas antigas)
        return filter(search, status, gender, isMinor, minHeight, maxHeight, startDate, endDate, false);
    }

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

            // ============================================================
            // REGRA ADM-018: Fila de Triagem ATIVA por padrão
            // POR PADRÃO (includePromoted=false = DEFAULT) NÃO MOSTRA fichas PROMOTED.
            // Status exibidos por padrão: PENDING, REVIEWING, APPROVED, DECLINED, REJECTED(legado), CONTACTED, ARCHIVED.
            // Apenas ?includePromoted=true retorna TODOS incluindo PROMOTED (histórico completo).
            // ============================================================
            if (includePromoted == null || !Boolean.TRUE.equals(includePromoted)) {
                predicates.add(cb.notEqual(root.get("status"), SubmissionStatus.PROMOTED));
            }

            // 1. Busca textual em fullName, email, city ou protocol
            if (StringUtils.hasText(search)) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                Predicate nameMatch = cb.like(cb.lower(root.get("fullName")), pattern);
                Predicate emailMatch = cb.like(cb.lower(root.get("email")), pattern);
                Predicate cityMatch = cb.like(cb.lower(root.get("city")), pattern);
                Predicate protocolMatch = cb.like(cb.lower(root.get("protocol")), pattern);
                predicates.add(cb.or(nameMatch, emailMatch, cityMatch, protocolMatch));
            }

            // 2. Filtro por status (padrão exclui ARCHIVED)
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            } else {
                predicates.add(cb.notEqual(root.get("status"), SubmissionStatus.ARCHIVED));
            }

            // 3. Filtro por gênero
            if (gender != null) {
                predicates.add(cb.equal(root.get("gender"), gender));
            }

            // 4. Filtro por menoridade
            if (isMinor != null) {
                if (isMinor) {
                    predicates.add(cb.lessThan(root.get("age"), 18));
                } else {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("age"), 18));
                }
            }

            // 5. Filtro por altura mínima e máxima
            if (minHeight != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("height"), minHeight));
            }
            if (maxHeight != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("height"), maxHeight));
            }

            // 6. Filtro por intervalo de datas de criação (createdAt)
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

package com.print.shop.spec;

import com.print.shop.entity.PrintJob;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/** 工单的动态筛选条件：条件给了才拼进去，没给就当没这回事。 */
public final class PrintJobSpecs {

    private PrintJobSpecs() {
    }

    public static Specification<PrintJob> filter(String client, String state, Long paperId,
                                                 LocalDate dueFrom, LocalDate dueTo) {
        return (root, query, cb) -> {
            List<Predicate> parts = new ArrayList<>();
            if (client != null && !client.isBlank()) {
                parts.add(cb.like(root.get("clientName"), "%" + client.trim() + "%"));
            }
            if (state != null && !state.isBlank()) {
                parts.add(cb.equal(root.get("jobState"), state));
            }
            if (paperId != null) {
                parts.add(cb.equal(root.get("paperId"), paperId));
            }
            if (dueFrom != null) {
                parts.add(cb.greaterThanOrEqualTo(root.get("dueDate"), dueFrom));
            }
            if (dueTo != null) {
                parts.add(cb.lessThanOrEqualTo(root.get("dueDate"), dueTo));
            }
            return parts.isEmpty() ? cb.conjunction() : cb.and(parts.toArray(new Predicate[0]));
        };
    }
}

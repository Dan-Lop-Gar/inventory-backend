package com.inventory.specification;

import com.inventory.entity.PurchaseOrder;
import com.inventory.enums.OrderStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class OrderSpecification {

    private OrderSpecification() {}

    public static Specification<PurchaseOrder> withFilters(
            OrderStatus status,
            UUID supplierId,
            String createdBy,
            LocalDateTime dateFrom,
            LocalDateTime dateTo
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (supplierId != null) {
                predicates.add(cb.equal(
                        root.get("supplier").get("id"), supplierId
                ));
            }

            if (createdBy != null && !createdBy.isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("createdBy")),
                        "%" + createdBy.toLowerCase() + "%"
                ));
            }

            if (dateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("createdAt"), dateFrom
                ));
            }

            if (dateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(
                        root.get("createdAt"), dateTo
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
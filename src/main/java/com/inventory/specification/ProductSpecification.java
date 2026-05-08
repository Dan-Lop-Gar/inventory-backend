package com.inventory.specification;

import com.inventory.entity.Product;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ProductSpecification {

    private ProductSpecification() {}

    public static Specification<Product> withFilters(
            String name,
            String sku,
            UUID categoryId,
            UUID supplierId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean active,
            Boolean belowMinimumStock
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (name != null && !name.isBlank()) {
                predicates.add(cb.like(
                    cb.lower(root.get("name")),
                    "%" + name.toLowerCase() + "%"
                ));
            }

            if (sku != null && !sku.isBlank()) {
                predicates.add(cb.like(
                    cb.lower(root.get("sku")),
                    "%" + sku.toLowerCase() + "%"
                ));
            }

            if (categoryId != null) {
                predicates.add(cb.equal(
                    root.get("category").get("id"), categoryId
                ));
            }

            if (supplierId != null) {
                predicates.add(cb.equal(
                    root.get("supplier").get("id"), supplierId
                ));
            }

            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                    root.get("price"), minPrice
                ));
            }

            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(
                    root.get("price"), maxPrice
                ));
            }

            if (active != null) {
                predicates.add(cb.equal(root.get("active"), active));
            }

            if (Boolean.TRUE.equals(belowMinimumStock)) {
                predicates.add(cb.lessThanOrEqualTo(
                    root.get("stockCurrent"),
                    root.get("stockMinimum")
                ));
            }

            // Evita queries N+1 — fetch join en la misma query
            if (query.getResultType() != Long.class) {
                root.fetch("category", JoinType.LEFT);
                root.fetch("supplier", JoinType.LEFT);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
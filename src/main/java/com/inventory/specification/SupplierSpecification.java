package com.inventory.specification;

import com.inventory.entity.Supplier;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class SupplierSpecification {

    private SupplierSpecification() {}

    public static Specification<Supplier> withFilters(
            String name,
            String country,
            Boolean active
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (name != null && !name.isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("name")),
                        "%" + name.toLowerCase() + "%"
                ));
            }

            if (country != null && !country.isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("country")),
                        "%" + country.toLowerCase() + "%"
                ));
            }

            if (active != null) {
                predicates.add(cb.equal(root.get("active"), active));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

package com.inventory.repository;

import com.inventory.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SupplierRepository
        extends JpaRepository<Supplier, UUID>,
        JpaSpecificationExecutor<Supplier> {

    Optional<Supplier> findByEmailAndActiveTrue(String email);

    boolean existsByEmailAndActiveTrue(String email);

    List<Supplier> findByActiveTrueOrderByNameAsc();

    @Query("""
        SELECT s FROM Supplier s
        WHERE s.active = true
        AND LOWER(s.name) LIKE LOWER(CONCAT('%', :name, '%'))
        ORDER BY s.name ASC
        """)
    List<Supplier> findActiveByNameContaining(String name);
}

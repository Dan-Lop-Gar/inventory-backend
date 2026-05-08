package com.inventory.repository;

import com.inventory.entity.PurchaseOrder;
import com.inventory.enums.OrderStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository
        extends JpaRepository<PurchaseOrder, UUID>,
        JpaSpecificationExecutor<PurchaseOrder> {

    @Query("""
        SELECT o FROM PurchaseOrder o
        JOIN FETCH o.supplier
        LEFT JOIN FETCH o.lines l
        LEFT JOIN FETCH l.product
        WHERE o.id = :id
        """)
    Optional<PurchaseOrder> findByIdWithDetails(@Param("id") UUID id);

    List<PurchaseOrder> findByStatusAndRetryCountLessThan(
            OrderStatus status, int maxRetries
    );

    @Query("""
        SELECT COUNT(o) FROM PurchaseOrder o
        WHERE o.status = :status
        AND o.createdAt >= :since
        """)
    Long countByStatusSince(
            @Param("status") OrderStatus status,
            @Param("since") LocalDateTime since
    );
}
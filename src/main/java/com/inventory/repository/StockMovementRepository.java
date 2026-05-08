package com.inventory.repository;

import com.inventory.entity.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface StockMovementRepository
        extends JpaRepository<StockMovement, UUID>,
        JpaSpecificationExecutor<StockMovement> {

    Page<StockMovement> findByProductId(UUID productId, Pageable pageable);

    @Query("""
        SELECT sm FROM StockMovement sm
        JOIN FETCH sm.product p
        WHERE sm.createdAt BETWEEN :from AND :to
        ORDER BY sm.createdAt DESC
        """)
    List<StockMovement> findMovementsBetween(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query(value = """
        SELECT
            DATE_TRUNC('day', created_at) as day,
            movement_type,
            SUM(quantity) as total_quantity,
            COUNT(*) as total_movements
        FROM stock_movements
        WHERE created_at >= :since
        GROUP BY DATE_TRUNC('day', created_at), movement_type
        ORDER BY day DESC
        """, nativeQuery = true)
    List<Object[]> findDailyMovementStats(@Param("since") LocalDateTime since);
}
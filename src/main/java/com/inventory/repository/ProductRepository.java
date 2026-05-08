package com.inventory.repository;

import com.inventory.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository
        extends JpaRepository<Product, UUID>,
                JpaSpecificationExecutor<Product> {

    Optional<Product> findBySkuAndActiveTrue(String sku);

    @Query("""
        SELECT p FROM Product p
        JOIN FETCH p.category
        JOIN FETCH p.supplier
        WHERE p.active = true
        AND p.stockCurrent <= p.stockMinimum
        """)
    List<Product> findProductsBelowMinimumStock();

    @Query("""
        SELECT p FROM Product p
        JOIN FETCH p.category c
        JOIN FETCH p.supplier s
        WHERE p.id = :id
        AND p.active = true
        """)
    Optional<Product> findActiveById(@Param("id") UUID id);

    @Query(value = """
        SELECT
            c.name as category,
            COUNT(p.id) as total_products,
            SUM(p.stock_current) as total_stock,
            AVG(p.price) as avg_price
        FROM products p
        JOIN categories c ON p.category_id = c.id
        WHERE p.active = true
        GROUP BY c.name
        ORDER BY total_stock DESC
        """, nativeQuery = true)
    List<Object[]> findStockStatsByCategory();
}

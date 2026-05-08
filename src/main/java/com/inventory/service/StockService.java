package com.inventory.service;

import com.inventory.entity.Product;
import com.inventory.entity.StockMovement;
import com.inventory.enums.MovementType;
import com.inventory.exception.InsufficientStockException;
import com.inventory.exception.ProductNotFoundException;
import com.inventory.kafka.producer.StockAlertProducer;
import com.inventory.repository.ProductRepository;
import com.inventory.repository.StockMovementRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
public class StockService {

    private final ProductRepository productRepository;
    private final StockMovementRepository stockMovementRepository;
    private final StockAlertProducer stockAlertProducer;

    public StockService(
            ProductRepository productRepository,
            StockMovementRepository stockMovementRepository,
            StockAlertProducer stockAlertProducer) {
        this.productRepository = productRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.stockAlertProducer = stockAlertProducer;
    }

    @Transactional
    public int registerMovement(
            UUID productId,
            MovementType type,
            int quantity,
            UUID referenceId,
            String referenceType,
            String notes,
            String createdBy) {

        Product product = productRepository.findActiveById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        // valida que hay stock suficiente para salidas
        if (!type.isInbound() && product.getStockCurrent() < quantity) {
            throw new InsufficientStockException(
                    product.getName(),
                    product.getStockCurrent(),
                    quantity
            );
        }

        int stockBefore = product.getStockCurrent();
        product.adjustStock(type.isInbound() ? quantity : -quantity);
        productRepository.save(product);

        // Registra el movimiento
        StockMovement movement = StockMovement.builder()
                .product(product)
                .movementType(type)
                .quantity(quantity)
                .stockBefore(stockBefore)
                .stockAfter(product.getStockCurrent())
                .referenceId(referenceId)
                .referenceType(referenceType)
                .notes(notes)
                .createdBy(createdBy)
                .build();

        stockMovementRepository.save(movement);

        log.info("Movimiento registrado: {} {} unidades de {} - Stock: {} → {}",
                type, quantity, product.getSku(), stockBefore, product.getStockCurrent()
        );

        // Si después del movimiento el stock quedó bajo, envía alerta
        if (product.isBelowMinimumStock()) {
            stockAlertProducer.sendAlert(product);
            log.warn("Stock bajo después de movimiento: {} - {}/{}",
                    product.getSku(), product.getStockCurrent(), product.getStockMinimum()
            );
        }

        return product.getStockCurrent();
    }
}

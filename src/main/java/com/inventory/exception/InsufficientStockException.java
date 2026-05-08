package com.inventory.exception;

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String productName, int available, int requested) {
        super("Stock insuficiente para '%s'. Disponible: %d, Solicitado: %d"
                .formatted(productName, available, requested));
    }
}

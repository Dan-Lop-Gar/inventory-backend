package com.inventory.exception;

import java.util.UUID;

public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(UUID id) {
        super("Producto no encontrado con ID: " + id);
    }
    public ProductNotFoundException(String sku) {
        super("Producto no encontrado con SKU: " + sku);
    }
}

package com.inventory.exception;

import java.util.UUID;

public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(UUID id) {
        super("Orden no encontrada con ID: " + id);
    }
}

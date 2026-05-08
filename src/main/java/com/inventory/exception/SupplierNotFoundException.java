package com.inventory.exception;

import java.util.UUID;

public class SupplierNotFoundException extends RuntimeException {

    public SupplierNotFoundException(UUID id) {
        super("Proveedor no encontrado con ID: " + id);
    }

    public SupplierNotFoundException(String email) {
        super("Proveedor no encontrado con email: " + email);
    }
}

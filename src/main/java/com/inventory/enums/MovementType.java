package com.inventory.enums;

public enum MovementType {
    PURCHASE_IN,
    SALE_OUT,
    ADJUSTMENT_IN,
    ADJUSTMENT_OUT,
    RETURN_IN;

    public boolean isInbound() {
        return switch (this) {
            case PURCHASE_IN, ADJUSTMENT_IN, RETURN_IN -> true;
            case SALE_OUT, ADJUSTMENT_OUT -> false;
        };
    }

    public int applyTo(int currentStock, int quantity) {
        return isInbound() ? currentStock + quantity : currentStock - quantity;
    }
}
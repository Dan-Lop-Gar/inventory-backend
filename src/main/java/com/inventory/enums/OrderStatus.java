package com.inventory.enums;

// Sealed interface — el compilador garantiza que cubres todos los casos en switch expressions
public enum OrderStatus {
    PENDING,
    APPROVED,
    RECEIVED,
    CANCELLED,
    FAILED;

    public boolean isTerminal() {
        return switch (this) {
            case RECEIVED, CANCELLED, FAILED -> true;
            case PENDING, APPROVED -> false;
        };
    }

    public boolean canTransitionTo(OrderStatus next) {
        return switch (this) {
            case PENDING  -> next == APPROVED || next == CANCELLED || next == FAILED;
            case APPROVED -> next == RECEIVED || next == CANCELLED || next == FAILED;
            case RECEIVED, CANCELLED, FAILED -> false;
        };
    }
}
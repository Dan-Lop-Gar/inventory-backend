package com.inventory.kafka.event;

import com.inventory.enums.MovementType;

import java.time.LocalDateTime;
import java.util.UUID;

public record StockMovementEvent(
        UUID movementId,
        UUID productId,
        String productName,
        MovementType movementType,
        int quantity,
        int stockBefore,
        int stockAfter,
        String createdBy,
        LocalDateTime occurredAt
) {}
package com.inventory.kafka.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record StockAlertEvent(
        UUID productId,
        String productName,
        String sku,
        int stockCurrent,
        int stockMinimum,
        LocalDateTime alertedAt
) {}

package com.inventory.kafka.event;

import com.inventory.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record OrderEvent(
        UUID orderId,
        String orderNumber,
        OrderStatus status,
        UUID supplierId,
        BigDecimal totalAmount,
        String createdBy,
        int retryCount,
        LocalDateTime eventTime
) {}

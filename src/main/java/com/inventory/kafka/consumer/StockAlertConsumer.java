package com.inventory.kafka.consumer;

import com.inventory.config.KafkaConfig;
import com.inventory.kafka.event.StockAlertEvent;
import com.inventory.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockAlertConsumer {

    private final NotificationService notificationService;

    @KafkaListener(
            topics = KafkaConfig.TOPIC_STOCK_ALERTS,
            groupId = "inventory-alert-group"
    )
    public void consumeStockAlert(StockAlertEvent event) {
        log.warn("Alerta de stock recibida: {} SKU: {} Stock: {}/{}",
                event.productName(), event.sku(),
                event.stockCurrent(), event.stockMinimum()
        );

        // Notifica internamente via gRPC al servicio de notificaciones
        notificationService.sendStockAlert(event);
    }
}
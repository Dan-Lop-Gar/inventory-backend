package com.inventory.kafka.consumer;

import com.inventory.config.KafkaConfig;
import com.inventory.kafka.event.OrderEvent;
import com.inventory.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(
            topics = KafkaConfig.TOPIC_ORDER_EVENTS,
            groupId = "inventory-order-group"
    )
    public void consumeOrderEvent(OrderEvent event) {
        log.info("Evento de orden recibido: {} - Status: {}",
                event.orderNumber(), event.status()
        );

        // Notifica al usuario que creó la orden via gRPC interno
        notificationService.sendOrderNotification(event);

        switch (event.status()) {
            case APPROVED -> log.info("Orden aprobada: {}", event.orderNumber());
            case RECEIVED -> log.info("Orden recibida: {} - Stock actualizado",
                    event.orderNumber());
            case CANCELLED -> log.warn("Orden cancelada: {}", event.orderNumber());
            case FAILED    -> log.error("Orden fallida: {} - Enviada al DLQ",
                    event.orderNumber());
            default -> log.debug("Evento de orden: {} - {}",
                    event.orderNumber(), event.status());
        }
    }
}

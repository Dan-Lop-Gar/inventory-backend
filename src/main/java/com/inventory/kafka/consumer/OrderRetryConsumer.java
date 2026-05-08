package com.inventory.kafka.consumer;

import com.inventory.config.KafkaConfig;
import com.inventory.kafka.event.OrderEvent;
import com.inventory.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderRetryConsumer {

    private static final int MAX_RETRIES = 3;
    private final OrderService orderService;

    @KafkaListener(
            topics = KafkaConfig.TOPIC_ORDER_RETRY_DLQ,
            groupId = "inventory-retry-group",
            containerFactory = "retryKafkaListenerContainerFactory"
    )
    public void consumeFailedOrder(OrderEvent event, Acknowledgment ack) {
        log.warn("Procesando orden fallida del DLQ: {} intento: {}",
                event.orderNumber(), event.retryCount());

        try {
            if (event.retryCount() >= MAX_RETRIES) {
                log.error("Orden {} superó máximo de reintentos ({}). Descartando.",
                        event.orderNumber(), MAX_RETRIES);
                ack.acknowledge();
                return;
            }

            // Reintenta aprobar la orden
            orderService.approve(event.orderId());
            log.info("Orden reintentada exitosamente: {}", event.orderNumber());
            ack.acknowledge();

        } catch (Exception ex) {
            log.error("Error en reintento de orden: {}", event.orderNumber(), ex);
            orderService.markAsFailed(
                    event.orderId(),
                    "Reintento fallido: " + ex.getMessage()
            );
            ack.acknowledge();
        }
    }
}
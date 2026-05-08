package com.inventory.kafka.producer;

import com.inventory.config.KafkaConfig;
import com.inventory.entity.PurchaseOrder;
import com.inventory.enums.OrderStatus;
import com.inventory.kafka.event.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventProducer {

    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    public void sendOrderEvent(PurchaseOrder order, OrderStatus status) {
        OrderEvent event = new OrderEvent(
                order.getId(),
                order.getOrderNumber(),
                status,
                order.getSupplier().getId(),
                order.getTotalAmount(),
                order.getCreatedBy(),
                order.getRetryCount(),
                LocalDateTime.now()
        );

        String topic = status == OrderStatus.FAILED
                ? KafkaConfig.TOPIC_ORDER_RETRY_DLQ
                : KafkaConfig.TOPIC_ORDER_EVENTS;

        kafkaTemplate.send(topic, order.getId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Error enviando evento de orden: {}",
                                order.getOrderNumber(), ex);
                    } else {
                        log.info("Evento de orden enviado: {} status: {}",
                                order.getOrderNumber(), status);
                    }
                });
    }
}
package com.inventory.kafka.producer;

import com.inventory.config.KafkaConfig;
import com.inventory.entity.Product;
import com.inventory.kafka.event.StockAlertEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockAlertProducer {

    private final KafkaTemplate<String, StockAlertEvent> kafkaTemplate;

    public void sendAlert(Product product) {
        StockAlertEvent event = new StockAlertEvent(
                product.getId(),
                product.getName(),
                product.getSku(),
                product.getStockCurrent(),
                product.getStockMinimum(),
                LocalDateTime.now()
        );

        CompletableFuture<SendResult<String, StockAlertEvent>> future =
                kafkaTemplate.send(
                        KafkaConfig.TOPIC_STOCK_ALERTS,
                        product.getId().toString(),
                        event
                );

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Error enviando alerta de stock para producto: {}",
                        product.getSku(), ex);
            } else {
                log.debug("Alerta de stock enviada para: {} offset: {}",
                        product.getSku(),
                        result.getRecordMetadata().offset()
                );
            }
        });
    }
}
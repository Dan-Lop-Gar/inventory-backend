package com.inventory.config;

import com.inventory.kafka.event.OrderEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;

@Configuration
public class KafkaConfig {

    public static final String TOPIC_STOCK_ALERTS     = "stock-alerts";
    public static final String TOPIC_ORDER_EVENTS     = "order-events";
    public static final String TOPIC_STOCK_MOVEMENTS  = "stock-movements";
    public static final String TOPIC_ORDER_RETRY_DLQ  = "order-retry-dlq";

    @Bean
    public NewTopic stockAlertsTopic() {
        return TopicBuilder.name(TOPIC_STOCK_ALERTS)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic orderEventsTopic() {
        return TopicBuilder.name(TOPIC_ORDER_EVENTS)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic stockMovementsTopic() {
        return TopicBuilder.name(TOPIC_STOCK_MOVEMENTS)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic orderRetryDlqTopic() {
        return TopicBuilder.name(TOPIC_ORDER_RETRY_DLQ)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OrderEvent> retryKafkaListenerContainerFactory(
            ConsumerFactory<String, OrderEvent> consumerFactory) {

        ConcurrentKafkaListenerContainerFactory<String, OrderEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        factory.setConcurrency(1);

        return factory;
    }
}

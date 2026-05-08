package com.inventory.service;

import com.inventory.grpc.generated.*;
import com.inventory.kafka.event.OrderEvent;
import com.inventory.kafka.event.StockAlertEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NotificationService {

    private final NotificationServiceGrpc.NotificationServiceBlockingStub notificationServiceStub;

    public NotificationService(NotificationServiceGrpc.NotificationServiceBlockingStub notificationServiceStub){
        this.notificationServiceStub = notificationServiceStub;
    }

    public void sendStockAlert(StockAlertEvent event) {
        try {
            StockAlertNotification notification = StockAlertNotification.newBuilder()
                    .setProductId(event.productId().toString())
                    .setProductName(event.productName())
                    .setSku(event.sku())
                    .setStockCurrent(event.stockCurrent())
                    .setStockMinimum(event.stockMinimum())
                    .build();

            NotificationResponse response = notificationServiceStub.sendStockAlert(notification);

            log.info("Stock alert notification enviada: {} - {}",
                    event.sku(), response.getMessage()
            );

        } catch (Exception e) {
            log.error("Error enviando stock alert notification para: {}",
                    event.sku(), e
            );
            // No relanzamos — la notificación fallida no debe afectar el flujo principal
        }
    }

    public void sendOrderNotification(OrderEvent event) {
        try {
            OrderNotification notification = OrderNotification.newBuilder()
                    .setOrderId(event.orderId().toString())
                    .setOrderNumber(event.orderNumber())
                    .setStatus(event.status().name())
                    .setCreatedBy(event.createdBy())
                    .setTotalAmount(event.totalAmount().doubleValue())
                    .build();

            NotificationResponse response = notificationServiceStub
                    .sendOrderNotification(notification);

            log.info("Order notification enviada: {} - {}",
                    event.orderNumber(), response.getMessage()
            );

        } catch (Exception e) {
            log.error("Error enviando order notification: {}",
                    event.orderNumber(), e
            );
        }
    }
}

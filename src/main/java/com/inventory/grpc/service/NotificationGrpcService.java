package com.inventory.grpc.service;

import com.inventory.grpc.generated.*;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.ExecutorService;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.grpc.server.service.GrpcService;

@Slf4j
@GrpcService
public class NotificationGrpcService
        extends NotificationServiceGrpc.NotificationServiceImplBase {

    // Usa virtual threads para no bloquear mientras "envía" la notificación
    private final ExecutorService notificationExecutor;

    public NotificationGrpcService(
            @Qualifier("notificationExecutor") ExecutorService notificationExecutor) {
        this.notificationExecutor = notificationExecutor;
    }

    // SendStockAlert
    // Llamado por StockAlertConsumer cuando recibe el evento de Kafka
    @Override
    public void sendStockAlert(
            StockAlertNotification request,
            StreamObserver<NotificationResponse> responseObserver) {

        // Ejecuta en virtual thread para no bloquear el gRPC thread pool
        notificationExecutor.submit(() -> {
            try {
                log.warn("ALERTA DE STOCK recibida internamente: {} SKU:{} Stock:{}/{}",
                        request.getProductName(),
                        request.getSku(),
                        request.getStockCurrent(),
                        request.getStockMinimum()
                );

                // - Email al equipo de compras
                // - Slack/Teams notification
                // - Push notification al dashboard
                Thread.sleep(100); // simula latencia de envío externo

                log.info("Notificación de stock bajo enviada para: {}", request.getSku());

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Notificación interrumpida", e);
            }
        });

        // Responde inmediatamente sin esperar que el executor termine
        // El envío real es asíncrono
        responseObserver.onNext(
                NotificationResponse.newBuilder()
                        .setSent(true)
                        .setMessage("Notificación de stock encolada para: " + request.getSku())
                        .build()
        );
        responseObserver.onCompleted();
    }

    // SendOrderNotification
    // Llamado por OrderEventConsumer cuando una orden cambia de estado
    @Override
    public void sendOrderNotification(
            OrderNotification request,
            StreamObserver<NotificationResponse> responseObserver) {

        notificationExecutor.submit(() -> {
            try {
                log.info("Notificación de orden: {} - Status: {} - Usuario: {}",
                        request.getOrderNumber(),
                        request.getStatus(),
                        request.getCreatedBy()
                );

                // notificar al usuario que creó la orden
                Thread.sleep(100);

                log.info("Notificación de orden enviada: {}", request.getOrderNumber());

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Notificación de orden interrumpida", e);
            }
        });

        responseObserver.onNext(
                NotificationResponse.newBuilder()
                        .setSent(true)
                        .setMessage("Notificación de orden encolada: " + request.getOrderNumber())
                        .build()
        );
        responseObserver.onCompleted();
    }
}
